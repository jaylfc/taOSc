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
        quickReplyText: String = ""
    ): OutboundCall? {
        val decisionId = payload.decisionId ?: return null
        
        val bodyJson = JSONObject().apply {
            when (action) {
                is DecisionAction.Approve -> put("value", "approve")
                is DecisionAction.Deny -> put("value", "deny")
                is DecisionAction.Pick -> {
                    when (payload.decisionType) {
                        "multi_select" -> {
                            put("value", listOf(action.value))
                        }
                        else -> put("value", action.value)
                    }
                }
                is DecisionAction.QuickReply -> put("value", quickReplyText)
                is DecisionAction.AddNote -> {
                    put("value", quickReplyText)
                    put("note", quickReplyText)
                }
            }
        }.toString()
        
        return OutboundCall(
            url = "$baseUrl/api/decisions/$decisionId/answer",
            method = "POST",
            body = bodyJson
        )
    }
}
