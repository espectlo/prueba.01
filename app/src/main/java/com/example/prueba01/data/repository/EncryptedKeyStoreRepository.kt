package com.example.prueba01.data.repository

import android.content.Context
import android.content.SharedPreferences

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

    fun savePinHashAndSalt(pinHash: String, saltHex: String) {
        prefs.edit()
            .putString(KEY_USER_SECURITY_PIN_HASH, pinHash)
            .putString(KEY_USER_SECURITY_PIN_SALT, saltHex)
            .apply()
    }

    fun getPinHash(): String? = prefs.getString(KEY_USER_SECURITY_PIN_HASH, null)
    fun getPinSalt(): String? = prefs.getString(KEY_USER_SECURITY_PIN_SALT, null)
}
