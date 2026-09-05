package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialStoreTest {
    @Test
    fun `save and read token`() {
        val store = FakeCredentialStore()
        store.saveToken("test-token-123")
        assertEquals("test-token-123", store.readToken())
    }
    
    @Test
    fun `delete token`() {
        val store = FakeCredentialStore()
        store.saveToken("token")
        store.deleteToken()
        assertNull(store.readToken())
    }
    
    @Test
    fun `read token when none exists`() {
        val store = FakeCredentialStore()
        assertNull(store.readToken())
    }
    
    @Test
    fun `overwrite token`() {
        val store = FakeCredentialStore()
        store.saveToken("first")
        store.saveToken("second")
        assertEquals("second", store.readToken())
    }
}
