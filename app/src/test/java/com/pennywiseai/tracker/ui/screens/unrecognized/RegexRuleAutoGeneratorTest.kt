package com.pennywiseai.tracker.ui.screens.unrecognized

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexRuleAutoGeneratorTest {

    @Test
    fun isSafeRegex_detectsCatastrophicBacktrackingPatterns() {
        // Obvious ReDoS vulnerabilities
        assertFalse(RegexRuleAutoGenerator.isSafeRegex("(.*)+"))
        assertFalse(RegexRuleAutoGenerator.isSafeRegex("(.+)+"))
        assertFalse(RegexRuleAutoGenerator.isSafeRegex("([a-zA-Z]+)*"))

        // Safe bounded patterns
        assertTrue(RegexRuleAutoGenerator.isSafeRegex("""(?:INR|Rs\.?)\s*([0-9,]+(?:\.[0-9]{1,2})?)"""))
        assertTrue(RegexRuleAutoGenerator.isSafeRegex("""(?i).*?debited.*?(?<amount>[0-9,]+)"""))
    }

    @Test
    fun generateFromSms_extractsAmountAndAccount() {
        val sampleSms = "Your A/C ending 4912 is debited by INR 1,450.50 at STARBUCKS on 18-Oct-26. Avail Bal: INR 12,340.00"

        val result = RegexRuleAutoGenerator.generateFromSms(sampleSms)

        assertTrue(result.isReDoSSafe)
        assertEquals("amount", result.amountGroup)
        assertEquals("account", result.accountGroup)
        assertEquals("merchant", result.merchantGroup)
        assertEquals("balance", result.balanceGroup)
        assertEquals(1, result.sampleMatchCount)
    }

    @Test
    fun generateFromSms_handlesCreditMessages() {
        val sampleCreditSms = "INR 25,000.00 credited to your card ending 8812 on 15-Oct-26. Info: Salary deposit."

        val result = RegexRuleAutoGenerator.generateFromSms(sampleCreditSms)

        assertTrue(result.isReDoSSafe)
        assertEquals("amount", result.amountGroup)
        assertTrue(result.patternString.contains("credited"))
    }
}
