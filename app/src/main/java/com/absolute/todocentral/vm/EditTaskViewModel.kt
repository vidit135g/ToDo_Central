package com.absolute.todocentral.vm

import android.app.Application
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class EditTaskViewModel(app: Application) : BaseViewModel(app) {
    fun updateTask(task: Task) = repository.updateTask(task)
}