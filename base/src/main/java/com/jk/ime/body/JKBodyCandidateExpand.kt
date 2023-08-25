package com.jk.ime.body

import android.view.View
import com.jk.ime.JKBody

class JKBodyCandidateExpand(key: String, content: View, onBack: ((body: JKBody) -> Unit)? = null) :
    JKBody(key, content, onBack) {

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if(visible){

        }
    }
}