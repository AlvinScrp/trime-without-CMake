package com.jk.ime

object JKViewKey {

    const val KeyBoard = "keyBoard" //键盘布局
    const val GOODS = "goods"
    const val Shared = "Shared"
    const val Setting = "InputSetting" // 输入法设置
    const val CandidateExpand = "CandidateExpand"
//    const val Hide = "Hide"   //关闭输入法


    @JvmStatic
    fun isCandidateExpand(key: String): Boolean = key == CandidateExpand

    @JvmStatic
    fun isKeyBoard(key: String): Boolean = key == KeyBoard

//    @JvmStatic
//    fun isHide(key: String): Boolean = key == Hide
}