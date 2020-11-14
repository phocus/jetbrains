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
import java.awt.Color
import java.awt.Insets
import java.lang.reflect.Field
import javax.swing.UIManager

/**
 * This heavily uses instrumentation to do bytecode manipulation at runtime.
 * Doing this is in general a bad idea, but since versions are fixed, and i want
 * my IDE to look a certain way, i don't really care if this is ugly.
 *
 * But just don't use this code to learn from, it's bad!
 */
object PhocusAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        overWriteFinalStaticField(NavBarUIManager::class.java, "DARCULA", NavBarUI())
        makeSingleHeightTabsHeightThemeable()
        makeTreeRowHeightThemeable()
        makeToolWindowHeaderHeightThemeable()
        makeScrollBarThumbArcThemeable()
        makeStripeButtonPaddingThemeable()
        makeStripeBackgroundThemeable()
        removeEditorTabsBorder()
        allowTabOverline()
    }

    /**
     * Allows rendering overlines instead of underlines on tabs
     * Can be set through `Tab.overline=true`
     */
    private fun allowTabOverline() {
        UIManager.getBoolean("Tab.overline")
        if (UIManager.getBoolean("Tab.overline")) {
            val ctClass = ClassPool(true)["com.intellij.ui.tabs.impl.JBDefaultTabPainter"]
            ctClass.getDeclaredMethod("underlineRectangle").setBody(
                """
                    {
                        return new java.awt.Rectangle($2.x, $2.y, $2.width, $3);
                    }
                """.trimIndent()
            )
            ctClass.toClass()
        }
    }

    /**
     * This removes the awkward 1 pixel border above the editor tabs,
     * making the tabs now the same height as the actual tab bar
     */
    private fun removeEditorTabsBorder() {
        val ctClass = ClassPool(true)["com.intellij.ui.tabs.impl.JBEditorTabsBorder"]
        ctClass.getDeclaredMethod("getEffectiveBorder").setBody("{return new java.awt.Insets(0, 0, 0, 0);}")
        ctClass.toClass()
    }

    /**
     * Makes the Stripe background color themeable via `Stripe.background="#RRGGBB"`.
     */
    private fun makeStripeBackgroundThemeable() {
        val backgroundColor = UIManager.getColor("Stripe.background")
        if (backgroundColor is Color) {
            val ctClass = ClassPool(true)["com.intellij.openapi.wm.impl.Stripe"]
            ctClass.getDeclaredMethod("paintComponent").setBody(
                """
                    {
                        $1.setColor(new java.awt.Color(
                            ${backgroundColor.red},
                            ${backgroundColor.green},
                            ${backgroundColor.blue}
                        ));
                        $1.fillRect(0, 0, getWidth(), getHeight());
                    }
                """.trimIndent()
            )
            ctClass.toClass()
        }
    }

    /**
     * Makes it possible to set a StripeButton padding via `Stripe.Button.padding="top,left,bottom,right"`
     */
    private fun makeStripeButtonPaddingThemeable() {
        val padding = UIManager.getInsets("Stripe.Button.padding")
        if (padding is Insets) {
            val cp = ClassPool(true)
            val ctClass = cp["com.intellij.openapi.wm.impl.StripeButton"]
            ctClass.constructors[0].instrument(
                object : ExprEditor() {
                    override fun edit(m: MethodCall) {
                        if (m.methodName == "setBorder") {
                            m.replace(
                                """
                                    {
                                        $1 = com.intellij.util.ui.JBUI.Borders.empty(
                                            ${padding.top},
                                            ${padding.left},
                                            ${padding.bottom},
                                            ${padding.right}
                                        );
                                        ${'$'}proceed($$);
                                    }
                                """
                            )
                        }
                    }
                }
            )
            ctClass.toClass()
        }
    }

    /**
     * Makes the height of ToolWindowHeaders themeable through `ToolWindow.Header.height`.
     * @see com.intellij.openapi.wm.impl.ToolWindowHeader.getPreferredSize
     */
    private fun makeToolWindowHeaderHeightThemeable() {
        val height = UIManager.get("ToolWindow.Header.height")
        if (height is Int) {
            val ctClass = ClassPool(true)["com.intellij.openapi.wm.impl.ToolWindowHeader"]
            ctClass.getDeclaredMethod("getPreferredSize").setBody(
                """
                    {
                        return new java.awt.Dimension(
                            super.getPreferredSize().width,
                            com.intellij.util.ui.JBUI.scale($height)
                        );
                    }
                """.trimIndent()
            )
            ctClass.toClass()
        }
    }

    /**
     * Makes the editor tabs height themeable through `EditorTabs.height`.
     * @see com.intellij.ui.tabs.impl.SingleHeightTabs.SingleHeightLabel.getPreferredHeight
     */
    private fun makeSingleHeightTabsHeightThemeable() {
        val height = UIManager.get("EditorTabs.height")
        if (height is Int) {
            val ctClass = ClassPool(true)["com.intellij.ui.tabs.impl.SingleHeightTabs\$SingleHeightLabel"]
            ctClass.getDeclaredMethod("getPreferredHeight")
                .setBody("{ return com.intellij.util.ui.JBUI.scale($height); }")
            ctClass.toClass()
        }
    }

    /**
     * Makes the tree row height themeable through `Tree.height` because `Tree.rowHeight`
     * is ignored in themes right now. Please fix this @JetBrains
     */
    private fun makeTreeRowHeightThemeable() {
        val height = UIManager.get("Tree.height")
        if (height is Int) {
            UIManager.put("Tree.rowHeight", JBUI.scale(height))
        }
    }

    /**
     * Makes the scrollbar thumb arc themeable through `ScrollBar.thumbArc`.
     * Also adds some slight padding by default.
     *
     * @see com.intellij.ui.components.ScrollBarPainter.Thumb.paint
     */
    private fun makeScrollBarThumbArcThemeable() {
        if (SystemInfoRt.isMac) return
        val arc = UIManager.get("ScrollBar.thumbArc")
        if (arc is Int) {
            val ctClass = ClassPool(true)["com.intellij.ui.components.ScrollBarPainter\$Thumb"]
            ctClass.getDeclaredMethod("paint").instrument(
                object : ExprEditor() {
                    override fun edit(m: MethodCall) {
                        if (m.methodName == "paint") {
                            m.replace(
                                """
                                    {
                                        $2 += 1; $3 += 1; $4 -= 2; $5 -= 2;
                                        $6 = com.intellij.util.ui.JBUI.scale($arc);
                                        ${'$'}proceed($$);
                                    }
                                """.trimIndent()
                            )
                        }
                    }
                }
            )
            ctClass.toClass()
        }
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
