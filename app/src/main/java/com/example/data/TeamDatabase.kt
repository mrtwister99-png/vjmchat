package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        ActivityEntity::class,
        TaskNoteEntity::class,
        TaskNoteCommentEntity::class,
        IdeaEntity::class,
        ProjectCommentEntity::class,
        ChatMessageEntity::class,
        CalendarEventEntity::class,
        CalendarCommentEntity::class
    ],
    version = 108,
    exportSchema = false
)
abstract class TeamDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao

    companion object {
        @Volatile
        private var INSTANCE: TeamDatabase? = null

        private val MIGRATION_107_108 = object : Migration(107, 108) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN lastActivityReadTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): TeamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TeamDatabase::class.java,
                    "team_firemni_database"
                )
                    .addMigrations(MIGRATION_107_108)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
