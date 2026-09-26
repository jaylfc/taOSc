package com.taosc.taosc

import org.json.JSONObject

enum class ShareDestinationKind { LIBRARY, PROJECT_FILES, AGENT_CHAT }

data class ShareDestination(val kind: ShareDestinationKind, val id: String, val label: String)

sealed class ShareDestinationsError : Exception() {
    data object NotPaired : ShareDestinationsError()
    data object Unreachable : ShareDestinationsError()
    data object InvalidResponse : ShareDestinationsError()
}

class ShareDestinationsClient(private val httpClient: HttpClient = DefaultHttpClient()) {
    fun fetch(baseUrl: String, deviceToken: String): List<ShareDestination> {
        val url = "$baseUrl/api/share/destinations"
        val response = httpClient.get(
            url,
            mapOf("Authorization" to "Bearer $deviceToken")
        )

        if (response.code !in 200..299) {
            when (response.code) {
                401 -> throw ShareDestinationsError.NotPaired
                else -> throw ShareDestinationsError.Unreachable
            }
        }

        return try {
            val json = JSONObject(response.body)
            if (!json.has("destinations")) {
                throw ShareDestinationsError.InvalidResponse
            }
            val destinations = mutableListOf<ShareDestination>()
            val jsonArray = json.getJSONArray("destinations")
            for (i in 0 until jsonArray.length()) {
                val destinationJson = jsonArray.getJSONObject(i)
                val kindStr = destinationJson.optString("kind", "")
                val id = destinationJson.optString("id", "")
                val label = destinationJson.optString("label", "")

                if (id.isEmpty() || label.isEmpty()) {
                    throw ShareDestinationsError.InvalidResponse
                }

                val kind = try {
                    ShareDestinationKind.valueOf(kindStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    continue
                }

                destinations.add(ShareDestination(kind, id, label))
            }
            destinations
        } catch (e: org.json.JSONException) {
            throw ShareDestinationsError.InvalidResponse
        }
    }
}
