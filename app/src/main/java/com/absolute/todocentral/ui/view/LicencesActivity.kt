package com.absolute.todocentral.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import com.absolute.todocentral.R
import com.absolute.todocentral.ui.view.base.BaseActivity
import com.absolute.todocentral.utils.applySomaTheme
import kotterknife.bindView

/**
 * Open-source notices for the libraries this app ships.
 *
 * Written out here rather than generated at build time: the dependency list is
 * short and stable, and a hand-kept list that matches `app/build.gradle` beats
 * pulling in a plugin for six entries. Anyone adding a dependency adds a line.
 */
class LicencesActivity : BaseActivity() {

    private val container: LinearLayout by bindView(R.id.llLicences)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySomaTheme()
        setContentView(R.layout.activity_licences)
        initToolbar(getString(R.string.about_licences))

        NOTICES.forEach { (name, kind, holder) ->
            val card = LayoutInflater.from(this)
                    .inflate(R.layout.licence_card, container, false)
            card.findViewById<TextView>(R.id.tvLicenceName).text = name
            card.findViewById<TextView>(R.id.tvLicenceKind).text = kind
            card.findViewById<TextView>(R.id.tvLicenceHolder).text = holder
            container.addView(card)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
            if (item.itemId == android.R.id.home) {
                onBackPressedDispatcher.onBackPressed()
                true
            } else false

    companion object {
        /** name, licence, copyright holder — mirrors app/build.gradle. */
        private val NOTICES = listOf(
                Triple("AndroidX", "Apache License 2.0", "Copyright The Android Open Source Project"),
                Triple("Material Components for Android", "Apache License 2.0", "Copyright Google LLC"),
                Triple("Kotlin", "Apache License 2.0", "Copyright JetBrains s.r.o. and contributors"),
                Triple("Room", "Apache License 2.0", "Copyright The Android Open Source Project"),
                Triple("RxJava / RxKotlin", "Apache License 2.0", "Copyright RxJava and RxKotlin contributors"),
                Triple("Lottie for Android", "Apache License 2.0", "Copyright Airbnb, Inc."),
                Triple("CircularAnim", "Apache License 2.0", "Copyright XunMengWinter"),
                Triple("Material Intro", "Apache License 2.0", "Copyright Jan Heinrich Reimer"),
                Triple("KotterKnife", "Apache License 2.0", "Copyright Jake Wharton"),
                Triple("Poppins", "SIL Open Font License 1.1", "Copyright The Poppins Project Authors")
        )
    }
}
