package com.devbyjonathan.stacklens.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devbyjonathan.stacklens.ai.CrashInsight

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
    val summary: String,
    val rootCause: String,
    val suggestedFix: String,
    val affectedLine: String?,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toCrashInsight(): CrashInsight {
        return CrashInsight(
            summary = summary,
            rootCause = rootCause,
            suggestedFix = suggestedFix,
            affectedLine = affectedLine
        )
    }

    companion object {
        fun fromCrashInsight(crashId: Long, insight: CrashInsight): CrashInsightEntity {
            return CrashInsightEntity(
                crashId = crashId,
                summary = insight.summary,
                rootCause = insight.rootCause,
                suggestedFix = insight.suggestedFix,
                affectedLine = insight.affectedLine
            )
        }
    }
}