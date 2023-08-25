package com.a.hykimeapp

import android.app.Application
import com.ime.hyk.HykIme
import com.ime.hyk.HykImeService
import timber.log.Timber

/**
 * Custom Application class.
 * Application class will only be created once when the app run,
 * so you can init a "global" class here, whose methods serve other
 * classes everywhere.
 */
class HykApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        HykIme.initConfig(this)
        try {
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            }
        } catch (e: Exception) {
            e.fillInStackTrace()
            return
        }
    }
}
