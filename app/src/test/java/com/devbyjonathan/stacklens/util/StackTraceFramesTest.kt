package com.devbyjonathan.stacklens.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StackTraceFramesTest {

    private val sampleWithAppFrames = """
        FATAL EXCEPTION: main
        java.lang.RuntimeException: Just Crash
            at com.example.app.MainActivity.onCreate(MainActivity.kt:29)
            at com.example.app.MainActivity.access${'$'}onCreate(MainActivity.kt:10)
            at android.app.Activity.performCreate(Activity.java:8305)
            at androidx.compose.foundation.ClickablePointerInputNode.invoke(Clickable.kt:987)
            at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:947)
            at java.lang.reflect.Method.invoke(Native Method)
    """.trimIndent()

    private val frameworkOnly = """
        FATAL EXCEPTION: main
            at android.app.Activity.performCreate(Activity.java:8305)
            at androidx.compose.foundation.ClickablePointerInputNode.invoke(Clickable.kt:987)
            at java.lang.reflect.Method.invoke(Native Method)
    """.trimIndent()

    @Test
    fun `isFrameworkFrame identifies android framework frames`() {
        assertTrue(isFrameworkFrame("android.app.Activity"))
        assertTrue(isFrameworkFrame("androidx.compose.foundation.Clickable"))
        assertTrue(isFrameworkFrame("java.lang.reflect.Method"))
        assertTrue(isFrameworkFrame("dalvik.system.VMStack"))
        assertTrue(isFrameworkFrame("com.android.internal.os.ZygoteInit"))
        assertTrue(isFrameworkFrame("kotlin.collections.ArraysKt"))
        assertTrue(isFrameworkFrame("kotlinx.coroutines.DispatchedTask"))
    }

    @Test
    fun `isFrameworkFrame does not match app classes`() {
        assertFalse(isFrameworkFrame("com.example.app.MainActivity"))
        assertFalse(isFrameworkFrame("com.devbyjonathan.stacklens.ai.CrashInsight"))
    }

    @Test
    fun `extractLikelyLocation returns first app-owned file colon line`() {
        val location = extractLikelyLocation(sampleWithAppFrames)
        assertEquals("MainActivity.kt:29", location)
    }

    @Test
    fun `extractLikelyLocation returns null when only framework frames exist`() {
        assertNull(extractLikelyLocation(frameworkOnly))
    }

    @Test
    fun `extractLikelyLocation returns null for empty content`() {
        assertNull(extractLikelyLocation(""))
    }

    @Test
    fun `countAppFrames counts only non-framework frames`() {
        val count = countAppFrames(sampleWithAppFrames)
        assertEquals(2, count)
    }

    @Test
    fun `countAppFrames returns zero for framework-only trace`() {
        assertEquals(0, countAppFrames(frameworkOnly))
    }
}
