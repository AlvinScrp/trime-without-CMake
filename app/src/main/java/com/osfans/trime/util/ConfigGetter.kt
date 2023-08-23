package com.osfans.trime.util

import android.util.TypedValue
import com.blankj.utilcode.util.SizeUtils
import com.jk.ime.JLog
import com.osfans.trime.core.Rime
import com.osfans.trime.util.ConfigGetter.getFloat
import com.osfans.trime.util.ConfigGetter.getInt

object ConfigGetter {

    @JvmStatic
    fun loadMap(name: String, key: String = ""): Map<String, *>? = Rime.config_get_map(name, key)

    private val cacheFloat = mutableMapOf<Any, Float>()
    private val cacheDouble = mutableMapOf<Any, Double>()
    private val cacheInt = mutableMapOf<Any, Int>()
    private val cachePixel = mutableMapOf<Float, Int>()

    @JvmStatic
    fun Map<String, *>.getInt(key: String, default: Int): Int {
        val o: Any = this[key] ?: default
        return cacheInt[o] ?: run { o.toString().toInt().also { cacheInt[o] = it } }
    }

    @JvmStatic
    fun Map<String, *>.getFloat(key: String, default: Float): Float {
//        JLog.d("getFloat ${key}, ${default}")
        val o: Any = this[key] ?: default
        return cacheFloat[o] ?: run { o.toString().toFloat().also { cacheFloat[o] = it } }
    }

    @JvmStatic
    fun Map<String, *>.getDouble(key: String, default: Double): Double {
        val o: Any = this[key] ?: default
        return cacheDouble[o] ?: run { o.toString().toDouble().also { cacheDouble[o] = it } }
    }

    @JvmStatic
    fun Map<String, *>.getString(key: String, default: String): String {
        val o = this.getOrElse(key) { default }
        return o.toString()
    }

    @JvmStatic
    fun Map<String, *>.getBoolean(key: String, default: Boolean): Boolean {
        val o = this.getOrElse(key) { default }
        return o.toString().toBooleanStrict()
    }

    @JvmStatic
    fun Map<String, *>.getPixel(key: String, default: Float): Int {
//        JLog.d("getPixel ${key}, ${default}")
        val f = this.getFloat(key, default)
        return cachePixel[f] ?: run {
            SizeUtils.applyDimension(f, TypedValue.COMPLEX_UNIT_SP).toInt().also { cachePixel[f] = it }
        }
    }
}
