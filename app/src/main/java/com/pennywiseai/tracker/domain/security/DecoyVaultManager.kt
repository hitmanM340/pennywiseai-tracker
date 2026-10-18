// [VERIFIED-2X]
package com.pennywiseai.tracker.domain.security

import android.content.Context
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verification outcome when a PIN is entered on the lock screen.
 */
enum class PinAuthResult {
    AUTHENTICATED_PRIMARY,
    AUTHENTICATED_DECOY,
    FAILED
}

/**
 * Chameleon Mode: Duress and Decoy Vault Manager.
 * 
 * Provides an isolated, believable Decoy Ledger when the user is under duress
 * or unlocks with a secondary decoy PIN. Protects all real accounts, transactions,
 * loans, and net worth calculations.
 * 
 * Credentials are salted and hashed with SHA-256 and stored in "device_encryption_prefs",
 * which is excluded from cloud backups in backup_rules.xml and data_extraction_rules.xml.
 */
@Singleton
class DecoyVaultManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "device_encryption_prefs"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_sha256"
        private const val KEY_DECOY_SALT = "decoy_pin_salt"
        private const val SALT_LENGTH = 16
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _isDecoyModeActive = MutableStateFlow(false)
    val isDecoyModeActive: StateFlow<Boolean> = _isDecoyModeActive.asStateFlow()

    /**
     * Checks if a decoy duress PIN has been configured.
     */
    fun isDecoyPinConfigured(): Boolean {
        return prefs.getString(KEY_DECOY_PIN_HASH, null) != null
    }

    /**
     * Sets or updates the secondary Decoy PIN with constant-time salting.
     */
    fun setDecoyPin(pin: String) {
        require(pin.length in 4..8) { "Decoy PIN must be between 4 and 8 digits" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        val saltHex = salt.joinToString("") { "%02x".format(it) }

        val hash = computeHash(pin, salt)
        val hashHex = hash.joinToString("") { "%02x".format(it) }

        prefs.edit()
            .putString(KEY_DECOY_SALT, saltHex)
            .putString(KEY_DECOY_PIN_HASH, hashHex)
            .apply()
    }

    /**
     * Clears the configured Decoy PIN.
     */
    fun clearDecoyPin() {
        prefs.edit()
            .remove(KEY_DECOY_SALT)
            .remove(KEY_DECOY_PIN_HASH)
            .apply()
        _isDecoyModeActive.value = false
    }

    /**
     * Verifies if the entered PIN matches the Decoy PIN using constant-time comparison
     * to prevent side-channel timing attacks.
     */
    fun verifyDecoyPin(enteredPin: String): Boolean {
        val storedHashHex = prefs.getString(KEY_DECOY_PIN_HASH, null) ?: return false
        val storedSaltHex = prefs.getString(KEY_DECOY_SALT, null) ?: return false

        val salt = hexToBytes(storedSaltHex)
        val candidateHash = computeHash(enteredPin, salt)
        val storedHash = hexToBytes(storedHashHex)

        val matches = MessageDigest.isEqual(candidateHash, storedHash)
        if (matches) {
            _isDecoyModeActive.value = true
        }
        return matches
    }

    /**
     * Programmatically activates Decoy Mode (e.g. via duress gesture).
     */
    fun activateDecoyMode() {
        _isDecoyModeActive.value = true
    }

    /**
     * Returns to real vault mode.
     */
    fun deactivateDecoyMode() {
        _isDecoyModeActive.value = false
    }

    /**
     * Provides plausible decoy bank accounts to display when decoy vault is unlocked.
     */
    fun getDecoyAccounts(): List<AccountBalanceEntity> {
        val now = LocalDateTime.now()
        return listOf(
            AccountBalanceEntity(
                bankName = "Daily Checking",
                accountLast4 = "4912",
                balance = BigDecimal("4280.00"),
                timestamp = now,
                accountType = "SAVINGS",
                isCreditCard = false
            ),
            AccountBalanceEntity(
                bankName = "Cash Pocket",
                accountLast4 = "0000",
                balance = BigDecimal("650.00"),
                timestamp = now,
                accountType = "CASH",
                isCreditCard = false
            )
        )
    }

    /**
     * Provides plausible everyday decoy transactions (coffee, grocery, commute).
     */
    fun getDecoyTransactions(): List<TransactionEntity> {
        val now = LocalDateTime.now()
        return listOf(
            TransactionEntity(
                id = 999901L,
                amount = BigDecimal("180.00"),
                merchantName = "Blue Tokai Coffee",
                category = "Food & Dining",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusHours(2),
                bankName = "Daily Checking",
                accountNumber = "4912",
                currency = "INR",
                transactionHash = "decoy_hash_001"
            ),
            TransactionEntity(
                id = 999902L,
                amount = BigDecimal("60.00"),
                merchantName = "Metro Transit Pass",
                category = "Transportation",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusHours(7),
                bankName = "Daily Checking",
                accountNumber = "4912",
                currency = "INR",
                transactionHash = "decoy_hash_002"
            ),
            TransactionEntity(
                id = 999903L,
                amount = BigDecimal("450.00"),
                merchantName = "Nature's Basket",
                category = "Groceries",
                transactionType = TransactionType.EXPENSE,
                dateTime = now.minusDays(1),
                bankName = "Daily Checking",
                accountNumber = "4912",
                currency = "INR",
                transactionHash = "decoy_hash_003"
            ),
            TransactionEntity(
                id = 999904L,
                amount = BigDecimal("2500.00"),
                merchantName = "Monthly Consulting Stipend",
                category = "Income",
                transactionType = TransactionType.INCOME,
                dateTime = now.minusDays(3),
                bankName = "Daily Checking",
                accountNumber = "4912",
                currency = "INR",
                transactionHash = "decoy_hash_004"
            )
        )
    }

    private fun computeHash(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(StandardCharsets.UTF_8))
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
