package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecisionPayloadParserTest {
    @Test
    fun `parse approve_deny payload returns correct actions`() {
        val json = """{"title":"Decision","body":"Approve this?","data":{"decision_type":"approve_deny","decision_id":"dec-1"}}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals("Decision", payload?.title)
        assertEquals("Approve this?", payload?.body)
        assertEquals("approve_deny", payload?.decisionType)
        assertEquals("dec-1", payload?.decisionId)
        assertEquals(2, payload?.actions?.size)
        assertEquals(DecisionAction.Approve, payload?.actions?.get(0))
        assertEquals(DecisionAction.Deny, payload?.actions?.get(1))
    }
    
    @Test
    fun `parse payload with explicit actions list`() {
        val json = """{"title":"Decision with custom actions","body":"Custom","data":{"decision_type":"approve_deny","decision_id":"dec-1"}, "actions":[{"id":"custom_approve","label":"Custom Approve"},{"id":"custom_deny","label":"Custom Deny"}]}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals("Decision with custom actions", payload?.title)
        assertEquals(2, payload?.actions?.size)
        // Should contain the custom actions from the root-level actions list, not the derived approve_deny actions
        assertEquals(DecisionAction.Pick("custom_approve", "Custom Approve"), payload?.actions?.get(0))
        assertEquals(DecisionAction.Pick("custom_deny", "Custom Deny"), payload?.actions?.get(1))
    }
    
    @Test
    fun `parse payload with image at root level`() {
        val json = """{"title":"Decision","body":"Test","data":{"decision_type":"approve_deny","decision_id":"dec-1"}, "image":"https://example.com/img.png"}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals("https://example.com/img.png", payload?.image)
    }
    
    @Test
    fun `parse single_select payload returns pick actions`() {
        val json = """{"title":"Pick one","body":"Choose","data":{"decision_type":"single_select","decision_id":"dec-2","options":[{"label":"A","value":"a"},{"label":"B","value":"b"}]}}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals(2, payload?.actions?.size)
        assertEquals(DecisionAction.Pick("a", "A"), payload?.actions?.get(0))
        assertEquals(DecisionAction.Pick("b", "B"), payload?.actions?.get(1))
    }
    
    @Test
    fun `parse multi_select payload returns pick actions`() {
        val json = """{"title":"Pick many","body":"Choose","data":{"decision_type":"multi_select","decision_id":"dec-3","options":[{"label":"X","value":"x"}]}}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals(1, payload?.actions?.size)
        assertEquals(DecisionAction.Pick("x", "X"), payload?.actions?.get(0))
    }
    
    @Test
    fun `parse free_text payload returns quick reply action`() {
        val json = """{"title":"Reply","body":"Type something","data":{"decision_type":"free_text","decision_id":"dec-4"}}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertEquals(1, payload?.actions?.size)
        assertEquals(DecisionAction.QuickReply, payload?.actions?.get(0))
    }
    
    @Test
    fun `parse non-decision payload returns null`() {
        val json = """{"title":"Hello","body":"World"}"""
        val payload = DecisionPayloadParser.parse(json)
        
        assertNull(payload)
    }
    
    @Test
    fun `parse malformed json returns null`() {
        val payload = DecisionPayloadParser.parse("{not valid json")
        assertNull(payload)
    }
}
