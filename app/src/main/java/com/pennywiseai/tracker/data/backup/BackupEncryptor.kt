package com.pennywiseai.tracker.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupEncryptor {
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
    }
}
