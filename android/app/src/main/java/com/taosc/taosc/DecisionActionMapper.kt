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
        
        val bodyJson = JSONObject().apply {
            when (action) {
                is DecisionAction.Approve -> put("value", "approve")
                is DecisionAction.Deny -> put("value", "deny")
                is DecisionAction.Pick -> {
                    when (payload.decisionType) {
                        "multi_select" -> {
                            // For multi_select, accumulate all selected options into array
                            val selectedOptions = payload.actions.filterIsInstance<DecisionAction.Pick>()
                            // If the action being sent is a quick reply text, add it to the array
                            val valueList = selectedOptions.map { it.value }.toMutableList()
                            if (quickReplyText.isNotEmpty()) {
                                valueList.add(quickReplyText)
                            }
                            put("value", valueList)
                        }
                        else -> put("value", action.value)  // single_select and other types
                    }
                }
                is DecisionAction.QuickReply -> put("value", quickReplyText)
                is DecisionAction.AddNote -> put("note", quickReplyText)
            }
        }.toString()
        
        return OutboundCall(
            url = "$baseUrl/api/decisions/$decisionId/answer",
            method = "POST",
            body = bodyJson
        )
    }
}
