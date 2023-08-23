package com.jk.ime

import android.view.View
import android.view.View.OnClickListener

open class JKBody(
    val key: String,
    private val content: View,
    private val onBack: ((body: JKBody) -> Unit)? = null
) {
    init {
        onBack?.let { content.setOnClickListener { onBack?.invoke(this) } }

    }

    open fun setVisible(visible: Boolean) {
        content.visibility = if (visible) View.VISIBLE else View.GONE
    }


}