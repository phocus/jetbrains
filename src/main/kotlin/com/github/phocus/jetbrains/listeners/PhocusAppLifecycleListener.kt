package com.github.phocus.jetbrains.listeners

import com.intellij.ide.AppLifecycleListener
import javassist.*

object PhocusAppLifecycleListener : AppLifecycleListener {

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        disableInternalDecoratorBorder()
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
}