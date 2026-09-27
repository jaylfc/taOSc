package com.taosc.taosc

import org.json.JSONObject
import java.io.IOException

data class SharedFile(val filename: String, val mimeType: String?, val bytes: ByteArray)

sealed class SendResult {
    data object Sent : SendResult()
    data object NotPaired : SendResult()
    data class Rejected(val code: Int) : SendResult()
    data object Unreachable : SendResult()
}

class ShareSender(private val httpClient: HttpClient, private val boundary: () -> String) {

    fun send(
        baseUrl: String,
        token: String,
        destination: ShareDestination,
        item: ShareItem,
        readFile: (uri: String) -> SharedFile
    ): SendResult {
        val headers = mapOf("Authorization" to "Bearer $token")

        return when (destination.kind) {
            ShareDestinationKind.LIBRARY -> sendLibrary(baseUrl, item, headers, readFile)
            ShareDestinationKind.PROJECT_FILES -> sendProjectFile(baseUrl, destination.id, item, headers, readFile)
            ShareDestinationKind.AGENT_CHAT -> {
                val cid = destination.channelId
                if (cid == null) SendResult.Rejected(0) else sendAgentChat(baseUrl, cid, item, headers)
            }
        }
    }

    fun canSend(destination: ShareDestination, items: List<ShareItem>): Boolean {
        if (destination.kind == ShareDestinationKind.AGENT_CHAT) {
            return items.none { it is ShareItem.File }
        }
        return true
    }

    private fun sendLibrary(
        baseUrl: String,
        item: ShareItem,
        headers: Map<String, String>,
        readFile: (uri: String) -> SharedFile
    ): SendResult {
        val uploadRequests = ShareUploadRequests(boundary())
        val request = try {
            when (item) {
                is ShareItem.Text -> {
                    val bytes = item.text.toByteArray(Charsets.UTF_8)
                    uploadRequests.libraryFile(baseUrl, "shared-text.txt", "text/plain; charset=utf-8", bytes, null)
                }
                is ShareItem.Link -> {
                    uploadRequests.libraryLink(baseUrl, item.url, item.title)
                }
                is ShareItem.File -> {
                    val sharedFile = readFile(item.uri)
                    uploadRequests.libraryFile(baseUrl, sharedFile.filename, sharedFile.mimeType, sharedFile.bytes, null)
                }
            }
        } catch (e: Exception) {
            return SendResult.Rejected(0)
        }
        return execute(request, headers)
    }

    private fun sendProjectFile(
        baseUrl: String,
        slug: String,
        item: ShareItem,
        headers: Map<String, String>,
        readFile: (uri: String) -> SharedFile
    ): SendResult {
        val uploadRequests = ShareUploadRequests(boundary())
        val request = try {
            when (item) {
                is ShareItem.Text -> {
                    val bytes = item.text.toByteArray(Charsets.UTF_8)
                    uploadRequests.projectFile(baseUrl, slug, "shared-text.txt", "text/plain; charset=utf-8", bytes)
                }
                is ShareItem.Link -> {
                    val bytes = item.url.toByteArray(Charsets.UTF_8)
                    uploadRequests.projectFile(baseUrl, slug, "shared-text.txt", "text/plain; charset=utf-8", bytes)
                }
                is ShareItem.File -> {
                    val sharedFile = readFile(item.uri)
                    uploadRequests.projectFile(baseUrl, slug, sharedFile.filename, sharedFile.mimeType, sharedFile.bytes)
                }
            }
        } catch (e: Exception) {
            return SendResult.Rejected(0)
        }
        return execute(request, headers)
    }

    private fun sendAgentChat(
        baseUrl: String,
        channelId: String,
        item: ShareItem,
        headers: Map<String, String>
    ): SendResult {
        val content = when (item) {
            is ShareItem.Text -> item.text
            is ShareItem.Link -> {
                if (item.title != null) {
                    item.title + "\n" + item.url
                } else {
                    item.url
                }
            }
            is ShareItem.File -> return SendResult.Rejected(0)
        }

        val json = JSONObject()
        json.put("channel_id", channelId)
        json.put("content", content)

        val body = json.toString().toByteArray(Charsets.UTF_8)
        val requestHeaders = headers + mapOf("Content-Type" to "application/json")
        return try {
            val response = httpClient.postBytes("$baseUrl/api/chat/messages", body, requestHeaders)
            mapResponse(response)
        } catch (e: IOException) {
            SendResult.Unreachable
        }
    }

    private fun execute(request: UploadRequest, headers: Map<String, String>): SendResult {
        return try {
            val response = httpClient.postBytes(request.url, request.body, headers + mapOf("Content-Type" to request.contentType))
            mapResponse(response)
        } catch (e: IOException) {
            SendResult.Unreachable
        }
    }

    private fun mapResponse(response: HttpResponse): SendResult {
        return when (response.code) {
            in 200..299 -> SendResult.Sent
            401 -> SendResult.NotPaired
            403, 404 -> SendResult.Rejected(response.code)
            else -> SendResult.Unreachable
        }
    }
}
