package com.devbyjonathan.stacklens.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.devbyjonathan.stacklens.model.CrashLog
import com.devbyjonathan.stacklens.model.CrashType

@Entity(tableName = "crash_logs")
data class CrashLogEntity(
    @PrimaryKey val id: Long,
    val tag: String,
    val packageName: String?,
    val appName: String?,
    val timestamp: Long,
    val content: String,
    val processName: String?,
    val pid: Int?,
) {
    fun toCrashLog(): CrashLog {
        return CrashLog(
            id = id,
            tag = CrashType.fromTag(tag) ?: CrashType.DATA_APP_CRASH,
            packageName = packageName,
            appName = appName,
            timestamp = timestamp,
            content = content,
            processName = processName,
            pid = pid
        )
    }

    companion object {
        fun fromCrashLog(crashLog: CrashLog): CrashLogEntity {
            return CrashLogEntity(
                id = crashLog.id,
                tag = crashLog.tag.tag,
                packageName = crashLog.packageName,
                appName = crashLog.appName,
                timestamp = crashLog.timestamp,
                content = crashLog.content,
                processName = crashLog.processName,
                pid = crashLog.pid
            )
        }
    }
}
