package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareRetryPolicyTest {
    @Test
    fun `sent results in Done`() {
        val result = SendResult.Sent
        val attempt = 1
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.Done, decision)
    }

    @Test
    fun `not paired results in GiveUp with Pair this phone again`() {
        val result = SendResult.NotPaired
        val attempt = 1
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.GiveUp("Pair this phone again"), decision)
    }

    @Test
    fun `rejected results in GiveUp with taOS refused this destination`() {
        val result = SendResult.Rejected(403)
        val attempt = 1
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.GiveUp("taOS refused this destination"), decision)
    }

    @Test
    fun `unreachable with attempt less than 8 results in RetryLater`() {
        val result = SendResult.Unreachable
        val attempt = 1
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.RetryLater, decision)
    }

    @Test
    fun `unreachable with attempt 8 results in GiveUp with Could not reach taOS`() {
        val result = SendResult.Unreachable
        val attempt = 8
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.GiveUp("Could not reach taOS"), decision)
    }

    @Test
    fun `unreachable with attempt greater than 8 results in GiveUp with Could not reach taOS`() {
        val result = SendResult.Unreachable
        val attempt = 9
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.GiveUp("Could not reach taOS"), decision)
    }

    @Test
    fun `unreachable with attempt 7 results in RetryLater`() {
        val result = SendResult.Unreachable
        val attempt = 7
        val decision = decide(result, attempt)
        assertEquals(RetryDecision.RetryLater, decision)
    }
}