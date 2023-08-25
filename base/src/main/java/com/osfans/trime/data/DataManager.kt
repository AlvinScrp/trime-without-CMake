package com.osfans.trime.data

import java.io.File

object DataManager {
    private val prefs get() = AppPrefs.defaultInstance()

    @JvmStatic
    fun getDataDir(child: String = ""): String {
        return "${prefs.conf.userDataDir}${File.separator}${child}"
//        return  File(prefs.conf.userDataDir, child).absolutePath
    }
}
