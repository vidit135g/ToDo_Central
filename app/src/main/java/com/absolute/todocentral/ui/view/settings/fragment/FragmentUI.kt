package com.absolute.todocentral.ui.view.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.settings.fragment.base.BaseSettingsFragment
import com.absolute.todocentral.utils.PreferenceHelper
import com.absolute.todocentral.utils.SomaPeriod
import kotterknife.bindView
import androidx.appcompat.widget.SwitchCompat

class FragmentUI : BaseSettingsFragment() {
    val swAnimation: SwitchCompat by bindView(R.id.swAnimation)
    val clAnimations: View by bindView(R.id.clAnimations)
    val clChooseTheme: View by bindView(R.id.clChooseTheme)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_interface, container, false)
    }

    override fun onResume() {
        super.onResume()
        setTitle(getString(R.string.settings_page_title_user_interface))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAnimationSwitch()
    }

    private fun initAnimationSwitch() {
        val preferenceHelper = PreferenceHelper.getInstance()

        swAnimation.setOnTouchListener { _, event -> event.actionMasked == MotionEvent.ACTION_MOVE }
        swAnimation.isChecked = preferenceHelper.getBoolean(PreferenceHelper.ANIMATION_IS_ON)

        swAnimation.setOnClickListener {
            preferenceHelper.putBoolean(PreferenceHelper.ANIMATION_IS_ON, swAnimation.isChecked)
        }

        clAnimations.setOnClickListener {
            swAnimation.isChecked = !swAnimation.isChecked
            preferenceHelper.putBoolean(PreferenceHelper.ANIMATION_IS_ON, swAnimation.isChecked)
        }

        clChooseTheme.setOnClickListener {
            val listItems = resources.getStringArray(R.array.app_theme)
            // Index 0 is Auto; 1..6 map onto SomaPeriod.ORDER.
            val saved = SomaPeriod.getPreference()
            var selectedItemPosition =
                    if (saved == SomaPeriod.AUTO) 0
                    else SomaPeriod.ORDER.indexOf(saved).let { if (it < 0) 0 else it + 1 }

            val builder = AlertDialog.Builder(activity as Context,
                    if (SomaPeriod.isLight()) R.style.AlertDialogStyle_Light
                    else R.style.AlertDialogStyle_Dark)
            builder.apply {
                setTitle(getString(R.string.app_theme_dialog_title))
                setSingleChoiceItems(listItems, selectedItemPosition) { dialogInterface, i ->
                    selectedItemPosition = i
                    SomaPeriod.setPreference(
                            if (i == 0) SomaPeriod.AUTO else SomaPeriod.ORDER[i - 1])
                    isThemeChanged = true
                    dialogInterface.dismiss()
                    activity?.recreate()
                }
            }
            builder.create().apply {
                window?.attributes?.windowAnimations = R.style.DialogAnimation
                show()
                window?.setLayout(resources.getDimensionPixelSize(R.dimen.dialog_picker_width), ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    companion object {
        var isThemeChanged = false
    }
}