package com.taosc.taosc

sealed class ShareItem {
    data class Text(val text: String) : ShareItem()
    data class Link(val url: String, val title: String?) : ShareItem()
    data class File(val uri: String, val mimeType: String?) : ShareItem()
}

object ShareIntakeParser {
    fun parse(
        action: String?,
        mimeType: String?,
        text: String?,
        subject: String?,
        streamUris: List<String>
    ): List<ShareItem> {
        if (action != "android.intent.action.SEND" && action != "android.intent.action.SEND_MULTIPLE") {
            return emptyList()
        }

        val items = mutableListOf<ShareItem>()

        val trimmedText = text?.trim()
        if (!trimmedText.isNullOrBlank()) {
            if (isUrl(trimmedText)) {
                val linkTitle = subject?.trim()?.ifBlank { null }
                items.add(ShareItem.Link(trimmedText, linkTitle))
            } else {
                items.add(ShareItem.Text(trimmedText))
            }
        }

        for (uri in streamUris) {
            items.add(ShareItem.File(uri, mimeType))
        }

        return items
    }

    private fun isUrl(value: String): Boolean {
        val lower = value.lowercase()
        return (lower.startsWith("http://") || lower.startsWith("https://")) &&
            !lower.contains(" ") &&
            !lower.contains("\n") &&
            !lower.contains("\r")
    }
}
