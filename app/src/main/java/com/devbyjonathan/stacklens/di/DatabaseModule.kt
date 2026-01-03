package com.devbyjonathan.stacklens.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devbyjonathan.stacklens.data.local.StackLensDatabase
import com.devbyjonathan.stacklens.data.local.dao.CrashInsightDao
import com.devbyjonathan.stacklens.data.local.dao.CrashLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS crash_insights (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    crashId INTEGER NOT NULL,
                    summary TEXT NOT NULL,
                    rootCause TEXT NOT NULL,
                    suggestedFix TEXT NOT NULL,
                    affectedLine TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (crashId) REFERENCES crash_logs(id) ON DELETE CASCADE
                )
            """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_crash_insights_crashId ON crash_insights(crashId)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StackLensDatabase {
        return Room.databaseBuilder(
            context,
            StackLensDatabase::class.java,
            StackLensDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideCrashLogDao(database: StackLensDatabase): CrashLogDao {
        return database.crashLogDao()
    }

    @Provides
    @Singleton
    fun provideCrashInsightDao(database: StackLensDatabase): CrashInsightDao {
        return database.crashInsightDao()
    }
}