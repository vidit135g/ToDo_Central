package com.absolute.todocentral.vm

import android.app.Application
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class DeleteTaskViewModel(app: Application) : BaseViewModel(app) {
    fun deleteTask(task: Task) = repository.deleteTask(task)
}