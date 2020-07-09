package com.absolute.todocentral.ui.view.settings.fragment.base

import androidx.fragment.app.Fragment
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.settings.activity.SettingsActivity

abstract class BaseSettingsFragment : Fragment() {

    fun setTitle(title: String) {
        (activity as SettingsActivity).setToolbarTitle(title)
    }

    override fun onDetach() {
        super.onDetach()
        setTitle(getString(R.string.settings))
    }
}