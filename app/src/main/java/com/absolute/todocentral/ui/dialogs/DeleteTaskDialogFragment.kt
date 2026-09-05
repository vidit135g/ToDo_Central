package com.absolute.todocentral.ui.dialogs

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.absolute.todocentral.R
import com.absolute.todocentral.data.models.Task
import com.absolute.todocentral.service.alarm.AlarmHelper
import com.absolute.todocentral.ui.dialogs.base.BaseDialogFragment
import com.absolute.todocentral.vm.DeleteTaskViewModel

class DeleteTaskDialogFragment(var task: Task = Task()) : BaseDialogFragment() {
    private lateinit var mViewModel: DeleteTaskViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.dialog_default, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { mViewModel = createViewModel(it.application) }
        initDialog(view)
    }

    private fun initDialog(view: View) {
        val tvDialogMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val tvConfirm = view.findViewById<TextView>(R.id.tvConfirm)
        val tvCancel = view.findViewById<TextView>(R.id.tvCancel)

        tvDialogMessage.setText(R.string.dialog_message)
        tvConfirm.setText(R.string.action_delete)
        tvConfirm.setOnClickListener {
            mViewModel.deleteTask(task)
            if (task.date != 0L) {
                val alarmHelper = AlarmHelper.getInstance()
                alarmHelper.removeAlarm(task.timeStamp)
                alarmHelper.removeNotification(task.timeStamp, requireActivity().applicationContext)
            }
            activity?.finish()
        }
        tvCancel.setOnClickListener { dismiss() }
    }

    private fun createViewModel(application: Application) = ViewModelProvider(this)[DeleteTaskViewModel::class.java]
}
