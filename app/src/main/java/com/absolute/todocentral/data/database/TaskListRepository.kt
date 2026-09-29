package com.absolute.todocentral.data.database

import android.app.Application
import com.absolute.todocentral.data.TaskStats
import com.absolute.todocentral.data.models.Subtask
import com.absolute.todocentral.data.models.Task
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

class TaskListRepository(app: Application) {
    private val mTaskDao = TasksDatabase.getInstance(app).taskDAO()
    private val mSubtaskDao = TasksDatabase.getInstance(app).subtaskDAO()
    private val mAllTasksLiveData = mTaskDao.getAllTasksLiveData()

    fun getAllTasksLiveData() = mAllTasksLiveData

    fun deleteAllTasks() = Completable.fromCallable { mTaskDao.deleteAllTasks() }.subscribeOn(Schedulers.io()).subscribe()!!

    fun saveTask(task: Task) = Completable.fromCallable { mTaskDao.saveTask(task) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun deleteTask(task: Task) = Completable.fromCallable { mTaskDao.deleteTask(task) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun updateTask(task: Task) = Completable.fromCallable { mTaskDao.updateTask(task) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun updateTaskOrder(tasks: List<Task>) = Completable.fromCallable { mTaskDao.updateTaskOrder(tasks) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun getTasksForSearch(searchText: String) = mTaskDao.getTasksForSearch(searchText)

    fun getAllTasks(): ArrayList<Task> {
        val taskList = arrayListOf<Task>()
        Observable.fromCallable { mTaskDao.getAllTasks() }.subscribeOn(Schedulers.io())
                .flatMap { tasks -> Observable.fromIterable(tasks) }
                .subscribeBy(onNext = { task -> taskList.add(task) })
        return taskList
    }

    // --- Subtasks -----------------------------------------------------------

    fun subtasksLiveData(taskId: Long) = mSubtaskDao.forTaskLiveData(taskId)

    fun subtasksFor(taskId: Long): List<Subtask> = mSubtaskDao.forTask(taskId)

    fun allSubtasks(): List<Subtask> = mSubtaskDao.all()

    fun saveSubtask(subtask: Subtask) =
            Completable.fromCallable { mSubtaskDao.save(subtask) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun updateSubtask(subtask: Subtask) =
            Completable.fromCallable { mSubtaskDao.update(subtask) }.subscribeOn(Schedulers.io()).subscribe()!!

    fun deleteSubtask(subtask: Subtask) =
            Completable.fromCallable { mSubtaskDao.delete(subtask) }.subscribeOn(Schedulers.io()).subscribe()!!

    // --- Bento stats --------------------------------------------------------

    /** Reads on the caller's thread; callers dispatch it off the main thread. */
    fun statsNow(): TaskStats {
        val dayStart = TaskStats.startOfToday()
        val dayEnd = dayStart + TaskStats.DAY_MS
        val completions = mTaskDao.completionTimes()
        return TaskStats(
                dueToday = mTaskDao.countDueToday(dayStart, dayEnd),
                completedToday = mTaskDao.countCompletedBetween(dayStart, dayEnd),
                overdue = mTaskDao.countOverdue(dayStart),
                outstanding = mTaskDao.countOutstanding(),
                streakDays = TaskStats.streakFrom(completions),
                week = TaskStats.weekFrom(completions),
                nextTask = mTaskDao.nextTask()
        )
    }
}