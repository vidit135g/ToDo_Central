package com.absolute.todocentral.vm

import android.app.Application
import android.net.Uri
import com.absolute.todocentral.vm.base.BaseViewModel
import java.io.IOException
import java.io.ObjectOutputStream

class CreateBackupViewModel(val app: Application) : BaseViewModel(app) {
    private var isCreatedSuccessfully = false

    fun isBackupCreatedSuccessfully() = isCreatedSuccessfully

    fun createBackup(uri: Uri) {
        val tasks = repository.getAllTasks()
        isCreatedSuccessfully = try {
            val fileOutputStream = app.contentResolver.openOutputStream(uri)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)

            objectOutputStream.writeObject(tasks)
            objectOutputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
