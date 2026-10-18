package com.pennywiseai.tracker.data.manager

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.tracker.data.database.entity.CustomParserRuleEntity
import java.math.BigDecimal

/**
 * Pre-compiled regex wrapper for user-defined CustomParserRuleEntity
 * to eliminate Pattern.compile() overhead during bulk SMS scanning.
 */
class CompiledCustomRule(
    val entity: CustomParserRuleEntity,
    val senderRegex: Regex,
    val amountRegex: Regex,
    val merchantRegex: Regex?,
    val accountRegex: Regex?,
    val resolvedType: TransactionType
) {
    companion object {
        fun fromEntityOrNull(rule: CustomParserRuleEntity): CompiledCustomRule? = runCatching {
            CompiledCustomRule(
                entity = rule,
                senderRegex = Regex(rule.senderPattern, RegexOption.IGNORE_CASE),
                amountRegex = Regex(rule.amountRegex, RegexOption.IGNORE_CASE),
                merchantRegex = rule.merchantRegex?.takeIf { it.isNotBlank() }
                    ?.let { Regex(it, RegexOption.IGNORE_CASE) },
                accountRegex = rule.accountRegex?.takeIf { it.isNotBlank() }
                    ?.let { Regex(it, RegexOption.IGNORE_CASE) },
                resolvedType = runCatching { TransactionType.valueOf(rule.transactionType.uppercase()) }
                    .getOrDefault(TransactionType.EXPENSE)
            )
        }.getOrNull()
    }
}

object BuiltInBankFallbacks {

    // 1. SVC Bank (Shamrao Vitthal Cooperative Bank - India)
    private val SVC_SENDER = Regex("""(?:^|-)(?:SVC|SVCBNK|SVCBANK)$""", RegexOption.IGNORE_CASE)
    private val SVC_REGEX = Regex(
        """SVC\s+Bank\s+A/c\s+\*(\d{3,4})\s+(DEBITED|CREDITED)\s+for\s+(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)(?:.*?Clr\s+Bal\s+(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?))?(?:.*?UPI/(?:DR|CR)/\d+/([^/.]+))?""",
        RegexOption.IGNORE_CASE
    )

