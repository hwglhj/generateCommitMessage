package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

internal class ApiSiteExportDialog : DialogWrapper(true) {
    private val includeKeysBox = JBCheckBox(tr("aicommit.api.export.keys"), false)

    val includeApiKeys: Boolean
        get() = includeKeysBox.isSelected

    init {
        title = tr("aicommit.api.export.title")
        setOKButtonText(tr("aicommit.api.export"))
        setCancelButtonText(tr("dialog.button.cancel"))
        init()
    }

    override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
        .addComponent(includeKeysBox)
        .addComponent(JBLabel(tr("aicommit.api.export.keys.warning")))
        .panel
}
