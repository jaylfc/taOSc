package com.taosc.taosc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareUploadRequestsTest {
    @Test
    fun `library file with title`() {
        val request = ShareUploadRequests("boundary").libraryFile(
            baseUrl = "https://example.com",
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            bytes = "image".toByteArray(Charsets.UTF_8),
            title = "Vacation"
        )
        assertEquals("https://example.com/api/library/ingest", request.url)
        assertEquals("multipart/form-data; boundary=boundary", request.contentType)
        val content = String(request.body, Charsets.UTF_8)
        assertTrue(content.contains("Vacation"))
        assertTrue(content.contains("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\""))
    }

    @Test
    fun `library file without title`() {
        val request = ShareUploadRequests("boundary").libraryFile(
            baseUrl = "https://example.com",
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            bytes = "image".toByteArray(Charsets.UTF_8),
            title = null
        )
        val content = String(request.body, Charsets.UTF_8)
        assertFalse(content.contains("name=\"title\""))
    }

    @Test
    fun `library file with blank title`() {
        val request = ShareUploadRequests("boundary").libraryFile(
            baseUrl = "https://example.com",
            filename = "photo.jpg",
            mimeType = "image/jpeg",
            bytes = "image".toByteArray(Charsets.UTF_8),
            title = "   "
        )
        val content = String(request.body, Charsets.UTF_8)
        assertFalse(content.contains("name=\"title\""))
    }

    @Test
    fun `library link`() {
        val request = ShareUploadRequests("boundary").libraryLink(
            baseUrl = "https://example.com",
            url = "https://example.org/page",
            title = "My Link"
        )
        assertEquals("https://example.com/api/library/ingest", request.url)
        assertEquals("multipart/form-data; boundary=boundary", request.contentType)
        val content = String(request.body, Charsets.UTF_8)
        assertTrue(content.contains("name=\"url\""))
        assertTrue(content.contains("https://example.org/page"))
        assertTrue(content.contains("name=\"title\""))
        assertTrue(content.contains("My Link"))
    }

    @Test
    fun `library link without title`() {
        val request = ShareUploadRequests("boundary").libraryLink(
            baseUrl = "https://example.com",
            url = "https://example.org/page",
            title = null
        )
        val content = String(request.body, Charsets.UTF_8)
        assertFalse(content.contains("name=\"title\""))
    }

    @Test
    fun `project file slug with space and slash`() {
        val request = ShareUploadRequests("boundary").projectFile(
            baseUrl = "https://example.com",
            slug = "a/b c",
            filename = "doc.pdf",
            mimeType = "application/pdf",
            bytes = "pdf".toByteArray(Charsets.UTF_8)
        )
        assertEquals("https://example.com/api/projects/a%2Fb%20c/files/upload", request.url)
    }

    @Test
    fun `null mimeType defaults to application octet stream`() {
        val request = ShareUploadRequests("boundary").libraryFile(
            baseUrl = "https://example.com",
            filename = "unknown.bin",
            mimeType = null,
            bytes = byteArrayOf(0x00.toByte(), 0xFF.toByte()),
            title = null
        )
        val content = String(request.body, Charsets.UTF_8)
        assertTrue(content.contains("Content-Type: application/octet-stream"))
    }

    @Test
    fun `upload request equals compares body bytes`() {
        val bytes = byteArrayOf(0x00.toByte(), 0xFF.toByte())
        val a = UploadRequest("url", "type", bytes)
        val b = UploadRequest("url", "type", byteArrayOf(0x00.toByte(), 0xFF.toByte()))
        val c = UploadRequest("url", "type", byteArrayOf(0x00.toByte(), 0xFE.toByte()))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertFalse(a.equals(c))
    }
}
