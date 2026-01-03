package com.devbyjonathan.stacklens.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devbyjonathan.stacklens.data.local.entity.CrashLogEntity

@Dao
interface CrashLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crashes: List<CrashLogEntity>)

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC")
    suspend fun getAllCrashes(): List<CrashLogEntity>

    @Query("SELECT * FROM crash_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getCrashesSince(sinceTimestamp: Long): List<CrashLogEntity>

    @Query("SELECT * FROM crash_logs WHERE tag IN (:tags) AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getCrashesByTagsSince(
        tags: List<String>,
        sinceTimestamp: Long,
    ): List<CrashLogEntity>

    @Query("DELETE FROM crash_logs WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOlderThan(olderThanTimestamp: Long): Int

    @Query("SELECT MAX(timestamp) FROM crash_logs")
    suspend fun getLatestTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM crash_logs")
    suspend fun getCount(): Int

    @Query("DELETE FROM crash_logs")
    suspend fun deleteAll()
}
