package com.jk.ime

import android.view.View

open class JKTab(
    val key: String,
    private val item: View,
    private val onClick: ((body: JKTab) -> Unit)? = null
) {

    init {
        onClick?.let { item.setOnClickListener { onClick?.invoke(this) } }

    }


    fun setSelected(selected: Boolean) {
        item.isSelected = selected
    }
}