package com.taosc.taosc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MultipartBodyTest {
    @Test
    fun `one field body has exact bytes`() {
        val multipart = MultipartBody("boundary")
        multipart.addField("name", "value")
        val body = multipart.toByteArray()
        val expected = "--boundary\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\nvalue\r\n--boundary--\r\n"
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), body)
    }

    @Test
    fun `one file body has exact bytes`() {
        val multipart = MultipartBody("boundary")
        multipart.addFile("file", "hello.txt", "text/plain", "hello".toByteArray(Charsets.UTF_8))
        val body = multipart.toByteArray()
        val expected = "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"hello.txt\"\r\nContent-Type: text/plain\r\n\r\nhello\r\n--boundary--\r\n"
        assertArrayEquals(expected.toByteArray(Charsets.UTF_8), body)
    }

    @Test
    fun `terminator appears exactly once`() {
        val multipart = MultipartBody("boundary")
        multipart.addField("a", "1")
        multipart.addField("b", "2")
        multipart.addFile("c", "c.txt", "text/plain", "3".toByteArray(Charsets.UTF_8))
        val body = multipart.toByteArray()
        val content = String(body, Charsets.UTF_8)
        val count = content.split("--boundary--").size - 1
        assertEquals(1, count)
    }

    @Test
    fun `binary file bytes survive unchanged`() {
        val multipart = MultipartBody("boundary")
        val bytes = byteArrayOf(0x00.toByte(), 0xFF.toByte(), 0x01.toByte(), 0xFE.toByte())
        multipart.addFile("file", "binary.bin", "application/octet-stream", bytes)
        val body = multipart.toByteArray()

        val header = "--boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"binary.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n"
        val headerBytes = header.toByteArray(Charsets.UTF_8)
        val footerBytes = "\r\n--boundary--\r\n".toByteArray(Charsets.UTF_8)
        val fileContent = body.sliceArray(headerBytes.size until body.size - footerBytes.size)
        assertArrayEquals(bytes, fileContent)
    }

    @Test
    fun `hostile filename cannot inject header`() {
        val multipart = MultipartBody("boundary")
        multipart.addFile("file", "evil\"\r\nContent-Type: foo\r\n\r\ninjected", "text/plain", "safe".toByteArray(Charsets.UTF_8))
        val body = multipart.toByteArray()
        val content = String(body, Charsets.UTF_8)
        val lines = content.split("\r\n")
        assertFalse("Unexpected injected header", lines.any { it.startsWith("Content-Type: foo") })
    }
}
