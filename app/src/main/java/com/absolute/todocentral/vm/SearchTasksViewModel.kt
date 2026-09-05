package com.absolute.todocentral.vm

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class SearchTasksViewModel(app: Application) : BaseViewModel(app) {
    val searchInputLiveData: MutableLiveData<String> = MutableLiveData()
    val searchResultLiveData: LiveData<List<Task>> = searchInputLiveData.switchMap { query ->
        if (query.isNotEmpty()) {
            repository.getTasksForSearch(query)
        } else {
            MutableLiveData()
        }
    }
}
