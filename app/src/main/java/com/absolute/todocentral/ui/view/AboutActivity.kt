package com.absolute.todocentral.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import com.absolute.todocentral.BuildConfig
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.base.BaseActivity
import com.absolute.todocentral.utils.applySomaTheme
import com.absolute.todocentral.utils.toastLong
import kotterknife.bindView

/**
 * About. The app shipped without one, so the version, the build details and
 * the open-source notices had nowhere to live.
 *
 * Everything on the build card is read from [BuildConfig] rather than written
 * into a layout, so it cannot fall out of step with what was actually shipped.
 */
class AboutActivity : BaseActivity() {

    private val tvVersion: TextView by bindView(R.id.tvAboutVersion)
    private val llBuild: LinearLayout by bindView(R.id.llAboutBuild)
    private val tvWhatsNew: TextView by bindView(R.id.tvAboutWhatsNew)
    private val tvLicences: TextView by bindView(R.id.tvAboutLicences)
    private val tvPrivacy: TextView by bindView(R.id.tvAboutPrivacy)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySomaTheme()
        setContentView(R.layout.activity_about)
        initToolbar(getString(R.string.settings_about))

        tvVersion.text = getString(R.string.about_version,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        buildRow(R.string.about_row_package, BuildConfig.APPLICATION_ID)
        buildRow(R.string.about_row_version, BuildConfig.VERSION_NAME)
        buildRow(R.string.about_row_build, BuildConfig.VERSION_CODE.toString())
        buildRow(R.string.about_row_flavour, BuildConfig.BUILD_TYPE)
        buildRow(R.string.about_row_android, "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")

        tvWhatsNew.setOnClickListener {
            startActivity(Intent(this, ChangelogActivity::class.java))
        }
        tvLicences.setOnClickListener {
            startActivity(Intent(this, LicencesActivity::class.java))
        }
        tvPrivacy.setOnClickListener { open(PRIVACY_URL) }
    }

    /** One label/value line on the build card. */
    private fun buildRow(labelRes: Int, value: String) {
        val row = LayoutInflater.from(this)
                .inflate(R.layout.about_build_row, llBuild, false)
        row.findViewById<TextView>(R.id.tvBuildLabel).setText(labelRes)
        row.findViewById<TextView>(R.id.tvBuildValue).text = value
        llBuild.addView(row)
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toastLong(getString(R.string.about_no_browser))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == android.R.id.home) {
                onBackPressedDispatcher.onBackPressed()
                true
            } else false

    companion object {
        private const val PRIVACY_URL =
                "https://play.google.com/store/apps/details?id=com.absolute.todocentral"
    }
}
