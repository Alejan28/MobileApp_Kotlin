package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.todo.data.Item
import com.example.myapplication.todo.data.local.ItemDao

// 🔑 Version is bumped to 5 to accommodate the new Item structure (lat/lon)
@Database(entities = [Item::class], version = 5)
abstract class MyAppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var INSTANCE: MyAppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE items_new (_id TEXT PRIMARY KEY NOT NULL, text TEXT NOT NULL)")
                database.execSQL("INSERT INTO items_new (_id, text) SELECT _id, text FROM items")
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items RENAME COLUMN text TO title")
                database.execSQL("ALTER TABLE items ADD COLUMN artist TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE items ADD COLUMN noTracks INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE items ADD COLUMN releaseDate TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE items ADD COLUMN date TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE items ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 🔑 MIGRATION 4 -> 5: ADDS LAT AND LON
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE items ADD COLUMN lat REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE items ADD COLUMN lon REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getDatabase(context: Context): MyAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    MyAppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}