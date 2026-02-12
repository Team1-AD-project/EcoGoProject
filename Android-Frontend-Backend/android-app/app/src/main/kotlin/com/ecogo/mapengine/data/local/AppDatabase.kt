package com.ecogo.mapengine.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ecogo.mapengine.data.local.dao.NavigationHistoryDao
import com.ecogo.mapengine.data.local.entity.NavigationHistory

/**
 * Application database
 * Singleton pattern, provides a globally unique database instance
 */
@Database(
    entities = [NavigationHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get navigation history DAO
     */
    abstract fun navigationHistoryDao(): NavigationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance (singleton)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ecogo_database"
                )
                    .fallbackToDestructiveMigration() // Acceptable for development; production requires a migration strategy
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing: clear database instance
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
