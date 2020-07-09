package com.absolute.todocentral.vm

import android.app.Application
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class AddTaskViewModel(app: Application) : BaseViewModel(app) {
    fun saveTask(task: Task) = repository.saveTask(task)
}