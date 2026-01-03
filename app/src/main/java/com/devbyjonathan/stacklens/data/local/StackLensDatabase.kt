package com.devbyjonathan.stacklens.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devbyjonathan.stacklens.data.local.dao.CrashLogDao
import com.devbyjonathan.stacklens.data.local.entity.CrashLogEntity

@Database(
    entities = [CrashLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StackLensDatabase : RoomDatabase() {

    abstract fun crashLogDao(): CrashLogDao

    companion object {
        const val DATABASE_NAME = "stacklens_db"
    }
}
