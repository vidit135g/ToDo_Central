package com.absolute.todocentral.ui.view

import android.os.Bundle
import android.view.MenuItem
import com.absolute.todocentral.BuildConfig
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.base.BaseActivity
import com.absolute.todocentral.utils.PreferenceHelper
import daio.io.dresscode.matchDressCode
import kotlinx.android.synthetic.main.activity_changelog.*

class ChangelogActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matchDressCode()
        setContentView(R.layout.activity_changelog)
        initToolbar(getString(R.string.whats_new_title), R.drawable.round_close_black_24)
        btnConfirm.setOnClickListener { onBackPressed() }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == android.R.id.home) {
                onBackPressed()
                true
            } else false

    override fun onBackPressed() {
        super.onBackPressed()
        PreferenceHelper.getInstance().putInt(PreferenceHelper.VERSION_CODE, BuildConfig.VERSION_CODE)
    }
}