    // 2. Commercial Bank of Ceylon (ComBank - Sri Lanka, LKR)
    private val COMBANK_SENDER = Regex("""COMBANK|CMB""", RegexOption.IGNORE_CASE)
    private val COMBANK_DEBIT_REGEX = Regex(
        """Purchase\s+at\s+(.+?)\s+for\s+(LKR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?).*?has\s+been\s+authorised\s+on\s+your\s+(?:debit|credit)\s+card\s+ending\s+#?(\d{4})""",
        RegexOption.IGNORE_CASE
    )
    private val COMBANK_CREDIT_REGEX = Regex(
        """Credit\s+for\s+(LKR|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(?:A/c|account)?\s*#?[\d*]*(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // 3. LazyPay Repayment Detection (#799)
    private val LAZYPAY_SENDER = Regex("""LAZYPAY|LZYPAY""", RegexOption.IGNORE_CASE)
    private val LAZYPAY_REPAYMENT_KEYWORDS = Regex(
        """\b(?:repayment|payment\s+of\s+(?:Rs\.?|INR)?\s*[\d,.]+\s+(?:received|towards|is\s+successful)|thank\s+you\s+for\s+(?:your\s+)?payment|towards\s+your\s+LazyPay\s+statement|bill\s+payment\s+received)\b""",
        RegexOption.IGNORE_CASE
    )
    private val LAZYPAY_SPEND_KEYWORDS = Regex(
        """\b(?:spent|used|charged|purchase|paid\s+to\s+merchant|debited)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Fixes Issue #799: LazyPayParser returns CREDIT for every accepted message,
     * including statement repayment confirmations. Reclassify confirmed repayments
     * so they reduce debt rather than counting as another credit purchase.
     */
    fun normalizeParsedTransaction(
        parsed: ParsedTransaction,
        sender: String,
        body: String
    ): ParsedTransaction {
        if (LAZYPAY_SENDER.containsMatchIn(sender) || parsed.bankName.equals("LazyPay", ignoreCase = true)) {
            val isRepayment = LAZYPAY_REPAYMENT_KEYWORDS.containsMatchIn(body) &&
                !LAZYPAY_SPEND_KEYWORDS.containsMatchIn(body)
            if (isRepayment && parsed.type == TransactionType.CREDIT) {
                return parsed.copy(
                    type = TransactionType.TRANSFER, // Reduces credit/BNPL balance without counting as income or purchase
                    merchant = parsed.merchant ?: "LazyPay Repayment"
                )
            }
        }
        return parsed
    }

    /**
     * Parses SVC Bank and ComBank (Sri Lanka) SMS messages when parser-core returns null.
     */
    fun tryParseBuiltInFallbacks(
        sender: String,
        body: String,
        timestamp: Long
    ): ParsedTransaction? {
        if (body.length > 2_000) return null

        // Check SVC Bank
        if (SVC_SENDER.containsMatchIn(sender) || body.startsWith("SVC Bank", ignoreCase = true)) {
            SVC_REGEX.find(body)?.let { match ->
                val accountLast4 = match.groupValues[1]
                val direction = match.groupValues[2].uppercase()
                val amount = match.groupValues[3].replace(",", "").toBigDecimalOrNull() ?: return null
                val balance = match.groupValues.getOrNull(4)?.replace(",", "")?.toBigDecimalOrNull()
                val merchant = match.groupValues.getOrNull(5)?.trim()?.takeIf { it.isNotEmpty() }

                return ParsedTransaction(
                    amount = amount,
                    type = if (direction == "CREDITED") TransactionType.INCOME else TransactionType.EXPENSE,
                    merchant = merchant ?: "SVC Bank Transaction",
                    reference = null,
                    accountLast4 = accountLast4,
                    balance = balance,
                    currency = "INR",
                    bankName = "SVC Bank",
                    smsBody = body,
                    timestamp = timestamp,
                    sender = sender
                )
            }
        }

        // Check Commercial Bank of Ceylon (ComBank)
        if (COMBANK_SENDER.containsMatchIn(sender)) {
            COMBANK_DEBIT_REGEX.find(body)?.let { match ->
                val merchant = match.groupValues[1].trim()
                val amount = match.groupValues[3].replace(",", "").toBigDecimalOrNull() ?: return null
                val cardLast4 = match.groupValues[4]

                return ParsedTransaction(
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    merchant = merchant,
                    reference = null,
                    accountLast4 = cardLast4,
                    balance = null,
                    isFromCard = true,
                    currency = "LKR",
                    bankName = "Commercial Bank of Ceylon",
                    smsBody = body,
                    timestamp = timestamp,
                    sender = sender
                )
            }

            COMBANK_CREDIT_REGEX.find(body)?.let { match ->
                val amount = match.groupValues[2].replace(",", "").toBigDecimalOrNull() ?: return null
                val accountLast4 = match.groupValues[3]

                return ParsedTransaction(
                    amount = amount,
                    type = TransactionType.INCOME,
                    merchant = "Account Credit",
                    reference = null,
                    accountLast4 = accountLast4,
                    balance = null,
                    currency = "LKR",
                    bankName = "Commercial Bank of Ceylon",
                    smsBody = body,
                    timestamp = timestamp,
                    sender = sender
                )
            }
        }

        return null
    }

    /**
     * Evaluates pre-compiled user custom rules in O(R) without any DB or Regex compile calls.
     */
    fun tryParseWithCompiledCustomRules(
        compiledRules: List<CompiledCustomRule>,
        sender: String,
        body: String,
        timestamp: Long
    ): ParsedTransaction? {
        if (compiledRules.isEmpty() || body.length > 2_000) return null

        for (rule in compiledRules) {
            if (!rule.senderRegex.containsMatchIn(sender)) continue
            val amountMatch = rule.amountRegex.find(body) ?: continue
            val rawAmount = amountMatch.groupValues.getOrNull(1)?.replace(",", "")?.trim()
                ?: amountMatch.value.replace(Regex("[^0-9.]"), "")
            val amount = rawAmount.toBigDecimalOrNull() ?: continue
            if (amount <= BigDecimal.ZERO) continue

            val merchant = rule.merchantRegex?.find(body)?.groupValues?.getOrNull(1)?.trim()
            val accountLast4 = rule.accountRegex?.find(body)?.groupValues?.getOrNull(1)?.trim()

            return ParsedTransaction(
                amount = amount,
                type = rule.resolvedType,
                merchant = merchant ?: rule.entity.name,
                reference = null,
                accountLast4 = accountLast4,
                balance = null,
                bankName = rule.entity.name.ifBlank { sender },
                smsBody = body,
                timestamp = timestamp,
                sender = sender,
                currency = "INR"
            )
        }
        return null
    }
}
