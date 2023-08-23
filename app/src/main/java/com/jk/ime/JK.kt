package com.jk.ime

import android.view.View
import com.blankj.utilcode.util.ToastUtils
import com.osfans.trime.databinding.MainInputLayoutBinding
import com.osfans.trime.ime.core.Trime
import com.osfans.trime.ime.keyboard.KeyboardView

object JK {

    var hasCandidate = false
    var candidateExpanded = false


    var tabKeys =
        listOf(
            JKViewKey.GOODS,
            JKViewKey.Shared,
            JKViewKey.Setting,
            JKViewKey.Hide
        )

     var keyboardView: KeyboardView?=null
     var candidateBarView: View?=null
     var tabBarView: View?=null


    private var bodyMap = mapOf<String, JKBody>()
    private var tabMap = mapOf<String, JKTab>()


    fun init(binding: MainInputLayoutBinding) {
        this.candidateBarView = binding.candidateView.candidateBarView
        this.tabBarView = binding.tabBarView
        binding.candidateView.candidateDown.setOnClickListener { expandCandidate(listOf("a", "b", "c")) }
        initTabBar(binding)
        initBody(binding)
        updateJKUI(false, JKViewKey.KeyBoard)
    }


  private  fun initTabBar(binding: MainInputLayoutBinding) {
        val list = listOf(
            JKTab(JKViewKey.GOODS, binding.tvTabGoods) { onJKTabClick(it) },
            JKTab(JKViewKey.Shared, binding.tvTabShared) { onJKTabClick(it) },
            JKTab(JKViewKey.Setting, binding.tvTabSetting) { onJKTabClick(it) },
            JKTab(JKViewKey.Hide, binding.tvTabHide) { onJKTabClick(it) },
        )
        tabMap = list.associateBy { it.key }
    }

    private  fun initBody(binding: MainInputLayoutBinding) {

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

    private fun onJKTabClick(jkTab: JKTab) {
        val key: String = jkTab.key
        hasCandidate = false
        candidateExpanded = false
        if (JKViewKey.isHide(key)) {
            hideIme()
        } else {
            updateJKUI(hasCandidate, key)
        }

    }

    private  fun onJKBodyBack(jkBody: JKBody) {
        updateJKUI(hasCandidate, JKViewKey.KeyBoard)

    }

    private fun hideIme() {
        Trime.getServiceOrNull()?.requestHideSelf(0)
    }

    fun onUpdateComposing(hasCandidate: Boolean) {
        this.hasCandidate = hasCandidate
        candidateExpanded = false
        updateJKUI(hasCandidate, JKViewKey.KeyBoard)

    }

    private  fun expandCandidate(candidates: List<String>) {
        hasCandidate = true
        candidateExpanded = true
        updateJKUI(hasCandidate, JKViewKey.CandidateExpand)
    }

    private fun updateJKUI(hasCandidate: Boolean, showBodyKey: String) {
//        JLog.d("updateJKUI hasCandidate:" + hasCandidate + " , showBodyKey:" + showBodyKey, true)
        val isCandidateExpand: Boolean = JKViewKey.isCandidateExpand(showBodyKey)
        val isKeyBoard: Boolean = JKViewKey.isKeyBoard(showBodyKey)
        this.candidateBarView?.visibility = if (hasCandidate && !isCandidateExpand) View.VISIBLE else View.GONE
        this.tabBarView?.visibility = if (!hasCandidate && isKeyBoard) View.VISIBLE else View.GONE

        tabMap.forEach { entry ->
            entry.value.setSelected(entry.key == showBodyKey)
        }

        bodyMap.forEach { it.value.setVisible(it.key == showBodyKey) }
    }


}