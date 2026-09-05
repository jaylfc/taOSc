package com.taosc.taosc

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewUtilsTest {
    @Test
    fun `http url is valid`() {
        assertTrue(WebViewUtils.isHttpUrl("http://example.com"))
    }

    @Test
    fun `https url is valid`() {
        assertTrue(WebViewUtils.isHttpUrl("https://example.com"))
    }

    @Test
    fun `non http url is invalid`() {
        assertFalse(WebViewUtils.isHttpUrl("ftp://example.com"))
    }

    @Test
    fun `empty url is invalid`() {
        assertFalse(WebViewUtils.isHttpUrl(""))
    }

    @Test
    fun `file url is invalid`() {
        assertFalse(WebViewUtils.isHttpUrl("file:///path/to/file"))
    }

    @Test
    fun `javascript url is invalid`() {
        assertFalse(WebViewUtils.isHttpUrl("javascript:alert('xss')"))
    }

    @Test
    fun `data url is invalid`() {
        assertFalse(WebViewUtils.isHttpUrl("data:text/html,<h1>Hello</h1>"))
    }
}
