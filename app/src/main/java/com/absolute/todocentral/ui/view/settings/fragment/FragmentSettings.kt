package com.absolute.todocentral.ui.view.settings.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.absolute.todocentral.BuildConfig
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.settings.fragment.base.BaseSettingsFragment
import com.absolute.todocentral.utils.DeviceInfo
import com.absolute.todocentral.utils.toast


class FragmentSettings : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtons()
    }

    private fun initButtons() {
        view?.findViewById<View>(R.id.tvUI)?.setOnClickListener { openFragment(FragmentUI()) }
        view?.findViewById<View>(R.id.tvNotifications)?.setOnClickListener { openFragment(FragmentNotifications()) }
        view?.findViewById<View>(R.id.tvDateAndTime)?.setOnClickListener { openFragment(FragmentDateAndTime()) }
        view?.findViewById<View>(R.id.tvBackupAndRestore)?.setOnClickListener { openFragment(FragmentBackupAndRestore()) }
        view?.findViewById<View>(R.id.tvRate)?.setOnClickListener { rateThisApp() }
        view?.findViewById<View>(R.id.tvFeedback)?.setOnClickListener { sendFeedback() }
        view?.findViewById<View>(R.id.tvOtherApps)?.setOnClickListener { openUri(GOOGLE_PLAY_PAGE) }
    }

    private fun openFragment(fragment: BaseSettingsFragment) =
            fragmentManager?.beginTransaction()?.replace(R.id.flFragmentContainer, fragment)?.addToBackStack(null)?.commit()

    private fun rateThisApp() {
        val appPackageName = activity?.packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$APP_PAGE_SHORT_LINK$appPackageName")))
        } catch (exception: android.content.ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$APP_PAGE_LONG_LINK$appPackageName")))
        }
    }

    private fun sendFeedback() {
        val email = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.Builder().scheme(SCHEME).build()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.author_gmail)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_title))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_device_info) + "\n" + DeviceInfo.deviceInfo
                    + "\n" + getString(R.string.feedback_app_version) + BuildConfig.VERSION_NAME
                    + "\n" + getString(R.string.feedback))
        }

        try {
            startActivity(Intent.createChooser(email, getString(R.string.settings_feedback)))
        } catch (exception: android.content.ActivityNotFoundException) {
            toast(getString(R.string.settings_no_email_apps))
        }
    }

    private fun openUri(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (exception: android.content.ActivityNotFoundException) {
            toast(getString(R.string.settings_no_browser_apps))
        }
    }

    private companion object {
        const val GOOGLE_PLAY_PAGE = "https://play.google.com/store/apps/developer?id=JAi+Gupta"
        const val APP_PAGE_SHORT_LINK = "market://details?id="
        const val APP_PAGE_LONG_LINK = "https://play.google.com/store/com/details?id="
        const val SCHEME = "mailto"
    }
}