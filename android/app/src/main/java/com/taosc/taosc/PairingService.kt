package com.taosc.taosc

import java.net.HttpURLConnection
import java.net.URL

interface HttpClient {
    fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): HttpResponse
    fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
}

data class HttpResponse(
    val code: Int,
    val body: String
)

class DefaultHttpClient : HttpClient {
    override fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        
        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        
        return HttpResponse(responseCode, responseBody)
    }
    
    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        
        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
        
        return HttpResponse(responseCode, responseBody)
    }
}

class PairingService(private val httpClient: HttpClient = DefaultHttpClient()) {
    fun createPairRequest(baseUrl: String, platform: String, displayName: String): PairRequestResponse {
        val url = "$baseUrl/api/devices/pair-requests"
        val body = Json.buildObject(
            "platform" to platform,
            "display_name" to displayName
        )
        
        val response = httpClient.post(url, body, mapOf("Content-Type" to "application/json"))
        
        if (response.code !in 200..299) {
            throw PairingError.Unreachable
        }
        
        return PairRequestResponse(
            pairRequestId = Json.requireString(response.body, "pair_request_id"),
            verifyCode = Json.requireString(response.body, "verify_code")
        )
    }
    
    fun pollPairRequest(baseUrl: String, requestId: String): PairRequestStatus {
        val url = "$baseUrl/api/devices/pair-requests/$requestId"
        val response = httpClient.get(url)
        
        when (response.code) {
            200 -> {
                val pollResponse = PairRequestPollResponse(
                    id = Json.requireString(response.body, "id"),
                    status = Json.requireString(response.body, "status"),
                    scopedToken = Json.optNullableString(response.body, "scoped_token")
                )
                return pollResponse.requestStatus ?: throw PairingError.InvalidResponse
            }
            404 -> throw PairingError.InvalidResponse
            else -> throw PairingError.Unreachable
        }
    }
}

private object Json {
    fun buildObject(vararg pairs: Pair<String, String>): String {
        return pairs.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${escape(key)}\":\"${escape(value)}\""
        }
    }
    
    fun requireString(body: String, key: String): String {
        val pattern = """"$key"\s*:\s*"([^"]*)""".toRegex()
        return pattern.find(body)?.groupValues?.get(1)
            ?: throw PairingError.InvalidResponse
    }
    
    fun optNullableString(body: String, key: String): String? {
        val pattern = """"$key"\s*:\s*(?:"([^"]*)"|null)""".toRegex()
        val match = pattern.find(body) ?: return null
        val value = match.groupValues[1]
        return if (value.isEmpty()) null else value
    }
    
    private fun escape(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
