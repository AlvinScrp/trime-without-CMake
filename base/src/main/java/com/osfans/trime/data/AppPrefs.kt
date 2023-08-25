package com.osfans.trime.data

import com.osfans.trime.ime.enums.InlineModeType
import com.osfans.trime.ime.landscapeinput.LandscapeInputUIMode
import com.osfans.trime.util.App

/**
 * Helper class for an organized access to the shared preferences.
 */
class AppPrefs {

    val keyboard = Keyboard(this)
    val looks = Looks(this)
    val conf = Configuration(this)
    val other = Other(this)

    companion object {
        private var defaultInstance: AppPrefs = AppPrefs()


        @JvmStatic
        fun defaultInstance(): AppPrefs {
            return defaultInstance
        }
    }

    /**
     *  Wrapper class of keyboard preferences.
     */
    class Keyboard(private val prefs: AppPrefs) {
        var inlinePreedit: InlineModeType =  InlineModeType.INLINE_PREVIEW
        var fullscreenMode: LandscapeInputUIMode =LandscapeInputUIMode.AUTO_SHOW
        var softCursorEnabled: Boolean = true
        var popupWindowEnabled: Boolean = false
        var popupKeyPressEnabled: Boolean = true
        var switchesEnabled: Boolean = true
        var switchArrowEnabled: Boolean = true
        var candidatePageSize: String = "30"
        var hookFastInput: Boolean = false
        var hookCandidate: Boolean = false
        var hookCandidateCommit: Boolean = false
        var hookCtrlA: Boolean = false
        var hookCtrlCV: Boolean = false
        var hookCtrlLR: Boolean = false
        var hookCtrlZY: Boolean = false
        var swipeEnabled: Boolean = true
        var swipeTravel: Int = 80
        var swipeTravelHi: Int = 200
        var swipeVelocity: Int = 800
        var swipeVelocityHi: Int = 25000
        var swipeTimeHi: Int = 80
        var longPressTimeout: Int = 20
        var repeatInterval: Int = 4
        var deleteCandidateTimeout: Int = 2000
        var shouldLongClickDeleteCandidate: Boolean = false
    }

    /**
     *  Wrapper class of keyboard appearance preferences.
     */
    class Looks(private val prefs: AppPrefs) {
        var selectedTheme: String = "jk.trime"
        var selectedColor: String = "default"

    }

    /**
     *  Wrapper class of configuration settings.
     */
    class Configuration(private val prefs: AppPrefs) {
        companion object {
            val EXTERNAL_PATH_PREFIX: String = App.context?.getExternalFilesDir(null)!!.absolutePath
        }
        var sharedDataDir: String = "$EXTERNAL_PATH_PREFIX/rime-jk-share"
        var userDataDir: String = "$EXTERNAL_PATH_PREFIX/rime-jk-user"

    }

    /**
     *  Wrapper class of configuration settings.
     */
    class Other(private val prefs: AppPrefs) {

        var clipboardCompareRules: String = ""
        var clipboardOutputRules: String = ""
        var draftOutputRules: String = ""
        var clipboardLimit: String = "50"
        var draftLimit: String = "20"
    }
}
