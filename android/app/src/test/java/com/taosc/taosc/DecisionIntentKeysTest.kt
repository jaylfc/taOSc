package com.taosc.taosc

import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionIntentKeysTest {
    @Test
    fun `same action on different decision ids gives different keys`() {
        val a = intentKey("dec-1", DecisionAction.Approve)
        val b = intentKey("dec-2", DecisionAction.Approve)
        assertEquals("decision/dec-1/approve", a)
        assertEquals("decision/dec-2/approve", b)
    }

    @Test
    fun `approve vs deny on one decision differ`() {
        val approve = intentKey("dec-1", DecisionAction.Approve)
        val deny = intentKey("dec-1", DecisionAction.Deny)
        assertEquals("decision/dec-1/approve", approve)
        assertEquals("decision/dec-1/deny", deny)
    }

    @Test
    fun `pick a vs pick b on one decision differ`() {
        val a = intentKey("dec-1", DecisionAction.Pick("a", "A"))
        val b = intentKey("dec-1", DecisionAction.Pick("b", "B"))
        assertEquals("decision/dec-1/pick/a", a)
        assertEquals("decision/dec-1/pick/b", b)
    }

    @Test
    fun `pick with same value on two decisions differ`() {
        val a = intentKey("dec-1", DecisionAction.Pick("opt-1", "Option 1"))
        val b = intentKey("dec-2", DecisionAction.Pick("opt-1", "Option 1"))
        assertEquals("decision/dec-1/pick/opt-1", a)
        assertEquals("decision/dec-2/pick/opt-1", b)
    }

    @Test
    fun `exact strings for approve and pick cases`() {
        assertEquals("decision/dec-1/approve", intentKey("dec-1", DecisionAction.Approve))
        assertEquals("decision/dec-1/pick/opt-1", intentKey("dec-1", DecisionAction.Pick("opt-1", "Option 1")))
    }
}
