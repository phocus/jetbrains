package com.github.phocus.jetbrains.ui

import com.intellij.ide.navigationToolbar.ui.CommonNavBarUI
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import java.awt.Insets
import javax.swing.UIManager

class NavBarUI : CommonNavBarUI() {
    override fun getElementPadding(): JBInsets {
        val padding = UIManager.getInsets("NavBar.padding")
        if (padding is Insets) {
            return JBUI.insets(padding)
        }
        return super.getElementPadding()
    }
}
