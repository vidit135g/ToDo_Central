package com.absolute.todocentral.ui.view.settings.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.settings.activity.SettingsActivity
import com.absolute.todocentral.ui.view.settings.fragment.base.BaseSettingsFragment
import com.absolute.todocentral.utils.toast
import com.absolute.todocentral.vm.CreateBackupViewModel
import com.absolute.todocentral.vm.RestoreBackupViewModel
import kotterknife.bindView

class FragmentBackupAndRestore : BaseSettingsFragment() {
    val clCreateBackup: View by bindView(R.id.clCreateBackup)
    val clRestoreBackup: View by bindView(R.id.clRestoreBackup)
    private lateinit var mSettingsActivity: SettingsActivity

    private val CREATE_BACKUP_REQUEST = 101
    private val RESTORE_BACKUP_REQUEST = 102

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_backup_and_restore, container, false)
    }

    override fun onResume() {
        super.onResume()
        setTitle(getString(R.string.settings_page_title_backup_and_restore))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSettingsActivity = activity as SettingsActivity
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        clCreateBackup.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, "Backup.ser")
            }
            startActivityForResult(intent, CREATE_BACKUP_REQUEST)
        }

        clRestoreBackup.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            startActivityForResult(intent, RESTORE_BACKUP_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri = data.data!!
            if (requestCode == CREATE_BACKUP_REQUEST) {
                val vm = ViewModelProvider(this)[CreateBackupViewModel::class.java]
                vm.createBackup(uri)
                if (vm.isBackupCreatedSuccessfully()) {
                    toast(getString(R.string.backup_create_message_success))
                } else {
                    toast(getString(R.string.backup_create_message_failure))
                }
            } else if (requestCode == RESTORE_BACKUP_REQUEST) {
                val vm = ViewModelProvider(this)[RestoreBackupViewModel::class.java]
                vm.restoreBackup(uri)
                if (vm.isBackupRestoredSuccessfully()) {
                    toast(getString(R.string.backup_restore_message_success))
                } else {
                    toast(getString(R.string.backup_restore_message_failure))
                }
            }
        }
    }
}
