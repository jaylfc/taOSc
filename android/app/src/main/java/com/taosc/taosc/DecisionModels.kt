package com.taosc.taosc

sealed class DecisionAction {
    data object Approve : DecisionAction()
    data object Deny : DecisionAction()
    data class Pick(val value: String, val label: String) : DecisionAction()
    data object QuickReply : DecisionAction()
    data object AddNote : DecisionAction()
}

data class DecisionPayload(
    val title: String,
    val body: String,
    val decisionType: String,
    val decisionId: String?,
    val actions: List<DecisionAction>,
    val image: String?,
    val raw: Map<String, Any>
) {
    companion object {
        val EMPTY = DecisionPayload("", "", "", null, emptyList(), null, emptyMap())
    }
}
