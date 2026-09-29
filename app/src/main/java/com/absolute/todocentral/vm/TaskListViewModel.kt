package com.absolute.todocentral.vm

import android.app.Application
import com.absolute.todocentral.data.TaskStats
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class TaskListViewModel(app: Application) : BaseViewModel(app) {
    val liveData = repository.getAllTasksLiveData()

    fun updateTaskOrder(tasks: List<Task>) = repository.updateTaskOrder(tasks)

    fun deleteTask(task: Task) = repository.deleteTask(task)

    fun saveTask(task: Task) = repository.saveTask(task)

    fun updateTask(task: Task) = repository.updateTask(task)

    /** Runs aggregate queries; call off the main thread. */
    fun statsNow(): TaskStats = repository.statsNow()

    /** taskId -> (done, total) for every task that has subtasks. */
    fun subtaskCounts(): Map<Long, Pair<Int, Int>> {
        val counts = HashMap<Long, Pair<Int, Int>>()
        repository.allSubtasks().groupBy { it.parentTaskId }.forEach { (taskId, items) ->
            counts[taskId] = Pair(items.count { it.isCompleted }, items.size)
        }
        return counts
    }
}