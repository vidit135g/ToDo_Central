package com.absolute.todocentral.vm.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.absolute.todocentral.data.database.TaskListRepository

abstract class BaseViewModel(app: Application) : AndroidViewModel(app) {
    val repository = TaskListRepository(app)
}