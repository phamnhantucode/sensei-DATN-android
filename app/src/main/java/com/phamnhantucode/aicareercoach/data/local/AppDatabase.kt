package com.phamnhantucode.aicareercoach.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuestionPoolEntity::class,
        UserProfileCacheEntity::class,
        AssessmentCacheEntity::class,
        TipsCacheEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionPoolDao(): QuestionPoolDao
    abstract fun userProfileCacheDao(): UserProfileCacheDao
    abstract fun assessmentCacheDao(): AssessmentCacheDao
    abstract fun tipsCacheDao(): TipsCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_career_coach_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
