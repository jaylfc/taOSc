package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsStoreTest {
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var settingsStore: SettingsStore
    private lateinit var credentialStore: FakeCredentialStore
    
    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        credentialStore = FakeCredentialStore()
        settingsStore = SettingsStore(prefs)
    }
    
    @Test
    fun `initial state has no url`() {
        assertFalse(settingsStore.hasServerUrl)
        assertEquals("", settingsStore.serverUrl)
    }
    
    @Test
    fun `saving url persists`() {
        settingsStore.serverUrl = "https://taos.example.com"
        assertEquals("https://taos.example.com", settingsStore.serverUrl)
        assertTrue(settingsStore.hasServerUrl)
    }
    
    @Test
    fun `url persists across instances`() {
        settingsStore.serverUrl = "https://taos.example.com"
        val newSettings = SettingsStore(prefs)
        assertEquals("https://taos.example.com", newSettings.serverUrl)
        assertTrue(newSettings.hasServerUrl)
    }
    
    @Test
    fun `updating url`() {
        settingsStore.serverUrl = "https://first.example.com"
        settingsStore.serverUrl = "https://second.example.com"
        assertEquals("https://second.example.com", settingsStore.serverUrl)
    }
    
    @Test
    fun `clearing url`() {
        settingsStore.serverUrl = "https://taos.example.com"
        settingsStore.serverUrl = ""
        assertEquals("", settingsStore.serverUrl)
        assertFalse(settingsStore.hasServerUrl)
    }
    
    @Test
    fun `isPaired returns true when token exists`() {
        credentialStore.saveToken("token-123")
        assertTrue(settingsStore.isPaired(credentialStore))
    }
    
    @Test
    fun `isPaired returns false when no token`() {
        assertFalse(settingsStore.isPaired(credentialStore))
    }
    
    @Test
    fun `logout clears url and token`() {
        settingsStore.serverUrl = "https://taos.example.com"
        credentialStore.saveToken("token-123")
        settingsStore.logout(credentialStore)
        assertEquals("", settingsStore.serverUrl)
        assertFalse(settingsStore.hasServerUrl)
        assertNull(credentialStore.readToken())
    }
}

class FakeSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()
    
    override fun getAll() = map.toMap()
    override fun getString(key: String, defValue: String?) = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?) = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int) = (map[key] as? Int) ?: defValue
    override fun getLong(key: String, defValue: Long) = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String, defValue: Float) = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String) = map.containsKey(key)
    
    override fun edit() = FakeEditor()
    
    inner class FakeEditor : android.content.SharedPreferences.Editor {
        private val changes = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        
        override fun putString(key: String, value: String?) = apply { changes[key] = value; removals.remove(key) }
        override fun putStringSet(key: String, values: Set<String>?) = apply { changes[key] = values; removals.remove(key) }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value; removals.remove(key) }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value; removals.remove(key) }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value; removals.remove(key) }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value; removals.remove(key) }
        override fun remove(key: String) = apply { removals.add(key); changes.remove(key) }
        override fun clear() = apply { changes.clear(); removals.clear() }
        
        override fun commit(): Boolean {
            removals.forEach { map.remove(it) }
            changes.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v }
            return true
        }
        
        override fun apply() {
            commit()
        }
    }
    
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

class FakeCredentialStore : CredentialStore {
    private var token: String? = null
    
    override fun saveToken(token: String) {
        this.token = token
    }
    
    override fun readToken(): String? {
        return token
    }
    
    override fun deleteToken() {
        token = null
    }
}
