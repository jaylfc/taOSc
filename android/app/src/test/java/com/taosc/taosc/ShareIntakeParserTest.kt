package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntakeParserTest {
    @Test
    fun `parse https url with subject returns Link`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "https://example.com/article",
            subject = "Example Article",
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val link = items[0] as ShareItem.Link
        assertEquals("https://example.com/article", link.url)
        assertEquals("Example Article", link.title)
    }

    @Test
    fun `parse url with surrounding whitespace is trimmed`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "  https://example.com/page  ",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val link = items[0] as ShareItem.Link
        assertEquals("https://example.com/page", link.url)
    }

    @Test
    fun `parse text containing url plus other words stays Text`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "Check out https://example.com for details",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val text = items[0] as ShareItem.Text
        assertEquals("Check out https://example.com for details", text.text)
    }

    @Test
    fun `parse sendMultiple with two uris returns two Files`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND_MULTIPLE",
            mimeType = "image/*",
            text = null,
            subject = null,
            streamUris = listOf("content://1", "content://2")
        )

        assertEquals(2, items.size)
        val file0 = items[0] as ShareItem.File
        val file1 = items[1] as ShareItem.File
        assertEquals("content://1", file0.uri)
        assertEquals("image/*", file0.mimeType)
        assertEquals("content://2", file1.uri)
        assertEquals("image/*", file1.mimeType)
    }

    @Test
    fun `parse text plus stream yields text first then file`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "Hello world",
            subject = null,
            streamUris = listOf("content://file")
        )

        assertEquals(2, items.size)
        assertTrue(items[0] is ShareItem.Text)
        assertTrue(items[1] is ShareItem.File)
        assertEquals("Hello world", (items[0] as ShareItem.Text).text)
        assertEquals("content://file", (items[1] as ShareItem.File).uri)
    }

    @Test
    fun `parse unknown action returns empty list`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.VIEW",
            mimeType = "*/*",
            text = "hello",
            subject = null,
            streamUris = emptyList()
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `parse blank text is dropped`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "   ",
            subject = null,
            streamUris = emptyList()
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `parse url with blank subject yields Link with null title`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "https://example.com",
            subject = "   ",
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val link = items[0] as ShareItem.Link
        assertEquals("https://example.com", link.url)
        assertEquals(null, link.title)
    }

    @Test
    fun `parse send with text plain returns Text`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "plain text",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val text = items[0] as ShareItem.Text
        assertEquals("plain text", text.text)
    }

    @Test
    fun `parse send image with stream returns File`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "image/png",
            text = null,
            subject = null,
            streamUris = listOf("content://img1")
        )

        assertEquals(1, items.size)
        val file = items[0] as ShareItem.File
        assertEquals("content://img1", file.uri)
        assertEquals("image/png", file.mimeType)
    }

    @Test
    fun `parse sendMultiple with wildcard mime returns Files`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND_MULTIPLE",
            mimeType = "*/*",
            text = null,
            subject = null,
            streamUris = listOf("content://a", "content://b", "content://c")
        )

        assertEquals(3, items.size)
        (items[0] as ShareItem.File).run { assertEquals("*/*", mimeType) }
        (items[1] as ShareItem.File).run { assertEquals("*/*", mimeType) }
        (items[2] as ShareItem.File).run { assertEquals("*/*", mimeType) }
    }

    @Test
    fun `parse null action returns empty list`() {
        val items = ShareIntakeParser.parse(
            action = null,
            mimeType = "text/plain",
            text = "hello",
            subject = null,
            streamUris = emptyList()
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun `parse empty streamUris with text returns only text`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "only text",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        assertTrue(items[0] is ShareItem.Text)
    }

    @Test
    fun `parse http url is recognized`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "http://example.com",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val link = items[0] as ShareItem.Link
        assertEquals("http://example.com", link.url)
        assertEquals(null, link.title)
    }

    @Test
    fun `parse text with newline is not treated as url`() {
        val items = ShareIntakeParser.parse(
            action = "android.intent.action.SEND",
            mimeType = "text/plain",
            text = "http://example.com\nmore",
            subject = null,
            streamUris = emptyList()
        )

        assertEquals(1, items.size)
        val text = items[0] as ShareItem.Text
        assertEquals("http://example.com\nmore", text.text)
    }
}
