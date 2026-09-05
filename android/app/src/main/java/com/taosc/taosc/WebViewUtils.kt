package com.taosc.taosc

object WebViewUtils {
    fun isHttpUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
