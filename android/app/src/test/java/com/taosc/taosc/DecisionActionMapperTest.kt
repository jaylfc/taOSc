package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionActionMapperTest {
    private val baseUrl = "https://taos.example.com"
    private val deviceId = "device-123"
    private val scopedToken = "token-abc"
    private val payload = DecisionPayload(
        title = "Decision",
        body = "Please decide",
        decisionType = "approve_deny",
        decisionId = "dec-1",
        actions = listOf(DecisionAction.Approve, DecisionAction.Deny),
        image = null,
        raw = emptyMap()
    )
    
    @Test
    fun `approve action maps to correct outbound call`() {
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.Approve,
            payload = payload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        assertEquals("{\"value\":\"approve\"}", call?.body)
    }
    
    @Test
    fun `deny action maps to correct outbound call`() {
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.Deny,
            payload = payload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        assertEquals("{\"value\":\"deny\"}", call?.body)
    }
    
    @Test
    fun `pick action maps to correct outbound call`() {
        val pickPayload = payload.copy(decisionType = "single_select")
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.Pick("opt-1", "Option 1"),
            payload = pickPayload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        assertEquals("{\"value\":\"opt-1\"}", call?.body)
    }
    
    @Test
    fun `quick reply action maps to correct outbound call`() {
        val freeTextPayload = payload.copy(decisionType = "free_text")
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.QuickReply,
            payload = freeTextPayload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken,
            quickReplyText = "hello"
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        assertEquals("{\"value\":\"hello\"}", call?.body)
    }
    
    @Test
    fun `add note action maps to correct outbound call`() {
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.AddNote,
            payload = payload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken,
            quickReplyText = "a note"
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        val bodyJson = org.json.JSONObject(call?.body)
        assertEquals("a note", bodyJson.getString("value"))
        assertEquals("a note", bodyJson.getString("note"))
    }
    
    @Test
    fun `multi-select pick action maps to JSON array with tapped value`() {
        val multiSelectPayload = DecisionPayload(
            title = "Decision",
            body = "Please decide",
            decisionType = "multi_select",
            decisionId = "dec-1",
            actions = emptyList(),
            image = null,
            raw = emptyMap()
        )
        
        val call = DecisionActionMapper.toOutboundCall(
            action = DecisionAction.Pick("opt-1", "Option 1"),
            payload = multiSelectPayload,
            baseUrl = baseUrl,
            deviceId = deviceId,
            scopedToken = scopedToken
        )
        
        assertEquals("POST", call?.method)
        assertEquals("$baseUrl/api/decisions/dec-1/answer", call?.url)
        val bodyJson = org.json.JSONObject(call?.body)
        assertTrue(bodyJson.has("value"))
        assertTrue(bodyJson.get("value") is org.json.JSONArray)
        val valueArray = bodyJson.getJSONArray("value")
        assertEquals(1, valueArray.length())
        assertEquals("opt-1", valueArray.getString(0))
    }
}
