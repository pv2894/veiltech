package com.veiltech.demo.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "veiltech_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) = prefs.edit().putString("jwt", token).apply()
    fun token(): String? = prefs.getString("jwt", null)
    fun saveUserId(id: Long) = prefs.edit().putLong("userId", id).apply()
    fun userId(): Long = prefs.getLong("userId", -1)
}
