package com.absolute.todocentral.ui.dialogs

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.dialogs.base.BaseDialogFragment
import com.absolute.todocentral.utils.toast
import com.absolute.todocentral.vm.RestoreBackupViewModel

class RestoreBackupDialogFragment : BaseDialogFragment() {
    private lateinit var mViewModel: RestoreBackupViewModel

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

        tvDialogMessage.setText(R.string.backup_restore_dialog_message)
        tvConfirm.setText(R.string.backup_restore_dialog_button)
        tvConfirm.setOnClickListener { restoreBackup() }
        tvCancel.setOnClickListener { dismiss() }
    }

    private fun restoreBackup() {
        if (mViewModel.isBackupExist()) {
            mViewModel.restoreBackup()

            if (mViewModel.isBackupRestoredSuccessfully()) {
                dismiss()
                toast(getString(R.string.backup_restore_message_success))
            } else {
                dismiss()
                toast(getString(R.string.backup_restore_message_failure))
            }
        } else {
            dismiss()
            toast(getString(R.string.backup_restore_message_nothing))
        }
    }

    private fun createViewModel(application: Application) = ViewModelProvider(this)[RestoreBackupViewModel::class.java]
}
