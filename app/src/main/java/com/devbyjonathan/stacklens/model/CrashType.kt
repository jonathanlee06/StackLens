package com.devbyjonathan.stacklens.model

enum class CrashType(val tag: String, val displayName: String) {
    // App crashes (Java/Kotlin)
    DATA_APP_CRASH("data_app_crash", "App Crash"),
    SYSTEM_APP_CRASH("system_app_crash", "System App Crash"),

    // ANR (Application Not Responding)
    DATA_APP_ANR("data_app_anr", "ANR"),
    SYSTEM_APP_ANR("system_app_anr", "System ANR"),

    // Native crashes (C/C++ tombstones)
    SYSTEM_TOMBSTONE("SYSTEM_TOMBSTONE", "Native Crash"),

    // Watchdog / System crashes
    SYSTEM_SERVER_CRASH("system_server_crash", "System Server Crash"),
    SYSTEM_SERVER_ANR("system_server_anr", "System Server ANR"),
    SYSTEM_SERVER_WTF("system_server_wtf", "System WTF"),

    // Strict mode violations
    DATA_APP_STRICTMODE("data_app_strictmode", "StrictMode Violation"),

    // Kernel panics
    SYSTEM_LAST_KMSG("SYSTEM_LAST_KMSG", "Kernel Log"),
    APANIC_CONSOLE("APANIC_CONSOLE", "Kernel Panic"),
    APANIC_THREADS("APANIC_THREADS", "Panic Threads");

    companion object {
        fun fromTag(tag: String): CrashType? = entries.find { it.tag == tag }

        val appCrashTags = listOf(DATA_APP_CRASH, SYSTEM_APP_CRASH)
        val anrTags = listOf(DATA_APP_ANR, SYSTEM_APP_ANR)
        val nativeTags = listOf(SYSTEM_TOMBSTONE)
        val allTags = entries.toList()
    }
}
