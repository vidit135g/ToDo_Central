package com.absolute.todocentral.data.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

@Entity(tableName = "tasks_table")
class Task : Serializable {

    companion object {
        // Pinned to the value computed for the pre-completion class shape.
        // Backups are written with ObjectOutputStream, so without this an added
        // field changes the implicit UID and every existing backup fails to
        // restore with InvalidClassException.
        private const val serialVersionUID: Long = 3231390178055522454L

        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2

        const val REPEAT_NONE = 0
        const val REPEAT_DAILY = 1
        const val REPEAT_WEEKLY = 2
        const val REPEAT_MONTHLY = 3
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @NonNull
    var id: Long = 0

    @ColumnInfo(name = "task_title")
    var title: String = ""

    @ColumnInfo(name = "task_note")
    var note: String = ""

    @ColumnInfo(name = "task_date")
    @NonNull
    var date: Long = 0

    @ColumnInfo(name = "task_position")
    @NonNull
    var position: Int = 0

    @ColumnInfo(name = "task_time_stamp")
    @NonNull
    var timeStamp: Long = 0

    /** When the task was ticked off, or 0 while it is still outstanding. */
    @ColumnInfo(name = "task_completed_at")
    @NonNull
    var completedAt: Long = 0

    /** [PRIORITY_LOW], [PRIORITY_NORMAL] or [PRIORITY_HIGH]. */
    @ColumnInfo(name = "task_priority")
    @NonNull
    var priority: Int = PRIORITY_NORMAL

    /** [REPEAT_NONE], [REPEAT_DAILY], [REPEAT_WEEKLY] or [REPEAT_MONTHLY]. */
    @ColumnInfo(name = "task_repeat")
    @NonNull
    var repeat: Int = REPEAT_NONE

    val isCompleted: Boolean
        get() = completedAt > 0

    val isRecurring: Boolean
        get() = repeat != REPEAT_NONE

    @Ignore
    constructor() {
        this.timeStamp = Date().time
    }

    constructor(id: Long, title: String, note: String, date: Long, position: Int, timeStamp: Long) {
        this.id = id
        this.title = title
        this.note = note
        this.date = date
        this.position = position
        this.timeStamp = timeStamp
    }
}
