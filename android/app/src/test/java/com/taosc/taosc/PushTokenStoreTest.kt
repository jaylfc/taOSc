package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PushTokenStoreTest {
    private lateinit var prefs: FakeSharedPreferences
    private lateinit var store: PushTokenStore
    
    @Before
    fun setUp() {
        prefs = FakeSharedPreferences()
        store = PushTokenStore(prefs)
    }
    
    @Test
    fun `initial state has no endpoint`() {
        assertNull(store.endpoint)
        assertFalse(store.hasEndpoint)
    }
    
    @Test
    fun `saving endpoint persists`() {
        store.endpoint = "https://push.example.com/endpoint"
        assertEquals("https://push.example.com/endpoint", store.endpoint)
        assertTrue(store.hasEndpoint)
    }
    
    @Test
    fun `clearing endpoint removes it`() {
        store.endpoint = "https://push.example.com/endpoint"
        store.clear()
        assertNull(store.endpoint)
        assertFalse(store.hasEndpoint)
    }
}
