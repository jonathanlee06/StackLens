package com.devbyjonathan.stacklens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devbyjonathan.stacklens.ai.CrashInsight
import com.devbyjonathan.stacklens.ai.Severity

@Entity(
    tableName = "crash_insights",
    foreignKeys = [
        ForeignKey(
            entity = CrashLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["crashId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["crashId"], unique = true)]
)
data class CrashInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val crashId: Long,
    val title: String = "",
    val summary: String,
    val rootCause: String,
    val suggestedFix: String,
    val affectedLine: String?,
    val severity: String = "MEDIUM",
    val confidence: Float = 0.7f,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toCrashInsight(): CrashInsight {
        val resolvedTitle = title.ifBlank { summary.substringBefore('.').take(60) }
        return CrashInsight(
            title = resolvedTitle,
            summary = summary,
            rootCause = rootCause,
            suggestedFix = suggestedFix,
            affectedLine = affectedLine,
            severity = runCatching { Severity.valueOf(severity.uppercase()) }.getOrDefault(Severity.MEDIUM),
            confidence = confidence,
        )
    }

    companion object {
        fun fromCrashInsight(crashId: Long, insight: CrashInsight): CrashInsightEntity {
            return CrashInsightEntity(
                crashId = crashId,
                title = insight.title,
                summary = insight.summary,
                rootCause = insight.rootCause,
                suggestedFix = insight.suggestedFix,
                affectedLine = insight.affectedLine,
                severity = insight.severity.name,
                confidence = insight.confidence,
            )
        }
    }
}
