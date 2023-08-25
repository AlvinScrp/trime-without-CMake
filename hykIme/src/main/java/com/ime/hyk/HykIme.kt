package com.ime.hyk

import android.content.ComponentName
import android.content.Context
import com.osfans.trime.ime.core.Trime
import com.osfans.trime.util.App
import com.osfans.trime.util.InputMethodUtils
import com.osfans.trime.util.RimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

object HykIme {
   private lateinit var context: Context

    fun initConfig(context: Context) {
        this.context = context
        App.context = context
        InputMethodUtils.serviceName = ComponentName(context, HykImeService::class.java).flattenToShortString()

    }

    fun initKeyboardAndDeploy(finallyDo: () -> Unit) {
        Trime.getServiceOrNull()?.initKeyboard()
        MainScope().launch {
            try {
                RimeUtils.deploy(context)
            } catch (ex: Exception) {
                Timber.e(ex, "Deploy Exception")
            } finally {
                finallyDo.invoke()
            }
        }

    }
}