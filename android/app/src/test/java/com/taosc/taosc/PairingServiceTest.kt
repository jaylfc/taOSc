package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingServiceTest {
    @Test
    fun `createPairRequest accepts 201`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests" to HttpResponse(
                201,
                """{"pair_request_id":"req-123","verify_code":"481905"}"""
            )
        ))
        val service = PairingService(client)
        val response = service.createPairRequest("https://example.com", "android", "Jay's Phone")
        assertEquals("req-123", response.pairRequestId)
        assertEquals("481905", response.verifyCode)
    }
    
    @Test
    fun `createPairRequest rejects non 2xx`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests" to HttpResponse(500, "")
        ))
        val service = PairingService(client)
        assertThrows(PairingError.Unreachable::class.java) {
            service.createPairRequest("https://example.com", "android", "Jay's Phone")
        }
    }
    
    @Test
    fun `pollPairRequest returns pending`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"pending","scoped_token":null}"""
            )
        ))
        val service = PairingService(client)
        assertEquals(PairRequestStatus.Pending, service.pollPairRequest("https://example.com", "req-123"))
    }
    
    @Test
    fun `pollPairRequest returns approved with token`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"approved","scoped_token":"token-abc"}"""
            )
        ))
        val service = PairingService(client)
        assertEquals(PairRequestStatus.Approved("token-abc", "req-123"), service.pollPairRequest("https://example.com", "req-123"))
    }
    
    @Test
    fun `pollPairRequest returns denied`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"denied","scoped_token":null}"""
            )
        ))
        val service = PairingService(client)
        assertEquals(PairRequestStatus.Denied, service.pollPairRequest("https://example.com", "req-123"))
    }
    
    @Test
    fun `pollPairRequest returns expired`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"expired","scoped_token":null}"""
            )
        ))
        val service = PairingService(client)
        assertEquals(PairRequestStatus.Expired, service.pollPairRequest("https://example.com", "req-123"))
    }
    
    @Test
    fun `pollPairRequest 404 throws invalid response`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(404, "")
        ))
        val service = PairingService(client)
        assertThrows(PairingError.InvalidResponse::class.java) {
            service.pollPairRequest("https://example.com", "req-123")
        }
    }
    
    @Test
    fun `pollPairRequest other error throws unreachable`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(500, "")
        ))
        val service = PairingService(client)
        assertThrows(PairingError.Unreachable::class.java) {
            service.pollPairRequest("https://example.com", "req-123")
        }
    }
    
    @Test
    fun `pollPairRequest unknown status throws invalid response`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"unknown","scoped_token":null}"""
            )
        ))
        val service = PairingService(client)
        assertThrows(PairingError.InvalidResponse::class.java) {
            service.pollPairRequest("https://example.com", "req-123")
        }
    }
    
    @Test
    fun `pollPairRequest approved without token throws invalid response`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/pair-requests/req-123" to HttpResponse(
                200,
                """{"id":"req-123","status":"approved","scoped_token":null}"""
            )
        ))
        val service = PairingService(client)
        assertThrows(PairingError.InvalidResponse::class.java) {
            service.pollPairRequest("https://example.com", "req-123")
        }
    }
    
    @Test
    fun `updatePushToken posts to correct endpoint`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/device-123/push-token" to HttpResponse(200, "{}")
        ))
        val service = PairingService(client)
        service.updatePushToken("https://example.com", "device-123", "https://push.example.com/ep", "token-abc")
    }
    
    @Test
    fun `updatePushToken throws on non-2xx`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/devices/device-123/push-token" to HttpResponse(500, "")
        ))
        val service = PairingService(client)
        assertThrows(PairingError.Unreachable::class.java) {
            service.updatePushToken("https://example.com", "device-123", "https://push.example.com/ep", "token-abc")
        }
    }
}

class FakeHttpClient(private val responses: MutableMap<String, HttpResponse> = mutableMapOf()) : HttpClient {
    override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
        return responses[url] ?: HttpResponse(500, "")
    }
    
    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        return responses[url] ?: HttpResponse(500, "")
    }
}
