package com.taosc.taosc

import org.json.JSONObject

object DecisionPayloadParser {
    fun parse(json: String): DecisionPayload? {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: JSONObject()
            val decisionType = data.optString("decision_type")
            
            if (decisionType.isBlank()) {
                return null
            }
            
            val actions = parseActions(decisionType, data)
            val image = data.optString("image").ifEmpty { null }
            
            DecisionPayload(
                title = root.optString("title").ifEmpty { "taOS" },
                body = root.optString("body").ifEmpty { "" },
                decisionType = decisionType,
                decisionId = data.optString("decision_id").ifEmpty { null },
                actions = actions,
                image = image,
                raw = jsonToMap(root)
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseActions(decisionType: String, data: JSONObject): List<DecisionAction> {
        return when (decisionType) {
            "approve_deny" -> listOf(
                DecisionAction.Approve,
                DecisionAction.Deny
            )
            "single_select", "multi_select" -> {
                val opts = data.optJSONArray("options") ?: return emptyList()
                buildList(opts.length()) {
                    for (i in 0 until opts.length()) {
                        val o = opts.getJSONObject(i)
                        val value = o.optString("value").ifEmpty { o.optString("label") }
                        val label = o.optString("label")
                        add(DecisionAction.Pick(value, label))
                    }
                }
            }
            "free_text" -> listOf(DecisionAction.QuickReply)
            else -> emptyList()
        }
    }
    
    private fun jsonToMap(json: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.get(key)
        }
        return map
    }
}
