package com.aldointelligence.kontekscek.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aldointelligence.kontekscek.data.model.ContextCardEntity
import com.aldointelligence.kontekscek.data.model.VerificationLogEntity

@Database(
    entities = [ContextCardEntity::class, VerificationLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class KonteksDatabase : RoomDatabase() {
    abstract fun konteksDao(): KonteksDao

    companion object {
        @Volatile
        private var INSTANCE: KonteksDatabase? = null

        fun getInstance(context: Context): KonteksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KonteksDatabase::class.java,
                    "kontekscek_local_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}