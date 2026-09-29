package com.absolute.todocentral.data.models

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * One step inside a task. Deleted with its parent, so a task's checklist can
 * never outlive the task it belongs to.
 */
@Entity(
        tableName = "subtasks_table",
        foreignKeys = [ForeignKey(
                entity = Task::class,
                parentColumns = ["_id"],
                childColumns = ["parent_task_id"],
                onDelete = ForeignKey.CASCADE)],
        indices = [Index("parent_task_id")]
)
class Subtask : Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    @NonNull
    var id: Long = 0

    @ColumnInfo(name = "parent_task_id")
    @NonNull
    var parentTaskId: Long = 0

    @ColumnInfo(name = "subtask_title")
    var title: String = ""

    @ColumnInfo(name = "subtask_position")
    @NonNull
    var position: Int = 0

    @ColumnInfo(name = "subtask_completed_at")
    @NonNull
    var completedAt: Long = 0

    val isCompleted: Boolean
        get() = completedAt > 0

    constructor()

    constructor(parentTaskId: Long, title: String, position: Int) {
        this.parentTaskId = parentTaskId
        this.title = title
        this.position = position
    }
}
