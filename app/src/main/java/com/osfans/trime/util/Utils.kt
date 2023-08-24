package com.osfans.trime.util

import android.content.Context
import com.blankj.utilcode.util.ToastUtils
import com.osfans.trime.R
import com.osfans.trime.TrimeApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val appContext: Context get() = TrimeApplication.getInstance().applicationContext

@Suppress("NOTHING_TO_INLINE")
inline fun CharSequence.startsWithAsciiChar(): Boolean {
    val firstCodePoint = this.toString().codePointAt(0)
    return firstCodePoint in 0x20 until 0x80
}
