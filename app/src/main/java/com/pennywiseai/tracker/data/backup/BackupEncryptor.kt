package com.pennywiseai.tracker.data.backup

<<<<<<< HEAD
import java.nio.ByteBuffer
=======
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryptor {
<<<<<<< HEAD
    private val MAGIC_HEADER = "PWENC1".toByteArray(Charsets.UTF_8)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + 16) return false
        return bytes.sliceArray(MAGIC_HEADER.indices).contentEquals(MAGIC_HEADER)
    }

    fun encrypt(plainBytes: ByteArray, password: CharArray, clearPassword: Boolean = false): ByteArray {
        require(password.isNotEmpty()) { "Backup password must not be empty" }
        return try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val cipherBytes = cipher.doFinal(plainBytes)

            ByteBuffer.allocate(MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + cipherBytes.size)
                .put(MAGIC_HEADER)
                .put(salt)
                .put(iv)
                .put(cipherBytes)
                .array()
        } finally {
            if (clearPassword) {
                password.fill('\u0000')
            }
        }
    }

    fun decrypt(encryptedBytes: ByteArray, password: CharArray, clearPassword: Boolean = false): ByteArray {
        require(isEncrypted(encryptedBytes)) { "Invalid encrypted backup header" }
        return try {
            val buffer = ByteBuffer.wrap(encryptedBytes, MAGIC_HEADER.size, encryptedBytes.size - MAGIC_HEADER.size)
            val salt = ByteArray(SALT_LENGTH).also { buffer.get(it) }
            val iv = ByteArray(IV_LENGTH).also { buffer.get(it) }
            val cipherBytes = ByteArray(buffer.remaining()).also { buffer.get(it) }

            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.doFinal(cipherBytes)
        } finally {
            if (clearPassword) {
                password.fill('\u0000')
            }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
=======
    private val MAGIC_BYTES = byteArrayOf(0x50, 0x57, 0x42, 0x4B) // 'P' 'W' 'B' 'K'
    private const val ITERATIONS = 600_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    fun encrypt(data: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        
        // Derive key
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")
        
        // Encrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(data)
        
        // Assemble output: MAGIC_BYTES + SALT + IV + CIPHERTEXT
        val output = ByteArray(MAGIC_BYTES.size + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        System.arraycopy(MAGIC_BYTES, 0, output, 0, MAGIC_BYTES.size)
        System.arraycopy(salt, 0, output, MAGIC_BYTES.size, SALT_LENGTH)
        System.arraycopy(iv, 0, output, MAGIC_BYTES.size + SALT_LENGTH, IV_LENGTH)
        System.arraycopy(ciphertext, 0, output, MAGIC_BYTES.size + SALT_LENGTH + IV_LENGTH, ciphertext.size)
        
        return output
    }

    fun decrypt(encryptedData: ByteArray, password: CharArray): ByteArray {
        val minLength = MAGIC_BYTES.size + SALT_LENGTH + IV_LENGTH
        if (encryptedData.size < minLength) {
            throw IllegalArgumentException("Invalid backup file: too short")
        }
        
        // Check magic bytes
        for (i in MAGIC_BYTES.indices) {
            if (encryptedData[i] != MAGIC_BYTES[i]) {
                throw IllegalArgumentException("Invalid backup file format: magic bytes mismatch")
            }
        }
        
        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(encryptedData, MAGIC_BYTES.size, salt, 0, SALT_LENGTH)
        
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedData, MAGIC_BYTES.size + SALT_LENGTH, iv, 0, IV_LENGTH)
        
        val ciphertextLength = encryptedData.size - minLength
        val ciphertext = ByteArray(ciphertextLength)
        System.arraycopy(encryptedData, minLength, ciphertext, 0, ciphertextLength)
        
        // Derive key
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(secretKeyBytes, "AES")
        
        // Decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        return cipher.doFinal(ciphertext)
    }

    fun hasMagicHeader(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_BYTES.size) return false
        for (i in MAGIC_BYTES.indices) {
            if (bytes[i] != MAGIC_BYTES[i]) return false
        }
        return true
>>>>>>> 49bfb232429353af5c844b3b4c1157612982a742
    }
}
