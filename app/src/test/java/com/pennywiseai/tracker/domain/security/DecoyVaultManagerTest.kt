package com.pennywiseai.tracker.domain.security

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DecoyVaultManagerTest {

    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val memoryStore = mutableMapOf<String, String>()

    private lateinit var decoyManager: DecoyVaultManager

    @Before
    fun setup() {
        memoryStore.clear()

        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<String>()
            memoryStore[key] = value
            editor
        }
        every { editor.remove(any()) } answers {
            val key = firstArg<String>()
            memoryStore.remove(key)
            editor
        }
        every { sharedPrefs.getString(any(), any()) } answers {
            val key = firstArg<String>()
            val default = secondArg<String?>()
            memoryStore[key] ?: default
        }

        decoyManager = DecoyVaultManager(context)
    }

    @Test
    fun setDecoyPin_andVerify_authenticatesCorrectPin() {
        assertFalse(decoyManager.isDecoyPinConfigured())

        decoyManager.setDecoyPin("1234")

        assertTrue(decoyManager.isDecoyPinConfigured())
        assertTrue(decoyManager.verifyDecoyPin("1234"))
        assertTrue(decoyManager.isDecoyModeActive.value)

        // Wrong pin fails
        assertFalse(decoyManager.verifyDecoyPin("9999"))
    }

    @Test
    fun clearDecoyPin_resetsState() {
        decoyManager.setDecoyPin("5678")
        assertTrue(decoyManager.isDecoyPinConfigured())

        decoyManager.clearDecoyPin()

        assertFalse(decoyManager.isDecoyPinConfigured())
        assertFalse(decoyManager.isDecoyModeActive.value)
    }

    @Test
    fun getDecoyAccounts_andTransactions_returnBelievableData() {
        val accounts = decoyManager.getDecoyAccounts()
        val transactions = decoyManager.getDecoyTransactions()

        assertEquals(2, accounts.size)
        assertEquals("Daily Checking", accounts[0].bankName)
        assertEquals("Cash Pocket", accounts[1].bankName)

        assertTrue(transactions.isNotEmpty())
        assertEquals("Blue Tokai Coffee", transactions[0].merchantName)
    }
}
