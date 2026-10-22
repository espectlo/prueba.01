package com.example.prueba01.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class EncryptedKeyStoreRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("safedoor_keystore_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MASTER_ENCRYPTION_KEY = "master_encryption_key"
        private const val KEY_USER_SECURITY_PIN_HASH = "user_security_pin_hash"
        private const val KEY_USER_SECURITY_PIN_SALT = "user_security_pin_salt"
    }

    fun saveMasterKey(masterKey: String) {
        prefs.edit().putString(KEY_MASTER_ENCRYPTION_KEY, masterKey).apply()
    }

    fun getMasterKey(): String? {
        return prefs.getString(KEY_MASTER_ENCRYPTION_KEY, null)
    }

    fun saveUserSecurityPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = bytesToHex(salt)
        val pinHash = hashPbkdf2Pin(pin, salt)

        prefs.edit()
            .putString(KEY_USER_SECURITY_PIN_HASH, pinHash)
            .putString(KEY_USER_SECURITY_PIN_SALT, saltHex)
            .apply()
    }

    fun verifyUserSecurityPin(inputPin: String): Boolean {
        val storedHash = prefs.getString(KEY_USER_SECURITY_PIN_HASH, null)
        val storedSaltHex = prefs.getString(KEY_USER_SECURITY_PIN_SALT, null)

        if (storedHash == null || storedSaltHex == null) {
            // PIN por defecto predeterminado inicial: 1234
            return inputPin == "1234"
        }

        val salt = hexToBytes(storedSaltHex)
        val inputHash = hashPbkdf2Pin(inputPin, salt)

        return MessageDigest.isEqual(
            inputHash.toByteArray(StandardCharsets.UTF_8),
            storedHash.toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun hashPbkdf2Pin(pin: String, salt: ByteArray): String {
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(pin.toCharArray(), salt, 100000, 256)
            val keyBytes = factory.generateSecret(spec).encoded
            bytesToHex(keyBytes)
        } catch (e: Exception) {
            pin
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
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
