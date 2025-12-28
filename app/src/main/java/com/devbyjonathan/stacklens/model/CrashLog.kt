package com.devbyjonathan.stacklens.model

data class CrashLog(
    val id: Long,
    val tag: CrashType,
    val packageName: String?,
    val appName: String?,
    val timestamp: Long,
    val content: String,
    val processName: String? = null,
    val pid: Int? = null
)
