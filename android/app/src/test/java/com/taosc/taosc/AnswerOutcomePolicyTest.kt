package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerOutcomePolicyTest {
    @Test
    fun `200 response returns DISMISS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(200, ""), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.DISMISS, outcome)
    }

    @Test
    fun `204 response returns DISMISS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(204, ""), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.DISMISS, outcome)
    }

    @Test
    fun `409 already-answered body returns DISMISS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(409, "already answered or not pending"), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.DISMISS, outcome)
    }

    @Test
    fun `409 gate body returns FAILED_USE_TAOS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(409, "gate decisions cannot be answered by a device"), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.FAILED_USE_TAOS, outcome)
    }

    @Test
    fun `404 response returns FAILED_USE_TAOS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(404, "not found"), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.FAILED_USE_TAOS, outcome)
    }

    @Test
    fun `400 response returns FAILED_USE_TAOS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(400, "bad value"), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.FAILED_USE_TAOS, outcome)
    }

    @Test
    fun `503 response returns FAILED_RETRY`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(503, ""), threw = false, missingCredentials = false)
        assertEquals(AnswerOutcome.FAILED_RETRY, outcome)
    }

    @Test
    fun `threw true returns FAILED_RETRY`() {
        val outcome = AnswerOutcomePolicy.classify(null, threw = true, missingCredentials = false)
        assertEquals(AnswerOutcome.FAILED_RETRY, outcome)
    }

    @Test
    fun `missingCredentials true with 200 response returns FAILED_USE_TAOS`() {
        val outcome = AnswerOutcomePolicy.classify(HttpResponse(200, ""), threw = false, missingCredentials = true)
        assertEquals(AnswerOutcome.FAILED_USE_TAOS, outcome)
    }
}
