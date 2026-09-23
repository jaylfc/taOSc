package com.taosc.taosc

fun intentKey(decisionId: String, action: DecisionAction): String {
    val slug = when (action) {
        is DecisionAction.Approve -> "approve"
        is DecisionAction.Deny -> "deny"
        is DecisionAction.QuickReply -> "quick_reply"
        is DecisionAction.AddNote -> "add_note"
        is DecisionAction.Pick -> "pick/${action.value}"
    }
    return "decision/$decisionId/$slug"
}
