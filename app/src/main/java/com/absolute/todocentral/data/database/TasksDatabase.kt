package com.absolute.todocentral.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.absolute.todocentral.data.models.Subtask
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.utils.PreferenceHelper

@Database(entities = [Task::class, Subtask::class], version = 5)
abstract class TasksDatabase : RoomDatabase() {

    abstract fun taskDAO(): TaskDao

    abstract fun subtaskDAO(): SubtaskDao

    companion object {
        private var mInstance: TasksDatabase? = null

        private const val DATABASE_NAME = "simpletodo_database"

        private const val TASKS_TABLE = "tasks_table"
        private const val TEMP_TABLE = "temp_table"

        private const val TASK_ID_COLUMN = "_id"
        private const val TASK_TITLE_COLUMN = "task_title"
        private const val TASK_NOTE_COLUMN = "task_note"
        private const val TASK_DATE_COLUMN = "task_date"
        private const val TASK_POSITION_COLUMN = "task_position"
        private const val TASK_TIME_STAMP_COLUMN = "task_time_stamp"
        private const val TASK_COMPLETED_AT_COLUMN = "task_completed_at"
        private const val TASK_PRIORITY_COLUMN = "task_priority"
        private const val TASK_REPEAT_COLUMN = "task_repeat"
        private const val SUBTASKS_TABLE = "subtasks_table"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE $TEMP_TABLE ($TASK_ID_COLUMN INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, $TASK_TITLE_COLUMN TEXT NOT NULL, " +
                        "$TASK_DATE_COLUMN INTEGER NOT NULL, $TASK_POSITION_COLUMN INTEGER NOT NULL, $TASK_TIME_STAMP_COLUMN INTEGER NOT NULL);")
                database.execSQL("INSERT INTO $TEMP_TABLE ($TASK_ID_COLUMN, $TASK_TITLE_COLUMN, $TASK_DATE_COLUMN, $TASK_POSITION_COLUMN, $TASK_TIME_STAMP_COLUMN) " +
                        "SELECT $TASK_ID_COLUMN, $TASK_TITLE_COLUMN, $TASK_DATE_COLUMN, $TASK_POSITION_COLUMN, $TASK_TIME_STAMP_COLUMN FROM $TASKS_TABLE")
                database.execSQL("DROP TABLE $TASKS_TABLE")
                database.execSQL("ALTER TABLE $TEMP_TABLE RENAME TO $TASKS_TABLE;")

                val preferenceHelper = PreferenceHelper.getInstance()
                if (!preferenceHelper.getBoolean(PreferenceHelper.IS_AFTER_DATABASE_MIGRATION)) {
                    preferenceHelper.putBoolean(PreferenceHelper.IS_AFTER_DATABASE_MIGRATION, true)
                }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE $TASKS_TABLE ADD COLUMN $TASK_NOTE_COLUMN TEXT DEFAULT '' NOT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE $TASKS_TABLE ADD COLUMN $TASK_COMPLETED_AT_COLUMN INTEGER DEFAULT 0 NOT NULL")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE $TASKS_TABLE ADD COLUMN $TASK_PRIORITY_COLUMN INTEGER DEFAULT 1 NOT NULL")
                database.execSQL("ALTER TABLE $TASKS_TABLE ADD COLUMN $TASK_REPEAT_COLUMN INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("CREATE TABLE IF NOT EXISTS $SUBTASKS_TABLE (" +
                        "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "parent_task_id INTEGER NOT NULL, " +
                        "subtask_title TEXT NOT NULL, " +
                        "subtask_position INTEGER NOT NULL, " +
                        "subtask_completed_at INTEGER NOT NULL, " +
                        "FOREIGN KEY(parent_task_id) REFERENCES $TASKS_TABLE(_id) ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_${SUBTASKS_TABLE}_parent_task_id " +
                        "ON $SUBTASKS_TABLE (parent_task_id)")
            }
        }

        fun getInstance(context: Context): TasksDatabase {
            synchronized(TasksDatabase::class.java) {
                if (mInstance == null) {
                    mInstance = Room.databaseBuilder(context,
                            TasksDatabase::class.java, DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                            .build()
                }
            }
            return mInstance!!
        }
    }
}