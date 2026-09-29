package com.taosc.taosc

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class ShareRetryPolicyTest {

    @Test
    fun `sent returns done`() {
        val result = decide(SendResult.Sent, 1)
        assertEquals(RetryDecision.Done, result)
    }

    @Test
    fun `not paired gives up immediately`() {
        val result = decide(SendResult.NotPaired, 1)
        assertEquals(RetryDecision.GiveUp("NotPaired"), result)
    }

    @Test
    fun `rejected gives up immediately`() {
        val result = decide(SendResult.Rejected(403), 1)
        assertEquals(RetryDecision.GiveUp("Rejected"), result)
    }

    @Test
    fun `unreachable retry later when attempt less than 8`() {
        val result = decide(SendResult.Unreachable, 7)
        assertEquals(RetryDecision.RetryLater, result)
    }

    @Test
    fun `unreachable give up at attempt 8`() {
        val result = decide(SendResult.Unreachable, 8)
        assertEquals(RetryDecision.GiveUp("Max attempts reached"), result)
    }

    @Test
    fun `unreachable give up beyond attempt 8`() {
        val result = decide(SendResult.Unreachable, 10)
        assertEquals(RetryDecision.GiveUp("Max attempts reached"), result)
    }

    @Test
    fun `giveUp hasNonBlankReason`() {
        val result = decide(SendResult.NotPaired, 1)
        assertNotNull(result.reason)
        assertTrue(result.reason.isNotBlank())

        val result2 = decide(SendResult.Rejected(403), 1)
        assertNotNull(result2.reason)
        assertTrue(result2.reason.isNotBlank())
    }
}