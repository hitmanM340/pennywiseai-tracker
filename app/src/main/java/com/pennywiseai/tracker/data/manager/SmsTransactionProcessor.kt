package com.pennywiseai.tracker.data.manager

import com.pennywiseai.tracker.data.preferences.BankAccountMergeStore

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.tracker.data.database.PennyWiseDatabase
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.ProfileEntity
import com.pennywiseai.tracker.data.database.entity.CardType
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.mapper.toEntity
import com.pennywiseai.tracker.data.mapper.toEntityType
import com.pennywiseai.tracker.utils.BalanceCalculator
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.CardRepository
import com.pennywiseai.tracker.data.preferences.IgnoredAccountsStore
import com.pennywiseai.tracker.data.repository.MerchantMappingRepository
import com.pennywiseai.tracker.data.repository.SubscriptionRepository
import com.pennywiseai.tracker.data.repository.TagRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
<<<<<<< HEAD
import com.pennywiseai.tracker.domain.model.rule.tagChanges
=======
import com.pennywiseai.tracker.data.database.dao.CustomParserRuleDao
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
import com.pennywiseai.tracker.domain.repository.RuleRepository
import com.pennywiseai.tracker.domain.service.RuleEngine
import java.math.BigDecimal
import java.time.Instant
import com.pennywiseai.tracker.receiver.BankNotificationConfig
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.ZoneId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared processor for SMS transactions. Used by both SmsBroadcastReceiver
 * and OptimizedSmsReaderWorker to ensure consistent transaction processing.
 */
@Singleton
class SmsTransactionProcessor @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
<<<<<<< HEAD
    private val tagRepository: TagRepository,
    private val ignoredAccountsStore: IgnoredAccountsStore,
    private val database: PennyWiseDatabase
