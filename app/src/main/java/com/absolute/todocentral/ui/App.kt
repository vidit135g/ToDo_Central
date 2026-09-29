package com.absolute.todocentral.ui

import android.app.Application
import com.absolute.todocentral.R
import com.absolute.todocentral.utils.PreferenceHelper
import com.absolute.todocentral.utils.SomaPeriod

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Activities also init this in onCreate, but the period has to be
        // resolved here first, before any of them exist.
        PreferenceHelper.getInstance().init(applicationContext)

    }
}
