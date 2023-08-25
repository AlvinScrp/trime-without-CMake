package com.jk.ime

import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.osfans.trime.core.Rime
import com.osfans.trime.databinding.MainInputLayoutBinding
import com.osfans.trime.ime.core.Trime
import com.osfans.trime.ime.keyboard.KeyboardView

object JK {

    var candidateExpanded = false

    var keyboardView: KeyboardView? = null
    var candidateBarView: View? = null
    var tabBarView: View? = null


    private var bodyMap = mapOf<String, JKBody>()

    fun init(binding: MainInputLayoutBinding) {
        this.candidateBarView = binding.candidateView.candidateBarView
        this.tabBarView = binding.tabBarView
        binding.candidateView.candidateDown.setOnClickListener { expandCandidate() }
        initTabBar(binding)
        initBody(binding)
        updateJKUI(JKViewKey.KeyBoard)
    }


    private fun initTabBar(binding: MainInputLayoutBinding) {
        binding.tvTabGoods.setOnClickListener { onJKTabClick(JKViewKey.GOODS) }
        binding.tvTabShared.setOnClickListener { onJKTabClick(JKViewKey.Shared) }
        binding.tvTabSetting.setOnClickListener { onJKTabClick(JKViewKey.Setting) }
        binding.tvTabHide.setOnClickListener { hideIme() }
    }

    private fun initBody(binding: MainInputLayoutBinding) {

        val keyboardView: KeyboardView = binding.mainKeyboardView
        val goodsView: View = binding.llGoods
        val sharedView: View = binding.llShare
        val settingView: View = binding.llSetting
        val candidateExpandView: View = binding.llCandidateExpand

        val list = listOf(
            JKBody(JKViewKey.KeyBoard, keyboardView),
            JKBody(JKViewKey.GOODS, goodsView) { onJKBodyBack(it) },
            JKBody(JKViewKey.Shared, sharedView) { onJKBodyBack(it) },
            JKBody(JKViewKey.Setting, settingView) { onJKBodyBack(it) },
            JKBody(JKViewKey.CandidateExpand, candidateExpandView) { onJKBodyBack(it) },
        )
        bodyMap = list.associateBy { it.key }

    }

    private fun onJKTabClick(key: String) {
//        val key: String = jkTab.key
        candidateExpanded = false
//        if (JKViewKey.isHide(key)) {
//            hideIme()
//        } else {
        updateJKUI(key)
//        }

    }

    private fun onJKBodyBack(jkBody: JKBody) {
        updateJKUI(JKViewKey.KeyBoard)

    }

    private fun hasCadidate(): Boolean {
        return (Rime.getCandidates()?.size ?: 0) > 0
    }

    private fun hideIme() {
        Trime.getServiceOrNull()?.requestHideSelf(0)
    }

    fun onUpdateComposing() {
        candidateExpanded = false
        updateJKUI(JKViewKey.KeyBoard)

    }

    private fun expandCandidate() {
        candidateExpanded = true
        updateJKUI(JKViewKey.CandidateExpand)
    }

    private fun updateJKUI(showBodyKey: String) {
        val hasCandidate = hasCadidate()
        val isCandidateExpand: Boolean = JKViewKey.isCandidateExpand(showBodyKey)
        if (isCandidateExpand && !hasCandidate) {
            return
        }
        val isKeyBoard: Boolean = JKViewKey.isKeyBoard(showBodyKey)
        this.candidateBarView?.visibility = if (hasCandidate && !isCandidateExpand) View.VISIBLE else View.GONE
        this.tabBarView?.visibility = if (!hasCandidate && isKeyBoard) View.VISIBLE else View.GONE

        bodyMap.forEach { it.value.setVisible(it.key == showBodyKey) }
    }


}