package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShareDestinationsClientTest {

    @Test
    fun `fetch parses all three kinds in server order`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","id":"library","label":"Library"},
                    {"kind":"project_files","id":"proj-123","label":"My Project"},
                    {"kind":"agent_chat","id":"agent-456","label":"Agent Chat"}
                ]}"""
            )
        ))

        val destinations = ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")

        assertEquals(3, destinations.size)
        assertEquals(ShareDestinationKind.LIBRARY, destinations[0].kind)
        assertEquals("library", destinations[0].id)
        assertEquals("Library", destinations[0].label)

        assertEquals(ShareDestinationKind.PROJECT_FILES, destinations[1].kind)
        assertEquals("proj-123", destinations[1].id)
        assertEquals("My Project", destinations[1].label)

        assertEquals(ShareDestinationKind.AGENT_CHAT, destinations[2].kind)
        assertEquals("agent-456", destinations[2].id)
        assertEquals("Agent Chat", destinations[2].label)
    }

    @Test
    fun `fetch sends Authorization header with Bearer token`() {
        var recordedHeaders: Map<String, String>? = null

        val client = object : FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(200, """{"destinations":[]}""")
        )) {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                recordedHeaders = headers
                return super.get(url, headers)
            }
        }

        ShareDestinationsClient(client).fetch("https://example.com", "my-device-token")

        assertEquals("Bearer my-device-token", recordedHeaders?.get("Authorization"))
    }

    @Test
    fun `fetch skips unknown kind`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","id":"library","label":"Library"},
                    {"kind":"unknown_kind","id":"unknown-1","label":"Unknown"},
                    {"kind":"project_files","id":"proj-123","label":"My Project"}
                ]}"""
            )
        ))

        val destinations = ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")

        assertEquals(2, destinations.size)
        assertEquals(ShareDestinationKind.LIBRARY, destinations[0].kind)
        assertEquals(ShareDestinationKind.PROJECT_FILES, destinations[1].kind)
    }

    @Test
    fun `fetch missing label throws InvalidResponse`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","id":"library","label":"Library"},
                    {"kind":"project_files","id":"proj-123"}
                ]}"""
            )
        ))

        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch missing id throws InvalidResponse`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","label":"Library"}
                ]}"""
            )
        ))

        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch missing destinations array throws InvalidResponse`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                """{"other_field":"value"}"""
            )
        ))

        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch non-JSON body throws InvalidResponse`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(
                200,
                "not valid json"
            )
        ))

        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch 401 throws NotPaired`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(401, "")
        ))

        assertThrows(ShareDestinationsError.NotPaired::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch 503 throws Unreachable`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(503, "")
        ))

        assertThrows(ShareDestinationsError.Unreachable::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }

    @Test
    fun `fetch 404 throws Unreachable`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://example.com/api/share/destinations" to HttpResponse(404, "")
        ))

        assertThrows(ShareDestinationsError.Unreachable::class.java) {
            ShareDestinationsClient(client).fetch("https://example.com", "device-token-abc")
        }
    }
}