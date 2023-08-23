package com.jk.ime

import android.util.Log

object JLog {
    @JvmStatic
    fun d(msg: String?, stackTrace: Boolean = false) {
        if (!stackTrace) {
            Log.d("alvin", msg!!)
        } else {
            Log.d("alvin", Log.getStackTraceString(Throwable(msg)))
        }
    }
}
