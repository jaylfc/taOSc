package com.taosc.taosc

import org.json.JSONObject

data class OutboundCall(
    val url: String,
    val method: String,
    val body: String
)

object DecisionActionMapper {
    fun toOutboundCall(
        action: DecisionAction,
        payload: DecisionPayload,
        baseUrl: String,
        deviceId: String,
        scopedToken: String,
        quickReplyText: String = ""
    ): OutboundCall? {
        val decisionId = payload.decisionId ?: return null
        
        when (action) {
            is DecisionAction.AddNote -> return null
            is DecisionAction.Approve -> put("value", "approve")
            is DecisionAction.Deny -> put("value", "deny")
            is DecisionAction.Pick -> {
                when (payload.decisionType) {
                    "multi_select" -> {
                        put("value", JSONArray().put(action.value))
                    }
                    else -> put("value", action.value)
                }
            }
            is DecisionAction.QuickReply -> put("value", quickReplyText)
        }
    }
}
