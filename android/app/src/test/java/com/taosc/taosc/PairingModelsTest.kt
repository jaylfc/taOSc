package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingModelsTest {
    @Test
    fun `pending poll response returns pending status`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "pending",
            scopedToken = null
        )
        assertEquals(PairRequestStatus.Pending, response.requestStatus)
    }
    
    @Test
    fun `approved poll response with token returns approved status`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "approved",
            scopedToken = "token-abc"
        )
        assertEquals(PairRequestStatus.Approved("token-abc"), response.requestStatus)
    }
    
    @Test
    fun `denied poll response returns denied status`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "denied",
            scopedToken = null
        )
        assertEquals(PairRequestStatus.Denied, response.requestStatus)
    }
    
    @Test
    fun `expired poll response returns expired status`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "expired",
            scopedToken = null
        )
        assertEquals(PairRequestStatus.Expired, response.requestStatus)
    }
    
    @Test
    fun `mixed case status string is recognized`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "PENDING",
            scopedToken = null
        )
        assertEquals(PairRequestStatus.Pending, response.requestStatus)
    }
    
    @Test
    fun `unknown status string returns null`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "unknown",
            scopedToken = null
        )
        assertNull(response.requestStatus)
    }
    
    @Test
    fun `approved response with null token is invalid`() {
        val response = PairRequestPollResponse(
            id = "req-123",
            status = "approved",
            scopedToken = null
        )
        assertNull(response.requestStatus)
    }
}
