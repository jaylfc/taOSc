package com.taosc.taosc

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class ShareDestinationKind {
    LIBRARY,
    PROJECT_FILES,
    AGENT_CHAT
}

data class ShareDestination(
    val kind: ShareDestinationKind,
    val id: String,
    val label: String
)

sealed class ShareDestinationsError : Exception() {
    data object NotPaired : ShareDestinationsError()
    data object Unreachable : ShareDestinationsError()
    data object InvalidResponse : ShareDestinationsError()
}

class ShareDestinationsClient(private val httpClient: HttpClient = DefaultHttpClient()) {

    fun fetch(baseUrl: String, deviceToken: String): List<ShareDestination> {
        val url = "$baseUrl/api/share/destinations"
        val response = httpClient.get(url, mapOf("Authorization" to "Bearer $deviceToken"))

        return when (response.code) {
            200 -> parseDestinations(response.body)
            401 -> throw ShareDestinationsError.NotPaired
            in 200..299 -> throw ShareDestinationsError.InvalidResponse
            else -> throw ShareDestinationsError.Unreachable
        }
    }

    private fun parseDestinations(body: String): List<ShareDestination> {
        val json: JSONObject
        try {
            json = JSONObject(body)
        } catch (e: JSONException) {
            throw ShareDestinationsError.InvalidResponse
        }

        val destinationsArray: JSONArray
        try {
            destinationsArray = json.getJSONArray("destinations")
        } catch (e: JSONException) {
            throw ShareDestinationsError.InvalidResponse
        }

        val destinations = mutableListOf<ShareDestination>()
        for (i in 0 until destinationsArray.length()) {
            val item = destinationsArray.optJSONObject(i)
            if (item == null) {
                throw ShareDestinationsError.InvalidResponse
            }

            val kindString = item.optString("kind").ifEmpty { null }
            val id = item.optString("id").ifEmpty { null }
            val label = item.optString("label").ifEmpty { null }

            if (id == null || label == null) {
                throw ShareDestinationsError.InvalidResponse
            }

            val kind = kindString?.let { parseKind(it) } ?: continue

            destinations.add(ShareDestination(kind, id, label))
        }

        return destinations
    }

    private fun parseKind(kindString: String): ShareDestinationKind? {
        return when (kindString.lowercase()) {
            "library" -> ShareDestinationKind.LIBRARY
            "project_files" -> ShareDestinationKind.PROJECT_FILES
            "agent_chat" -> ShareDestinationKind.AGENT_CHAT
            else -> null
        }
    }
}