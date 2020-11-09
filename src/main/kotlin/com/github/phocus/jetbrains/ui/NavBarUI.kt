package com.github.phocus.jetbrains.ui

import com.intellij.ide.navigationToolbar.ui.CommonNavBarUI
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI

class NavBarUI(val height: Int) : CommonNavBarUI() {
    override fun getElementPadding(): JBInsets {
        return JBUI.insets( JBUI.scale(height) - 19, 3, 0, 3)
    }
}