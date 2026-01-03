package com.devbyjonathan.stacklens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbyjonathan.stacklens.data.local.entity.CrashInsightEntity

@Dao
interface CrashInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: CrashInsightEntity)

    @Query("SELECT * FROM crash_insights WHERE crashId = :crashId LIMIT 1")
    suspend fun getInsightForCrash(crashId: Long): CrashInsightEntity?

    @Query("DELETE FROM crash_insights WHERE crashId = :crashId")
    suspend fun deleteForCrash(crashId: Long)

    @Query("DELETE FROM crash_insights WHERE crashId NOT IN (SELECT id FROM crash_logs)")
    suspend fun deleteOrphanedInsights(): Int

    @Query("DELETE FROM crash_insights")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM crash_insights")
    suspend fun getCount(): Int
}