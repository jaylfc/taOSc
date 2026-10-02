package com.taosc.taosc

enum class AnswerOutcome { DISMISS, FAILED_RETRY, FAILED_USE_TAOS }

object AnswerOutcomePolicy {
    fun classify(response: HttpResponse?, threw: Boolean, missingCredentials: Boolean): AnswerOutcome {
        if (missingCredentials) return AnswerOutcome.FAILED_USE_TAOS
        if (threw || response == null) return AnswerOutcome.FAILED_RETRY
        val code = response.code
        return when {
            code in 200..299 -> AnswerOutcome.DISMISS
            code == 409 && response.body.contains("already answered or not pending") -> AnswerOutcome.DISMISS
            code == 404 -> AnswerOutcome.FAILED_USE_TAOS
            code == 409 -> AnswerOutcome.FAILED_USE_TAOS
            code in 500..599 -> AnswerOutcome.FAILED_RETRY
            else -> AnswerOutcome.FAILED_USE_TAOS
        }
    }
}
