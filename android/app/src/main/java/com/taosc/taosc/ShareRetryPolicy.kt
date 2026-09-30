package com.taosc.taosc

sealed class RetryDecision {
    object Done : RetryDecision()
    object RetryLater : RetryDecision()
    data class GiveUp(val reason: String) : RetryDecision()
}

fun decide(result: SendResult, attempt: Int): RetryDecision {
    return when (result) {
        is SendResult.Sent -> RetryDecision.Done
        is SendResult.NotPaired -> RetryDecision.GiveUp("Pair this phone again")
        is SendResult.Rejected -> RetryDecision.GiveUp("taOS refused this destination")
        is SendResult.Unreachable -> {
            if (attempt < 8) {
                RetryDecision.RetryLater
            } else {
                RetryDecision.GiveUp("Could not reach taOS")
            }
        }
    }
}