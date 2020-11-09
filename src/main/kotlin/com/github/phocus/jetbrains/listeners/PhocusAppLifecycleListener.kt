package com.github.phocus.jetbrains.listeners

import com.github.phocus.jetbrains.ui.NavBarUI
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.navigationToolbar.ui.NavBarUIManager
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.util.ui.JBUI
import javassist.ClassPool
import javassist.Modifier
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.jetbrains.annotations.NonNls
import java.lang.reflect.Field
import javax.swing.UIManager

object PhocusAppLifecycleListener : AppLifecycleListener {

    private const val barHeights = 34
    private const val treeRowHeight = 28
    private const val scrollBarArc = 7

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        disableInternalDecoratorBorder()
        setNavBarHeight(this.barHeights)
        setToolWindowHeaderHeight(this.barHeights)
        setSingleHeightTabsHeight(this.barHeights)
        setTreeRowHeight(this.treeRowHeight)
        setScrollbarArc(this.scrollBarArc)
    }

    /**
     * This forces tool window panels borders to be 0.
     * This is the 1 pixel wide draggable line between editor and tool windows.
     *
     * @see com.intellij.openapi.wm.impl.InternalDecorator.InnerPanelBorder.getBorderInsets
     */
    private fun disableInternalDecoratorBorder() {
        val cp = ClassPool(true)
        val ctClass = cp["com.intellij.openapi.wm.impl.InternalDecorator\$InnerPanelBorder"]
        ctClass.getDeclaredMethod("getBorderInsets").setBody("{ return new java.awt.Insets(0, 0, 0, 0); }")
        ctClass.toClass()
    }

    /**
     * Sets the navbar height (breadcrumb like thing) by providing a
     * custom NavBarUI implementation instance.
     */
    private fun setNavBarHeight(height: Int) {
        overWriteFinalStaticField(NavBarUIManager::class.java, "DARCULA", NavBarUI(height))
    }

    /**
     * Sets the tool window header height by forcing a preferred height.
     *
     * @see com.intellij.openapi.wm.impl.ToolWindowHeader.getPreferredSize
     */
    private fun setToolWindowHeaderHeight(height: Int) {
        val ctClass = ClassPool(true)["com.intellij.openapi.wm.impl.ToolWindowHeader"]
        ctClass.getDeclaredMethod("getPreferredSize").setBody(
            """{ return new java.awt.Dimension(
                    super.getPreferredSize().width,
                    com.intellij.util.ui.JBUI.scale($height));}
            """.trimIndent()
        )
        ctClass.toClass()
    }

    /**
     * Sets the editor tabs height, by forcing a preferred height.
     *
     * @see com.intellij.ui.tabs.impl.SingleHeightTabs.SingleHeightLabel.getPreferredHeight
     */
    private fun setSingleHeightTabsHeight(height: Int) {
        val ctClass = ClassPool(true)["com.intellij.ui.tabs.impl.SingleHeightTabs\$SingleHeightLabel"]
        ctClass.getDeclaredMethod("getPreferredHeight")
            .setBody("{ return com.intellij.util.ui.JBUI.scale($height); }")
        ctClass.toClass()
    }

    /**
     * Sets the tree row height which currently can't be set in the theme.
     * Please fix @Jetbrains
     */
    private fun setTreeRowHeight(height: Int) {
        UIManager.put("Tree.rowHeight", JBUI.scale(height))
    }

    /**
     * Overwrites paint call to set the arc parameter and add some small paddings.
     *
     * @see com.intellij.ui.components.ScrollBarPainter.Thumb.paint
     */
    private fun setScrollbarArc(arc: Int) {
        if (SystemInfoRt.isMac) return
        val ctClass = ClassPool(true)["com.intellij.ui.components.ScrollBarPainter\$Thumb"]
        ctClass.getDeclaredMethod("paint").instrument(
            object : ExprEditor() {
                override fun edit(m: MethodCall) {
                    if (m.methodName == "paint") {
                        m.replace(
                            """{
                                $2 += 1; $3 += 1; $4 -= 2; $5 -= 2;
                                $6 = com.intellij.util.ui.JBUI.scale($arc);
                                ${'$'}proceed($$);}
                            """.trimIndent()
                        )
                    }
                }
            }
        )
        ctClass.toClass()
    }

    /**
     * Overwrites a static final fields value
     */
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun overWriteFinalStaticField(cls: Class<*>, @NonNls fieldName: String, newValue: Any?) {
        for (field in cls.declaredFields) {
            if (field.name == fieldName) {
                field.isAccessible = true
                val modifiersField = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true
                modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
                field[null] = newValue
                modifiersField.setInt(field, field.modifiers or Modifier.FINAL)
                modifiersField.isAccessible = false
                field.isAccessible = false
                return
            }
        }
    }
}
