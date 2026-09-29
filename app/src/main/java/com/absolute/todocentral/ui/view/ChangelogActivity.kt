package com.absolute.todocentral.ui.view

import android.os.Bundle
import android.view.MenuItem
import com.absolute.todocentral.BuildConfig
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.base.BaseActivity
import com.absolute.todocentral.utils.PreferenceHelper
import com.absolute.todocentral.utils.applySomaTheme
import kotterknife.bindView
import android.widget.TextView

class ChangelogActivity : BaseActivity() {
    val btnConfirm: TextView by bindView(R.id.btnConfirm)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySomaTheme()
        setContentView(R.layout.activity_changelog)
        // The page carries its own headline, so the bar holds only the close
        // control rather than repeating the title.
        initToolbar("", R.drawable.round_close_black_24)
        btnConfirm.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == android.R.id.home) {
                onBackPressedDispatcher.onBackPressed()
                true
            } else false

    /**
     * Records that this version's changelog has been seen.
     *
     * This used to hang off an onBackPressed() override. Targeting API 36
     * turns predictive back on by default, and the system then dispatches
     * through OnBackInvokedCallback rather than that override — so a gesture
     * back would have dismissed the screen without ever marking it read, and
     * it would reappear on the next launch. Doing it here catches every way
     * out: gesture, button, the toolbar X, and Confirm.
     */
    override fun onStop() {
        super.onStop()
        PreferenceHelper.getInstance().putInt(PreferenceHelper.VERSION_CODE, BuildConfig.VERSION_CODE)
    }
}