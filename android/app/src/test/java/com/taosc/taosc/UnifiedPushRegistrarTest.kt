package com.taosc.taosc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UnifiedPushRegistrarTest {
    @Test
    fun `discover distributor returns null when none installed`() {
        val discovery = object : DistributorDiscovery {
            override fun findDistributor(): String? = null
        }
        val registrar = UnifiedPushRegistrar(
            broadcastSender = {},
            discovery = discovery,
            prefs = FakeSharedPreferences()
        )
        assertNull(registrar.discoverDistributor())
    }
    
    @Test
    fun `register returns false when no distributor`() {
        val discovery = object : DistributorDiscovery {
            override fun findDistributor(): String? = null
        }
        val registrar = UnifiedPushRegistrar(
            broadcastSender = {},
            discovery = discovery,
            prefs = FakeSharedPreferences()
        )
        assertFalse(registrar.register("com.taosc.taosc") { })
    }
}
