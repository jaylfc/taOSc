package com.taosc.taosc

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSenderTest {
    private fun recordingFake(): Pair<HttpClient, MutableList<RecordedCall>> {
        val calls = mutableListOf<RecordedCall>()
        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                calls.add(RecordedCall(url, headers, body.toByteArray(Charsets.UTF_8)))
                return HttpResponse(200, "")
            }

            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                calls.add(RecordedCall(url, headers, body))
                return HttpResponse(200, "")
            }

            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }

            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        return httpClient to calls
    }

    private data class RecordedCall(
        val url: String,
        val headers: Map<String, String>,
        val body: ByteArray
    )

    private fun assertCall(call: RecordedCall, expectedUrl: String, expectedToken: String) {
        assertEquals(expectedUrl, call.url)
        assertEquals("Bearer $expectedToken", call.headers["Authorization"])
    }

    @Test
    fun `library text sends multipart to library ingest`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Text("hello world")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/library/ingest", "token-123")
        assertEquals("multipart/form-data; boundary=boundary", call.headers["Content-Type"])
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("Content-Disposition: form-data; name=\"file\"; filename=\"shared-text.txt\""))
        assertTrue(bodyString.contains("Content-Type: text/plain; charset=utf-8"))
        assertTrue(bodyString.contains("hello world"))
    }

    @Test
    fun `library link sends multipart to library ingest`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Link("https://example.org/page", "My Link")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/library/ingest", "token-123")
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("name=\"url\""))
        assertTrue(bodyString.contains("https://example.org/page"))
        assertTrue(bodyString.contains("name=\"title\""))
        assertTrue(bodyString.contains("My Link"))
    }

    @Test
    fun `library file sends multipart to library ingest`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.File("content://test/photo.jpg", "image/jpeg")

        val result = sender.send("https://example.com", "token-123", destination, item) {
            SharedFile("photo.jpg", "image/jpeg", "jpg".toByteArray(Charsets.UTF_8))
        }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/library/ingest", "token-123")
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("filename=\"photo.jpg\""))
        assertTrue(bodyString.contains("image/jpeg"))
    }

    @Test
    fun `project files text sends multipart to project upload`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.PROJECT_FILES, "my-project", "My Project")
        val item = ShareItem.Text("hello world")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/projects/my-project/files/upload", "token-123")
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("Content-Disposition: form-data; name=\"file\"; filename=\"shared-text.txt\""))
        assertTrue(bodyString.contains("hello world"))
    }

    @Test
    fun `project files link sends url as file content`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.PROJECT_FILES, "my-project", "My Project")
        val item = ShareItem.Link("https://example.org/page", "My Link")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/projects/my-project/files/upload", "token-123")
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("https://example.org/page"))
        assertFalse(bodyString.contains("name=\"title\""))
    }

    @Test
    fun `project files file sends multipart to project upload`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.PROJECT_FILES, "my-project", "My Project")
        val item = ShareItem.File("content://test/doc.pdf", "application/pdf")

        val result = sender.send("https://example.com", "token-123", destination, item) {
            SharedFile("doc.pdf", "application/pdf", "pdf".toByteArray(Charsets.UTF_8))
        }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/projects/my-project/files/upload", "token-123")
        val bodyString = String(call.body, Charsets.UTF_8)
        assertTrue(bodyString.contains("filename=\"doc.pdf\""))
        assertTrue(bodyString.contains("application/pdf"))
    }

    @Test
    fun `agent chat text sends json message`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val item = ShareItem.Text("hello world")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/chat/messages", "token-123")
        assertEquals("application/json", call.headers["Content-Type"])
        val json = JSONObject(String(call.body, Charsets.UTF_8))
        assertEquals("dm-channel-456", json.getString("channel_id"))
        assertEquals("hello world", json.getString("content"))
    }

    @Test
    fun `agent chat link with title sends title plus url`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val item = ShareItem.Link("https://example.org/page", "My Link")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/chat/messages", "token-123")
        val json = JSONObject(String(call.body, Charsets.UTF_8))
        assertEquals("dm-channel-456", json.getString("channel_id"))
        assertEquals("My Link\nhttps://example.org/page", json.getString("content"))
    }

    @Test
    fun `agent chat link without title sends only url`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val item = ShareItem.Link("https://example.org/page", null)

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Sent, result)
        assertEquals(1, calls.size)
        val call = calls[0]
        assertCall(call, "https://example.com/api/chat/messages", "token-123")
        val json = JSONObject(String(call.body, Charsets.UTF_8))
        assertEquals("dm-channel-456", json.getString("channel_id"))
        assertEquals("https://example.org/page", json.getString("content"))
    }

    @Test
    fun `agent chat file returns rejected without network call`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val item = ShareItem.File("content://test/doc.pdf", "application/pdf")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Rejected(0), result)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `canSend returns false for agent chat with file`() {
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val items = listOf(ShareItem.File("content://test/doc.pdf", "application/pdf"))

        val result = ShareSender(DefaultHttpClient()) { "boundary" }.canSend(destination, items)

        assertFalse(result)
    }

    @Test
    fun `canSend returns true for agent chat with text`() {
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val items = listOf(ShareItem.Text("hello"))

        val result = ShareSender(DefaultHttpClient()) { "boundary" }.canSend(destination, items)

        assertTrue(result)
    }

    @Test
    fun `library text 401 returns not paired`() {
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Text("hello")

        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(401, "")
            }
            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                return HttpResponse(401, "")
            }
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        val sender = ShareSender(httpClient) { "boundary" }
        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.NotPaired, result)
    }

    @Test
    fun `library text 403 returns rejected`() {
        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(403, "")
            }
            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                return HttpResponse(403, "")
            }
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Text("hello")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Rejected(403), result)
    }

    @Test
    fun `library text 404 returns rejected`() {
        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(404, "")
            }
            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                return HttpResponse(404, "")
            }
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Text("hello")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Rejected(404), result)
    }

    @Test
    fun `library text ioexception returns unreachable`() {
        val calls = mutableListOf<RecordedCall>()
        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                calls.add(RecordedCall(url, headers, body.toByteArray(Charsets.UTF_8)))
                throw IOException("offline")
            }
            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                calls.add(RecordedCall(url, headers, body))
                throw IOException("offline")
            }
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.Text("hello")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Unreachable, result)
    }

    @Test
    fun `agent chat text ioexception returns unreachable`() {
        val calls = mutableListOf<RecordedCall>()
        val httpClient = object : HttpClient {
            override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
                throw IOException("offline")
            }
            override fun postBytes(url: String, body: ByteArray, headers: Map<String, String>): HttpResponse {
                calls.add(RecordedCall(url, headers, body))
                throw IOException("offline")
            }
            override fun get(url: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
            override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
                return HttpResponse(200, "")
            }
        }
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat", "dm-channel-456")
        val item = ShareItem.Text("hello world")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Unreachable, result)
    }

    @Test
    fun `library file readfile exception returns rejected with no network call`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.LIBRARY, "library", "Library")
        val item = ShareItem.File("content://test/photo.jpg", "image/jpeg")

        val result = sender.send("https://example.com", "token-123", destination, item) {
            throw IOException("file gone")
        }

        assertEquals(SendResult.Rejected(0), result)
        assertTrue(calls.isEmpty())
    }

    @Test
    fun `agent chat null channelId returns rejected with no network call`() {
        val (httpClient, calls) = recordingFake()
        val sender = ShareSender(httpClient) { "boundary" }
        val destination = ShareDestination(ShareDestinationKind.AGENT_CHAT, "chat-123", "My Chat")
        val item = ShareItem.Text("hello world")

        val result = sender.send("https://example.com", "token-123", destination, item) { throw AssertionError("should not read file") }

        assertEquals(SendResult.Rejected(0), result)
        assertTrue(calls.isEmpty())
    }
}
