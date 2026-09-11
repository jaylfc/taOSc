package com.taosc.taosc

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

interface HttpClient {
    fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): HttpResponse
    fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
    fun patch(url: String, body: String, headers: Map<String, String> = emptyMap()): HttpResponse
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
        
        override fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "PATCH"
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
        
        val json = try {
            JSONObject(response.body)
        } catch (e: org.json.JSONException) {
            throw PairingError.InvalidResponse
        }
        
        return PairRequestResponse(
            pairRequestId = json.optString("pair_request_id").ifEmpty { null } ?: throw PairingError.InvalidResponse,
            verifyCode = json.optString("verify_code").ifEmpty { null } ?: throw PairingError.InvalidResponse
        )
    }
    
    fun pollPairRequest(baseUrl: String, requestId: String): PairRequestStatus {
        val url = "$baseUrl/api/devices/pair-requests/$requestId"
        val response = httpClient.get(url)
        
        when (response.code) {
            200 -> {
                val json = try {
                    JSONObject(response.body)
                } catch (e: org.json.JSONException) {
                    throw PairingError.InvalidResponse
                }
                val pollResponse = PairRequestPollResponse(
                    id = json.optString("id").ifEmpty { null } ?: throw PairingError.InvalidResponse,
                    status = json.optString("status").ifEmpty { null } ?: throw PairingError.InvalidResponse,
                    scopedToken = if (json.isNull("scoped_token")) null else json.optString("scoped_token").ifEmpty { null }
                )
                return pollResponse.requestStatus ?: throw PairingError.InvalidResponse
            }
            404 -> throw PairingError.InvalidResponse
            else -> throw PairingError.Unreachable
        }
    }
    
    fun updatePushToken(baseUrl: String, deviceId: String, pushToken: String, scopedToken: String) {
        val url = "$baseUrl/api/devices/$deviceId/push-token"
        val body = Json.buildObject(
            "push_token" to pushToken
        )
        
        val response = patch(url, body, mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer $scopedToken"
        ))
        
        if (response.code !in 200..299) {
            throw PairingError.Unreachable
        }
    }
    
    private fun patch(url: String, body: String, headers: Map<String, String>): HttpResponse {
        // Implement PATCH using HttpURLConnection
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "PATCH"
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
}

private object Json {
    fun buildObject(vararg pairs: Pair<String, String>): String {
        return pairs.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"${escape(key)}\":\"${escape(value)}\""
        }
    }
    
    private fun escape(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
