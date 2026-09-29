package com.absolute.todocentral.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.absolute.todocentral.data.models.Task

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTask(task: Task)

    @Update
    fun updateTask(task: Task)

    @Update
    fun updateTaskOrder(tasks: List<Task>)

    @Delete
    fun deleteTask(task: Task)

    @Query("DELETE FROM tasks_table")
    fun deleteAllTasks()

    @Query("SELECT * FROM tasks_table ORDER BY task_position")
    fun getAllTasksLiveData(): LiveData<List<Task>>

    @Query("SELECT * FROM tasks_table ORDER BY task_position")
    fun getAllTasks(): List<Task>

    @Query("SELECT * FROM tasks_table WHERE task_title LIKE '%' || :searchText || '%'")
    fun getTasksForSearch(searchText: String): LiveData<List<Task>>

    // --- Bento home ---------------------------------------------------------
    // Sections and counters are resolved in SQL rather than filtered in the
    // adapter, so the tiles can never disagree with the list under them.

    /** Outstanding, dated before [dayStart]. */
    @Query("SELECT COUNT(*) FROM tasks_table WHERE task_completed_at = 0 " +
            "AND task_date > 0 AND task_date < :dayStart")
    fun countOverdue(dayStart: Long): Int

    /** Outstanding and due inside the given day. */
    @Query("SELECT COUNT(*) FROM tasks_table WHERE task_completed_at = 0 " +
            "AND task_date >= :dayStart AND task_date < :dayEnd")
    fun countDueToday(dayStart: Long, dayEnd: Long): Int

    /** Completed inside the given day. */
    @Query("SELECT COUNT(*) FROM tasks_table WHERE task_completed_at >= :dayStart " +
            "AND task_completed_at < :dayEnd")
    fun countCompletedBetween(dayStart: Long, dayEnd: Long): Int

    @Query("SELECT COUNT(*) FROM tasks_table WHERE task_completed_at = 0")
    fun countOutstanding(): Int

    /** Every completion timestamp, newest first; drives streaks and the week chart. */
    @Query("SELECT task_completed_at FROM tasks_table WHERE task_completed_at > 0 " +
            "ORDER BY task_completed_at DESC")
    fun completionTimes(): List<Long>

    /** The next thing worth doing: soonest due, then highest priority. */
    @Query("SELECT * FROM tasks_table WHERE task_completed_at = 0 " +
            "ORDER BY (CASE WHEN task_date = 0 THEN 1 ELSE 0 END), task_date, " +
            "task_priority DESC, task_position LIMIT 1")
    fun nextTask(): Task?
}