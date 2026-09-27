package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShareDestinationsClientTest {

    @Test
    fun `fetch returns all three destination kinds in order`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","id":"library","label":"Library"},
                    {"kind":"project_files","id":"my-project","label":"My Project"},
                    {"kind":"agent_chat","id":"chat-123","label":"My Chat"}
                ]}"""
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        val destinations = destinationsClient.fetch("https://api.example.com", "token-123")

        assertEquals(3, destinations.size)
        assertEquals(ShareDestinationKind.LIBRARY, destinations[0].kind)
        assertEquals("library", destinations[0].id)
        assertEquals("Library", destinations[0].label)
        assertEquals(ShareDestinationKind.PROJECT_FILES, destinations[1].kind)
        assertEquals("my-project", destinations[1].id)
        assertEquals("My Project", destinations[1].label)
        assertEquals(ShareDestinationKind.AGENT_CHAT, destinations[2].kind)
        assertEquals("chat-123", destinations[2].id)
        assertEquals("My Chat", destinations[2].label)
    }

    @Test
    fun `fetch sends correct Authorization header`() {
        var capturedHeaders: Map<String, String>? = null
        val client = object : FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[]}"""
            )
        )) {
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                capturedHeaders = headers
                return responses[url] ?: HttpResponse(500, "")
            }
        }
        val destinationsClient = ShareDestinationsClient(client)
        destinationsClient.fetch("https://api.example.com", "my-token")

        assertEquals("Bearer my-token", capturedHeaders!!["Authorization"])
    }

    @Test
    fun `fetch skips unknown kind and processes known ones`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[
                    {"kind":"library","id":"lib","label":"Library"},
                    {"kind":"unknown_kind","id":"ignore","label":"Ignore This"},
                    {"kind":"agent_chat","id":"chat-1","label":"Chat 1"}
                ]}"""
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        val destinations = destinationsClient.fetch("https://api.example.com", "token")

        assertEquals(2, destinations.size)
        assertEquals(ShareDestinationKind.LIBRARY, destinations[0].kind)
        assertEquals(ShareDestinationKind.AGENT_CHAT, destinations[1].kind)
    }

    @Test
    fun `fetch throws InvalidResponse when destinations array missing`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"wrong_key":[]}"""
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }

    @Test
    fun `fetch throws InvalidResponse when destination missing id`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[{"kind":"library","label":"Library"}]}"""
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }

    @Test
    fun `fetch throws InvalidResponse when destination missing label`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                """{"destinations":[{"kind":"library","id":"lib"}]}"""
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }

    @Test
    fun `fetch throws InvalidResponse when body is not valid JSON`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(
                200,
                "not json"
            )
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.InvalidResponse::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }

    @Test
    fun `fetch throws NotPaired on 401`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(401, "")
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.NotPaired::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }

    @Test
    fun `fetch throws Unreachable on other error codes`() {
        val client = FakeHttpClient(mutableMapOf(
            "https://api.example.com/api/share/destinations" to HttpResponse(503, "")
        ))
        val destinationsClient = ShareDestinationsClient(client)
        assertThrows(ShareDestinationsError.Unreachable::class.java) {
            destinationsClient.fetch("https://api.example.com", "token")
        }
    }
}