=======
    private val customParserRuleDao: CustomParserRuleDao
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
) {
    private val bankAccountMerges = BankAccountMergeStore(appContext)
    companion object {
        private const val TAG = "SmsTransactionProcessor"
    }

    /**
     * Result of processing an SMS message
     */
    data class ProcessingResult(
        val success: Boolean,
        val transactionId: Long? = null,
        val reason: String? = null,
        /**
         * True when nothing was saved on purpose (the account is ignored), as
         * opposed to a parse or storage failure. Callers that queue a message
         * for retry should treat this as settled, not as something to try again.
         */
        val intentionallySkipped: Boolean = false
    )

    /**
     * Parses and saves a transaction from an SMS message.
     *
     * @param sender SMS sender address
     * @param body SMS body text
     * @param timestamp SMS timestamp in milliseconds
     * @return ProcessingResult indicating success/failure and transaction ID
     */
    suspend fun processAndSaveTransaction(
        sender: String,
        body: String,
        timestamp: Long
    ): ProcessingResult {
        try {
            // First, try standard built-in parsers
            val parsers = BankParserFactory.getParsers(sender)
<<<<<<< HEAD
            var parsedTransaction = if (parsers.isNotEmpty()) {
                parsers.firstNotNullOfOrNull { it.parse(body, sender, timestamp) }
            } else {
                null
            }

            // Normalize known bank quirks (e.g. LazyPay repayment reclassification #799)
            if (parsedTransaction != null) {
                parsedTransaction = BuiltInBankFallbacks.normalizeParsedTransaction(parsedTransaction, sender, body)
            }

            // Fallback 1: Built-in bank fallbacks (SVC Bank, ComBank Sri Lanka LKR)
            if (parsedTransaction == null) {
                parsedTransaction = BuiltInBankFallbacks.tryParseBuiltInFallbacks(sender, body, timestamp)
            }

            // Fallback 2: User-defined dynamic custom parser rules (O(R) cached in memory)
            if (parsedTransaction == null) {
                parsedTransaction = BuiltInBankFallbacks.tryParseWithCompiledCustomRules(
                    getCompiledCustomRules(),
                    sender,
                    body,
                    timestamp
                )
            }

            if (parsedTransaction == null) {
                return ProcessingResult(false, reason = "Could not parse transaction from SMS: $sender")
=======
            var parsedTransaction: ParsedTransaction? = null
            
            if (parsers.isNotEmpty()) {
                parsedTransaction = parsers.firstNotNullOfOrNull { it.parse(body, sender, timestamp) }
            }

            // Fallback: If no parser matched or was found, check user-defined custom rules from the database
            if (parsedTransaction == null) {
                Log.d(TAG, "Standard parser failed or absent for $sender. Checking custom parser rules...")
                parsedTransaction = parseWithCustomRules(sender, body, timestamp)
            }

            if (parsedTransaction == null) {
                return ProcessingResult(false, reason = "Could not parse transaction from SMS or custom rules")
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
            }

            Log.d(TAG, "Parsed transaction: ${parsedTransaction.amount} from ${parsedTransaction.bankName}")

            // Save the transaction
            return saveParsedTransaction(parsedTransaction, body)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            return ProcessingResult(false, reason = e.message)
        }
    }

    /**
     * Attempts to parse raw SMS or notification body using custom user-defined regex rules.
     * Loops through rules for the matching package/sender, extracts amount and merchant,
     * and constructs a ParsedTransaction if matching.
     */
    private suspend fun parseWithCustomRules(
        sender: String,
        body: String,
        timestamp: Long
    ): ParsedTransaction? {
        val rules = customParserRuleDao.getRulesForSender(sender)
        if (rules.isEmpty()) return null

        // Loop through each rule to see if it matches the notification/SMS text.
        // Similar to a for loop in Python: `for rule in rules:`
        for (rule in rules) {
            try {
                // Compile the pattern dynamically. We ignore casing for user flexibility.
                val regex = Regex(rule.regexPattern, RegexOption.IGNORE_CASE)
                
                // Kotlin's Regex.find matches standard regex patterns like in C/Python.
                // It searches the text and returns the first match or null.
                val matchResult = regex.find(body)
                if (matchResult != null) {
                    // matchResult.groupValues[index] contains captured content (like match.group(index) in Python)
                    // Index 0 represents the whole matched string. Capture groups start at 1.
                    val amountStr = matchResult.groupValues.getOrNull(rule.amountGroupIndex)
                        ?.replace(",", "") ?: continue
                        
                    val amount = try {
                        BigDecimal(amountStr)
                    } catch (e: NumberFormatException) {
                        continue // Invalid amount string format (e.g., failed to convert to number), try next rule
                    }

                    // Extract the Merchant/Payee name
                    val merchant = matchResult.groupValues.getOrNull(rule.merchantGroupIndex)?.trim()

                    // Extract account last 4 digits (if the rule has mapped it)
                    val accountLast4 = if (rule.accountGroupIndex != -1) {
                        matchResult.groupValues.getOrNull(rule.accountGroupIndex)
                            ?.trim()
                            ?.filter { it.isDigit() }
                            ?.takeLast(4)
                    } else {
                        null
                    }

                    // Safely translate string type to com.pennywiseai.parser.core.TransactionType enum
                    val type = try {
                        com.pennywiseai.parser.core.TransactionType.valueOf(rule.type.uppercase())
                    } catch (e: Exception) {
                        com.pennywiseai.parser.core.TransactionType.EXPENSE
                    }

                    // Give a friendly bank display name based on sender/package
                    val bankName = when (sender) {
                        "GPay" -> "Google Pay"
                        "SBI" -> "State Bank of India"
                        else -> sender
                    }

                    Log.d(TAG, "Successfully matched custom parser rule '${rule.ruleName}' for $sender")
                    
                    return ParsedTransaction(
                        amount = amount,
                        type = type,
                        merchant = merchant,
                        reference = null,
                        accountLast4 = accountLast4,
                        balance = null,
                        smsBody = body,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = bankName,
                        isFromCard = false,
                        currency = "INR"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error matching custom rule: ${rule.ruleName}", e)
            }
        }
        return null
    }

    /**
     * Saves a parsed transaction to the database with all necessary processing:
     * - Duplicate detection
     * - Merchant mapping
     * - Rule application
     * - Subscription matching
     * - Balance updates
     */
    suspend fun saveParsedTransaction(
        parsedInput: ParsedTransaction,
        smsBody: String
    ): ProcessingResult = BankAccountMergeStore.mutationMutex.withLock {
        // One lock across the SMS receiver and the notification listener: a bank
        // that texts and pushes at the same moment would otherwise let both pass
        // the duplicate check before either insert lands.
        try {
            val parsedTransaction = bankAccountMerges.resolve(parsedInput)
            // Convert to entity
            val entity = parsedTransaction.toEntity()

            // An ignored account's messages are dropped before anything is
            // stored, so no row and no notification (#826). A debit-card
            // purchase reports the card's digits while the money leaves the
            // linked account, so check both — ignoring the account has to stop
            // its card spend too.
            val linkedAccountLast4 = if (parsedTransaction.isFromCard) {
                parsedTransaction.accountLast4?.let {
                    cardRepository.getCard(parsedTransaction.bankName, it)?.accountLast4
                }
            } else null
            if (ignoredAccountsStore.isIgnored(
                    entity.bankName,
                    entity.accountNumber,
                    linkedAccountLast4
                )
            ) {
                return ProcessingResult(
                    success = false,
                    reason = "Account is ignored",
                    intentionallySkipped = true
                )
            }

            // Check if this transaction was previously deleted by the user
            val existingTransaction = transactionRepository.getTransactionByHash(entity.transactionHash)
            if (existingTransaction != null) {
                if (existingTransaction.isDeleted) {
                    Log.d(TAG, "Skipping previously deleted transaction with hash: ${entity.transactionHash}")
                    return ProcessingResult(false, reason = "Transaction was previously deleted")
                }
                // Transaction already exists and not deleted - normal deduplication
                Log.d(TAG, "Transaction already exists: ${entity.transactionHash}")
                return ProcessingResult(false, reason = "Duplicate transaction")
            }

            // Same SMS already stored under an older hash (a parser fix changed the
            // amount): never insert it twice, and repair a known-bad amount. (#886)
            entity.smsBody?.let { body ->
                transactionRepository.getAnyBySms(body, entity.smsSender)?.let { stored ->
                    if (!stored.isDeleted && TransactionDeduplication.isParseArtifact(stored, entity)) {
                        transactionRepository.correctParsedAmount(stored, entity)
                    }
                    return ProcessingResult(false, reason = "Duplicate transaction")
                }
            }

            // Same charge already booked from the bank's other channel (SMS vs app
            // notification). Runs under saveLock, together with the insert below.
            if (entity.bankName in BankNotificationConfig.notificationBankNames) {
                val nearby = transactionRepository.getTransactionByAmountAndDate(
                    entity.amount,
                    entity.dateTime.minusMinutes(2),
                    entity.dateTime.plusMinutes(2)
                )
                if (TransactionDeduplication.isBookedByOtherChannel(
                        entity, nearby, BankNotificationConfig.notificationAliases
                    )
                ) {
                    Log.d(TAG, "Already booked from the other channel: ${entity.transactionHash}")
                    return ProcessingResult(false, reason = "Duplicate transaction")
                }
            }

            // Check for custom merchant mapping
            val customCategory = merchantMappingRepository.getCategoryForMerchant(entity.merchantName)
            val entityWithMapping = if (customCategory != null) {
                Log.d(TAG, "Found custom category mapping: ${entity.merchantName} -> $customCategory")
                entity.copy(category = customCategory)
            } else {
                entity
            }

            // Apply rule engine to the transaction
            val activeRules = ruleRepository.getActiveRulesByType(entityWithMapping.transactionType)

            // Check if this transaction should be blocked
            val blockingRule = ruleEngine.shouldBlockTransaction(
                entityWithMapping,
                smsBody,
                activeRules
            )

            if (blockingRule != null) {
                Log.d(TAG, "Transaction blocked by rule: ${blockingRule.name}")
                return ProcessingResult(false, reason = "Blocked by rule: ${blockingRule.name}")
            }

            val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                entityWithMapping,
                smsBody,
                activeRules
            )

            if (ruleApplications.isNotEmpty()) {
                Log.d(TAG, "Applied ${ruleApplications.size} rules to transaction")
            }

            // Check if this transaction matches an active subscription
            val matchedSubscription = subscriptionRepository.matchTransactionToSubscription(
                entityWithRules.merchantName,
                entityWithRules.amount
            )
            val finalEntity = if (matchedSubscription != null) {
                Log.d(TAG, "Matched subscription ${matchedSubscription.id}")
                entityWithRules.copy(isRecurring = true)
            } else {
                entityWithRules
            }

            val rowId = if (matchedSubscription != null) {
                // Atomicity: advance the billing cycle only if the charge row commits.
                // Insert returning -1L (duplicate hash) or a throw must leave
                // next_payment_date untouched. Callees are DAO passthroughs; no
                // nested transactions.
                database.withTransaction {
                    val id = transactionRepository.insertTransaction(finalEntity)
                    if (id != -1L) {
                        subscriptionRepository.updateNextPaymentDateAfterCharge(
                            matchedSubscription.id,
                            finalEntity.dateTime.toLocalDate()
                        )
                    }
                    id
                }
            } else {
                transactionRepository.insertTransaction(finalEntity)
            }
            if (rowId != -1L) {
                Log.d(TAG, "Saved new transaction with ID: $rowId${if (finalEntity.isRecurring) " (Recurring)" else ""}")

                // Save rule applications if any rules were applied. The engine ran
                // before the insert, so each application still carries the entity's
                // placeholder id of 0 — remap to the real row id, as the SMS worker
                // does, or the FK on rule_applications.transaction_id rejects the write
                // and the throw skips the tag/balance/widget work below.
                if (ruleApplications.isNotEmpty()) {
                    ruleRepository.saveRuleApplications(
                        ruleApplications.map { it.copy(transactionId = rowId.toString()) }
                    )
                    // ADD_TAG / REMOVE_TAG live in the tag table, not on the row (#748).
                    val (add, remove) = ruleApplications.tagChanges()
                    if (add.isNotEmpty() || remove.isNotEmpty()) tagRepository.applyTagChanges(rowId, add, remove)
                }

                // Process balance updates
                processBalanceUpdate(parsedTransaction, finalEntity, rowId)

                // Trigger widget refresh for the transaction-derived widgets
                com.pennywiseai.tracker.widget.WidgetRefresher.refreshTransactionWidgets(appContext)

                return ProcessingResult(true, transactionId = rowId)
            } else {
                Log.d(TAG, "Transaction already exists (duplicate): ${entity.transactionHash}")
                return ProcessingResult(false, reason = "Duplicate transaction")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}")
            return ProcessingResult(false, reason = e.message)
        }
    }

    private suspend fun processBalanceUpdate(
        parsedTransaction: ParsedTransaction,
        entity: TransactionEntity,
        rowId: Long
    ) {
        if (parsedTransaction.accountLast4 == null) {
            // Mobile-money wallets (eMola, M-Pesa Mozambique) have no per-account
            // number — the whole wallet is one account. Derive a single service-level
            // account row from the running balance the SMS carries, so the wallet shows
            // up as an account and counts toward the total balance.
            if (parsedTransaction.isMobileWallet && parsedTransaction.balance != null) {
                upsertWalletBalance(parsedTransaction, entity, rowId)
            }
            return
        }

        val isFromCard = parsedTransaction.isFromCard

        val targetAccountLast4: String? = if (isFromCard) {
            var card = parsedTransaction.accountLast4?.let {
                cardRepository.getCard(parsedTransaction.bankName, it)
            }

            if (card == null) {
                val isCredit = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT)
                parsedTransaction.accountLast4?.let { accountLast4 ->
                    cardRepository.findOrCreateCard(
                        cardLast4 = accountLast4,
                        bankName = parsedTransaction.bankName,
                        isCredit = isCredit
                    )
                }
                card = parsedTransaction.accountLast4?.let {
                    cardRepository.getCard(parsedTransaction.bankName, it)
                }
            }

            if (card == null) {
                Log.w(TAG, "Could not create/find card for ${parsedTransaction.bankName}")
                null
            } else {
                // Update card's balance
                cardRepository.updateCardBalance(
                    cardId = card.id,
                    balance = parsedTransaction.balance,
                    source = parsedTransaction.smsBody.take(200),
                    date = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(parsedTransaction.timestamp),
                        ZoneId.systemDefault()
                    )
                )

                when {
                    card.cardType == CardType.CREDIT -> parsedTransaction.accountLast4
                    card.cardType == CardType.DEBIT && card.accountLast4 != null -> card.accountLast4
                    else -> parsedTransaction.accountLast4
                }
            }
        } else {
            parsedTransaction.accountLast4
        }

        if (targetAccountLast4 != null) {
            val isCreditCard = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT) ||
                    parsedTransaction.accountLast4?.let {
                        cardRepository.getCard(parsedTransaction.bankName, it)?.cardType
                    } == CardType.CREDIT

            val existingAccount = accountBalanceRepository.getLatestBalance(
                parsedTransaction.bankName,
                targetAccountLast4
            )

            val resolvedIsCreditCard = isCreditCard || (existingAccount?.isCreditCard ?: false)

            // The account keeps its own currency; a foreign-currency spend isn't
            // added to it (see BalanceCalculator.balanceAfter).
            val accountCurrency = existingAccount?.currency
                ?: parsedTransaction.accountCurrency
                ?: parsedTransaction.currency
            val newBalance = BalanceCalculator.balanceAfter(
                explicitBalance = parsedTransaction.balance,
                isCreditCard = resolvedIsCreditCard,
                transactionType = parsedTransaction.type.toEntityType(),
                transactionAmount = parsedTransaction.amount,
                transactionCurrency = parsedTransaction.currency,
                accountCurrency = accountCurrency,
                currentBalance = existingAccount?.balance
            )

            // Credit-card SMS report the AVAILABLE limit, not the total. The UI derives
            // available = creditLimit − balance, so store total = available + outstanding
            // (the post-transaction balance) to keep that math correct. Falls back to the
            // existing stored limit when the SMS carries no limit. (#486)
            val resolvedCreditLimit = if (resolvedIsCreditCard && parsedTransaction.creditLimit != null) {
                parsedTransaction.creditLimit!! + newBalance
            } else {
                existingAccount?.creditLimit
            }

            val balanceEntity = AccountBalanceEntity(
                bankName = parsedTransaction.bankName,
                accountLast4 = targetAccountLast4,
                balance = newBalance,
                timestamp = entity.dateTime,
                transactionId = if (rowId != -1L) rowId else null,
                creditLimit = resolvedCreditLimit,
                isCreditCard = resolvedIsCreditCard,
                smsSource = parsedTransaction.smsBody.take(500),
                sourceType = "TRANSACTION",
                accountType = BalanceCalculator.preservedAccountType(existingAccount),
                currency = accountCurrency,
                profileId = existingAccount?.profileId ?: ProfileEntity.PERSONAL_ID,
                alias = existingAccount?.alias,
                lowBalanceThreshold = existingAccount?.lowBalanceThreshold
            )

            accountBalanceRepository.insertBalance(balanceEntity)
            Log.d(TAG, "Saved balance update for ${parsedTransaction.bankName} **$targetAccountLast4")
        }
    }

    /**
     * Derives a single service-level account row for a mobile-money wallet
     * (eMola, M-Pesa Mozambique). These SMS carry the running balance but no
     * per-account number, so the account is keyed on bankName with the
     * [AccountBalanceEntity.WALLET_ACCOUNT_MARKER] sentinel and the balance is
     * taken straight from the SMS (never a card, never credit).
     */
    private suspend fun upsertWalletBalance(
        parsedTransaction: ParsedTransaction,
        entity: TransactionEntity,
        rowId: Long
    ) {
        val balance = parsedTransaction.balance ?: return
        val existingAccount = accountBalanceRepository.getLatestBalance(
            parsedTransaction.bankName,
            AccountBalanceEntity.WALLET_ACCOUNT_MARKER
        )

        val balanceEntity = AccountBalanceEntity(
            bankName = parsedTransaction.bankName,
            accountLast4 = AccountBalanceEntity.WALLET_ACCOUNT_MARKER,
            balance = balance,
            timestamp = entity.dateTime,
            transactionId = if (rowId != -1L) rowId else null,
            creditLimit = null,
            isCreditCard = false,
            smsSource = parsedTransaction.smsBody.take(500),
            sourceType = "TRANSACTION",
            accountType = BalanceCalculator.preservedAccountType(existingAccount),
            currency = parsedTransaction.currency,
            profileId = existingAccount?.profileId ?: ProfileEntity.PERSONAL_ID,
            alias = existingAccount?.alias,
            lowBalanceThreshold = existingAccount?.lowBalanceThreshold
        )

        accountBalanceRepository.insertBalance(balanceEntity)
        Log.d(TAG, "Saved wallet balance for ${parsedTransaction.bankName}")
    }

    @Volatile
    private var cachedCustomRules: List<CompiledCustomRule>? = null
    @Volatile
    private var cacheTimestampMs: Long = 0L

    /**
     * Returns in-memory pre-compiled custom rules with a 10-second TTL cache to eliminate
     * repeated DB reads and Regex compilation during bulk inbox scans.
     */
    private suspend fun getCompiledCustomRules(): List<CompiledCustomRule> {
        val now = System.currentTimeMillis()
        val existing = cachedCustomRules
        if (existing != null && (now - cacheTimestampMs) < 10_000L) {
            return existing
        }
        val fresh = runCatching {
            database.customParserRuleDao().getEnabledRulesSync().mapNotNull(CompiledCustomRule::fromEntityOrNull)
        }.getOrDefault(emptyList())
        cachedCustomRules = fresh
        cacheTimestampMs = now
        return fresh
    }
}
