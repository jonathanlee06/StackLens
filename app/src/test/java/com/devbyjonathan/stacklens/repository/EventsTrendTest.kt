package com.devbyjonathan.stacklens.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Simple pure-math tests for the delta computation used by [EventsTrend]. We keep these out of
 * the repository (which depends on Room and live DAOs) by testing the same arithmetic in
 * isolation.
 */
class EventsTrendTest {

    @Test
    fun `delta returns 0 when both current and previous are zero`() {
        assertEquals(0f, computeDelta(0, 0), 0f)
    }

    @Test
    fun `delta returns 100 when previous is zero and current is positive`() {
        assertEquals(100f, computeDelta(5, 0), 0f)
    }

    @Test
    fun `delta returns positive percentage when growing`() {
        assertEquals(50f, computeDelta(15, 10), 0.01f)
    }

    @Test
    fun `delta returns negative percentage when shrinking`() {
        assertEquals(-50f, computeDelta(5, 10), 0.01f)
    }

    private fun computeDelta(current: Int, previous: Int): Float {
        return if (previous == 0) {
            if (current == 0) 0f else 100f
        } else {
            (current - previous) / previous.toFloat() * 100f
        }
    }
}
