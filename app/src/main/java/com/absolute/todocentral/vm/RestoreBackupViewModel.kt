package com.absolute.todocentral.vm

import android.app.Application
import android.net.Uri
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.service.alarm.AlarmHelper
import com.absolute.todocentral.vm.base.BaseViewModel
import java.io.ObjectInputStream
import java.util.*

class RestoreBackupViewModel(val app: Application) : BaseViewModel(app) {
    private var isRestoredSuccessfully = false

    fun isBackupRestoredSuccessfully() = isRestoredSuccessfully

    fun restoreBackup(uri: Uri) {
        val restoredTasks: List<Task>
        val alarmHelper = AlarmHelper.getInstance()

        isRestoredSuccessfully = try {
            val fileInputStream = app.contentResolver.openInputStream(uri)
            val objectInputStream = ObjectInputStream(fileInputStream)

            @Suppress("UNCHECKED_CAST")
            restoredTasks = objectInputStream.readObject() as List<Task>

            repository.deleteAllTasks()
            for (task in restoredTasks) {
                repository.saveTask(task)

                if (task.date != 0L && task.date > Calendar.getInstance().timeInMillis) {
                    alarmHelper.setAlarm(task)
                }
            }
            objectInputStream.close()
            true
        } catch (exception: Exception) {
            exception.printStackTrace()
            false
        }
    }
}
