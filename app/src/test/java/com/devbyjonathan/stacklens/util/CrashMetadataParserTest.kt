package com.devbyjonathan.stacklens.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CrashMetadataParserTest {

    @Test
    fun `parses all optional fields from a typical DropBox header`() {
        val content = """
            Process: com.example.app
            PID: 18440
            UID: 10298
            Flags: 0x29c8be44
            Package: com.example.app v1 (1.0)
            Foreground: Yes
            SystemUptimeMs: 408305
            Process-Runtime: 1124

            java.lang.RuntimeException: Just Crash
                at com.example.app.MainActivity.onCreate(MainActivity.kt:29)
        """.trimIndent()

        val meta = parseCrashMetadata(content)

        assertEquals(10298, meta.uid)
        assertEquals(408305L, meta.systemUptimeMs)
        assertEquals(1124L, meta.processRuntimeSec)
        assertEquals(true, meta.foreground)
    }

    @Test
    fun `returns null fields when lines are missing`() {
        val content = """
            Process: com.example.app

            java.lang.RuntimeException: Nothing
        """.trimIndent()

        val meta = parseCrashMetadata(content)

        assertNull(meta.uid)
        assertNull(meta.systemUptimeMs)
        assertNull(meta.processRuntimeSec)
        assertNull(meta.foreground)
    }

    @Test
    fun `parses Foreground No as false`() {
        val meta = parseCrashMetadata("Foreground: No\n")
        assertEquals(false, meta.foreground)
    }
}
