package com.github.phocus.jetbrains.listeners

import com.github.phocus.jetbrains.ui.NavBarUI
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.navigationToolbar.ui.NavBarUIManager
import javassist.*
import org.jetbrains.annotations.NonNls
import java.lang.reflect.Field

object PhocusAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        disableInternalDecoratorBorder()
        setNavBarHeight(34)
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
        ctClass.getDeclaredMethod("getBorderInsets")
                .setBody("{ return new java.awt.Insets(0, 0, 0, 0); }")
        ctClass.toClass()
    }

    /**
     * Sets the navbar height (breadcrumb like thing) by providing a
     * custom NavBarUI implementation instance.
     */
    private fun setNavBarHeight(height:Int) {
        overWriteFinalStaticField(NavBarUIManager::class.java, "DARCULA", NavBarUI(height))
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