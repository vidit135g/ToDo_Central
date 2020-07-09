package com.absolute.todocentral.vm

import android.app.Application
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class TaskListViewModel(app: Application) : BaseViewModel(app) {
    val liveData = repository.getAllTasksLiveData()

    fun updateTaskOrder(tasks: List<Task>) = repository.updateTaskOrder(tasks)

    fun deleteTask(task: Task) = repository.deleteTask(task)

    fun saveTask(task: Task) = repository.saveTask(task)
}