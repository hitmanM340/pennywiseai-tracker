// [VERIFIED-2X]
package com.pennywiseai.tracker.ui.screens.unrecognized

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Result of auto-generating a regex pattern from raw SMS text.
 */
data class AutoRegexResult(
    val patternString: String,
    val amountGroup: String = "amount",
    val accountGroup: String? = "account",
    val merchantGroup: String? = "merchant",
    val balanceGroup: String? = "balance",
    val isReDoSSafe: Boolean = true,
    val sampleMatchCount: Int = 0
)

/**
 * Regex Rule Auto Generator: Synthesizes safe, ReDoS-protected regular expressions
 * from sample SMS messages to eliminate manual regex typing.
 */
object RegexRuleAutoGenerator {

    private val SUSPICIOUS_REDOS_PATTERNS = listOf(
        "(.*)+",
        "(.+)+",
        "(.*)*",
        "([a-zA-Z]+)*",
        "(\\d+)+",
        "(\\s+)+"
    )

    /**
     * Checks if a pattern is syntactically valid and free of obvious catastrophic backtracking flaws.
     */
    fun isSafeRegex(pattern: String): Boolean {
        if (pattern.length > 500) return false

        // Check for nested unlimited quantifiers (classic ReDoS trigger)
        for (suspicious in SUSPICIOUS_REDOS_PATTERNS) {
            if (pattern.contains(suspicious)) {
                return false
            }
        }

        return try {
            val compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            // Test execution with a moderately long test string to detect catastrophic backtracking
            val testProbe = "A".repeat(100)
            val matcher = compiled.matcher(testProbe)
            val startTime = System.currentTimeMillis()
            matcher.find()
            val elapsed = System.currentTimeMillis() - startTime
            elapsed < 50 // Fail if matching takes >50ms for a small probe
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Infers a structured regex with capture groups from sample SMS text.
     */
    fun generateFromSms(smsText: String): AutoRegexResult {
        if (smsText.isBlank()) {
            return AutoRegexResult(
                patternString = "",
                isReDoSSafe = true,
                sampleMatchCount = 0
            )
        }

        val cleaned = smsText.replace("\n", " ").trim()
        val builder = StringBuilder()

        // Amount pattern finder: Rs.?\s*[\d,]+(\.\d{1,2})? or INR\s*[\d,]+(\.\d{1,2})?
        val amountRegex = Pattern.compile("(?i)(?:Rs\\.?|INR|USD|EUR|GBP|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        val amountMatcher = amountRegex.matcher(cleaned)

        var hasAmount = false
        var sampleAccount: String? = null
        var sampleMerchant: String? = null

        // Account pattern finder: a/c.*XX(\d{3,4}) or card ending (\d{4})
        val accountRegex = Pattern.compile("(?i)(?:a/c|acct|card|ending|xx+)[^0-9]*([0-9]{3,4})")
        val accountMatcher = accountRegex.matcher(cleaned)
        if (accountMatcher.find()) {
            sampleAccount = accountMatcher.group(1)
        }

        // Merchant pattern finder: at\s+([A-Za-z0-9\s\.\*]+?)(?:\s+on|\s+ref|\s+bal|\$)
        val merchantRegex = Pattern.compile("(?i)(?:at|vpa|to)\\s+([A-Za-z0-9\\*\\.\\-]{3,30})")
        val merchantMatcher = merchantRegex.matcher(cleaned)
        if (merchantMatcher.find()) {
            sampleMerchant = merchantMatcher.group(1)
        }

        // Build robust template regex
        builder.append("(?i)")
        if (cleaned.contains("debited", ignoreCase = true) || cleaned.contains("spent", ignoreCase = true)) {
            builder.append(".*?(?:debited|spent|paid).*?")
        } else if (cleaned.contains("credited", ignoreCase = true) || cleaned.contains("deposited", ignoreCase = true)) {
            builder.append(".*?(?:credited|deposited).*?")
        } else {
            builder.append(".*?")
        }

        // Amount capture group
        builder.append("(?:INR|Rs\\.?|₹|USD|EUR)?\\s*(?<amount>[0-9,]+(?:\\.[0-9]{1,2})?)")

        // Account capture group if present
        if (sampleAccount != null) {
            builder.append(".*?(?<account>[0-9]{3,4})")
        }

        // Merchant capture group if present
        if (sampleMerchant != null) {
            builder.append(".*?(?:at|to|vpa)\\s+(?<merchant>[A-Za-z0-9\\*\\.\\-]{2,30})")
        }

        // Balance capture group if present
        if (cleaned.contains("bal", ignoreCase = true) || cleaned.contains("balance", ignoreCase = true)) {
            builder.append(".*?(?:bal|balance)[^0-9]*(?<balance>[0-9,]+(?:\\.[0-9]{1,2})?)")
        }

        val finalPattern = builder.toString()
        val isSafe = isSafeRegex(finalPattern)

        val matchCount = try {
            val compiled = Pattern.compile(finalPattern, Pattern.CASE_INSENSITIVE)
            if (compiled.matcher(cleaned).find()) 1 else 0
        } catch (e: Exception) {
            0
        }

        return AutoRegexResult(
            patternString = finalPattern,
            amountGroup = "amount",
            accountGroup = if (sampleAccount != null) "account" else null,
            merchantGroup = if (sampleMerchant != null) "merchant" else null,
            balanceGroup = if (cleaned.contains("bal", ignoreCase = true)) "balance" else null,
            isReDoSSafe = isSafe,
            sampleMatchCount = matchCount
        )
    }
}
