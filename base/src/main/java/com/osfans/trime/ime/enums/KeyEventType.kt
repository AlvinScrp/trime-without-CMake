package com.osfans.trime.ime.enums


/** 按键事件枚举  */
enum class KeyEventType(val lower: String = "") {

    // 长按按键展开列表时，正上方为长按对应按键，排序如上，不展示combo及之前的按键，展示extra
    COMPOSING("composing"),
    HAS_MENU("has_menu"),
    PAGING("paging"),
    COMBO("combo"),
    ASCII("ascii"),
    CLICK("click"),
    SWIPE_UP("swipe_up"),
    LONG_CLICK("long_click"),
    SWIPE_DOWN("swipe_down"),
    SWIPE_LEFT("swipe_left"),
    SWIPE_RIGHT("swipe_right"),
    EXTRA("extra");


    companion object {
        private val arrays by lazy {
            arrayListOf<KeyEventType>().also {
                it.addAll(values())
            }
        }

        @JvmStatic
        fun valueOf(ordinal: Int): KeyEventType {
            if (ordinal < 0 || ordinal >= arrays.size) {
                return EXTRA
            }
            return arrays[ordinal]
        }
    }
}
