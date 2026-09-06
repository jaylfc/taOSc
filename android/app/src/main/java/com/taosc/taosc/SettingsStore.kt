package com.taosc.taosc

import android.content.Context
import android.content.SharedPreferences

class SettingsStore(private val prefs: SharedPreferences) {
    var serverUrl: String
        get() = prefs.getString("serverURL", "") ?: ""
        set(value) = prefs.edit().putString("serverURL", value).apply()
    
    val hasServerUrl: Boolean
        get() = serverUrl.isNotBlank()
    
    fun isPaired(credentialStore: CredentialStore): Boolean {
        return credentialStore.readToken() != null
    }
    
    fun logout(credentialStore: CredentialStore) {
        serverUrl = ""
        credentialStore.deleteToken()
    }
}
