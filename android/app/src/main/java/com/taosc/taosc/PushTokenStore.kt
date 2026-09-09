package com.taosc.taosc

import android.content.Context
import android.content.SharedPreferences

class PushTokenStore(private val prefs: SharedPreferences) {
    var endpoint: String?
        get() = prefs.getString("push_endpoint", null)
        set(value) = prefs.edit().putString("push_endpoint", value).apply()
    
    fun clear() {
        prefs.edit().remove("push_endpoint").apply()
    }
    
    val hasEndpoint: Boolean
        get() = !endpoint.isNullOrBlank()
}
