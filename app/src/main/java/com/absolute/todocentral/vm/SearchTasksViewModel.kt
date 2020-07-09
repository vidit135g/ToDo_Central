package com.absolute.todocentral.vm

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.vm.base.BaseViewModel

class SearchTasksViewModel(app: Application) : BaseViewModel(app) {
    val searchInputLiveData: MutableLiveData<String> = MutableLiveData()
    val searchResultLiveData: LiveData<List<Task>> = Transformations.switchMap(searchInputLiveData) {
        if (it.isNotEmpty()) {
            repository.getTasksForSearch(it)
        } else {
            MutableLiveData()
        }
    }
}