package com.github.phocus.jetbrains.ui

import com.intellij.ide.navigationToolbar.ui.CommonNavBarUI
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI

class NavBarUI(private val height: Int) : CommonNavBarUI() {
    companion object {
        private const val contentHeight = 19
        private const val leftRightPadding = 3
    }

    override fun getElementPadding(): JBInsets {
        return JBUI.insets(
            JBUI.scale(height) - NavBarUI.contentHeight,
            NavBarUI.leftRightPadding,
            0,
            NavBarUI.leftRightPadding
        )
    }
}
