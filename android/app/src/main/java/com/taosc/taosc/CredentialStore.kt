package com.taosc.taosc

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface CredentialStore {
    fun saveToken(token: String)
    fun readToken(): String?
    fun deleteToken()
}

class EncryptedCredentialStore private constructor(private val encryptedPrefs: SharedPreferences) : CredentialStore {
    companion object {
        fun create(context: Context): EncryptedCredentialStore {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "taosc_credentials",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            return EncryptedCredentialStore(encryptedPrefs)
        }
        
        private const val TOKEN_KEY = "com.jaylfc.taOSc.scopedToken"
    }
    
    override fun saveToken(token: String) {
        encryptedPrefs.edit().putString(TOKEN_KEY, token).apply()
    }
    
    override fun readToken(): String? {
        return encryptedPrefs.getString(TOKEN_KEY, null)
    }
    
    override fun deleteToken() {
        encryptedPrefs.edit().remove(TOKEN_KEY).apply()
    }
}
