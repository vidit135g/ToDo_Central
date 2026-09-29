package com.absolute.todocentral.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.absolute.todocentral.data.models.Subtask

@Dao
interface SubtaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(subtask: Subtask): Long

    @Update
    fun update(subtask: Subtask)

    @Delete
    fun delete(subtask: Subtask)

    @Query("SELECT * FROM subtasks_table WHERE parent_task_id = :taskId ORDER BY subtask_position")
    fun forTaskLiveData(taskId: Long): LiveData<List<Subtask>>

    @Query("SELECT * FROM subtasks_table WHERE parent_task_id = :taskId ORDER BY subtask_position")
    fun forTask(taskId: Long): List<Subtask>

    @Query("SELECT * FROM subtasks_table ORDER BY parent_task_id, subtask_position")
    fun all(): List<Subtask>

    @Query("SELECT COUNT(*) FROM subtasks_table WHERE parent_task_id = :taskId")
    fun countFor(taskId: Long): Int

    @Query("SELECT COUNT(*) FROM subtasks_table WHERE parent_task_id = :taskId AND subtask_completed_at > 0")
    fun countDoneFor(taskId: Long): Int

    @Query("DELETE FROM subtasks_table WHERE parent_task_id = :taskId")
    fun deleteForTask(taskId: Long)
}
