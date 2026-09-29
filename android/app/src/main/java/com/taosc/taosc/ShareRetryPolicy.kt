package com.taosc.taosc

sealed class RetryDecision(val reason: String = "") {
    object Done : RetryDecision()
    object RetryLater : RetryDecision()
    data class GiveUp(val reason: String) : RetryDecision()
}

fun decide(result: SendResult, attempt: Int): RetryDecision {
    return when (result) {
        is SendResult.Sent -> RetryDecision.Done
        is SendResult.NotPaired -> RetryDecision.GiveUp("NotPaired")
        is SendResult.Rejected -> RetryDecision.GiveUp("Rejected")
        is SendResult.Unreachable -> if (attempt < 8) RetryDecision.RetryLater else RetryDecision.GiveUp("Max attempts reached")
    }
}