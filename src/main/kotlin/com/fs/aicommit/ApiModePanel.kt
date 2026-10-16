package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ApiModePanel {
    val panel = JPanel(BorderLayout(0, 6))
    private val model = ApiConfigurationModel()
    private val tableModel = object : DefaultTableModel(arrayOf(
        tr("aicommit.api.table.name"), tr("aicommit.api.table.provider"), tr("aicommit.api.table.url"), tr("aicommit.api.table.model")
    ), 0) { override fun isCellEditable(row: Int, column: Int) = false }
    private val table = JTable(tableModel).apply { setSelectionMode(ListSelectionModel.SINGLE_SELECTION) }
    private val activeCombo = JComboBox<String>()
    private val status = JBLabel(" ")
    private val refreshModelsButton = JButton(tr("aicommit.api.refresh.models"), AllIcons.Actions.Refresh).apply {
        isEnabled = false
        toolTipText = tr("tooltip.refresh.models")
    }
    private var refreshing = false
    private var modelRefreshRunning = false

    init {
        val actions = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        actions.add(JButton(AllIcons.General.Add).apply {
            toolTipText = tr("aicommit.api.add")
            addActionListener { edit(null) }
        })
        actions.add(JButton(AllIcons.Actions.Edit).apply {
            toolTipText = tr("aicommit.api.edit")
            addActionListener { selected()?.let { edit(it) } }
        })
        actions.add(JButton(AllIcons.General.Remove).apply {
            toolTipText = tr("aicommit.api.remove")
            addActionListener { selected()?.let { this@ApiModePanel.model.remove(it.id); refresh() } }
        })
        actions.add(JButton(tr("aicommit.button.test")).apply { addActionListener { testConnection() } })
        refreshModelsButton.addActionListener { refreshSelectedModels() }
        actions.add(refreshModelsButton)
        actions.add(JSeparator(SwingConstants.VERTICAL).apply { preferredSize = Dimension(1, 24) })
        actions.add(JButton(tr("aicommit.api.import")).apply { addActionListener { importSites() } })
        actions.add(JButton(tr("aicommit.api.export")).apply { addActionListener { exportSites() } })
        table.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) { if (event.clickCount == 2) selected()?.let { edit(it) } }
        })
        table.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) updateRefreshModelsButton()
        }
        activeCombo.addActionListener {
            if (!refreshing) model.activeId = model.configs.getOrNull(activeCombo.selectedIndex)?.id.orEmpty()
        }
        val active = JPanel(BorderLayout(8, 0)).apply {
            add(JBLabel(tr("aicommit.api.active")), BorderLayout.WEST)
            add(activeCombo, BorderLayout.CENTER)
        }
        panel.add(JPanel(BorderLayout(0, 6)).apply { add(actions, BorderLayout.NORTH); add(active, BorderLayout.SOUTH) }, BorderLayout.NORTH)
        panel.add(JBScrollPane(table).apply { preferredSize = Dimension(540, 150) }, BorderLayout.CENTER)
        panel.add(status, BorderLayout.SOUTH)
    }

    private fun selected() = model.configs.getOrNull(table.selectedRow)
    private fun edit(config: ApiProviderConfig?) {
        val dialog = ApiConfigDialog(config ?: ApiProviderConfig())
        if (dialog.showAndGet()) { model.put(dialog.config()); refresh() }
    }

    private fun refresh() {
        val selectedId = selected()?.id ?: model.activeId
        refreshing = true
        tableModel.rowCount = 0
        model.configs.forEach { tableModel.addRow(arrayOf(it.name, tr("apiconfig.provider.${it.provider}"), it.baseUrl, it.model)) }
        activeCombo.model = DefaultComboBoxModel(model.configs.map { it.name }.toTypedArray())
        activeCombo.selectedIndex = model.configs.indexOfFirst { it.id == model.activeId }
        if (table.rowCount > 0) {
            val row = model.configs.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            table.setRowSelectionInterval(row, row)
        }
        refreshing = false
        updateRefreshModelsButton()
    }

    private fun updateRefreshModelsButton() {
        refreshModelsButton.isEnabled = selected() != null && !modelRefreshRunning
    }

    private fun refreshSelectedModels() {
        if (modelRefreshRunning) return
        val source = selected()?.deepCopy() ?: return
        val snapshot = source
        val label = source.name.ifBlank { source.baseUrl }
        modelRefreshRunning = true
        updateRefreshModelsButton()
        status.text = tr("message.model.refreshing.site", label)
        status.toolTipText = null
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null,
            tr("message.model.refreshing.site", label),
            true,
        ) {
            private var update: ApiModelCacheUpdate? = null

            override fun run(indicator: ProgressIndicator) {
                val resolved = ApiCredentials.resolve(snapshot)
                val fetched = ApiClient().fetchModels(resolved) { indicator.checkCanceled() }
                update = ApiModelCache.createUpdate(resolved, fetched, System.currentTimeMillis())
            }

            override fun onSuccess() {
                val candidate = update
                status.text = when {
                    candidate == null -> tr("message.model.retained")
                    model.applyModelCache(candidate) -> {
                        refresh()
                        status.toolTipText = null
                        val time = CACHE_TIME_FORMAT.format(
                            Instant.ofEpochMilli(candidate.fetchedAt).atZone(ZoneId.systemDefault())
                        )
                        val cachedCount = model.configs.firstOrNull { it.id == source.id }
                            ?.availableModels()
                            ?.size
                            ?: candidate.remoteModels.size
                        tr("message.model.refreshed.site", label, cachedCount, time)
                    }
                    else -> tr("message.model.discarded")
                }
            }

            override fun onCancel() {
                status.text = tr("message.model.retained")
                status.toolTipText = null
            }

            override fun onThrowable(error: Throwable) {
                status.text = tr("message.model.failed.retained")
                status.toolTipText = error.message
                Messages.showErrorDialog(
                    panel,
                    tr("message.model.failed", error.message.orEmpty()),
                    tr("message.error"),
                )
            }

            override fun onFinished() {
                modelRefreshRunning = false
                updateRefreshModelsButton()
            }
        })
    }

    private fun testConnection() {
        val config = (selected() ?: model.configs.firstOrNull { it.id == model.activeId })?.deepCopy() ?: return
        status.text = tr("message.testing")
        ProgressManager.getInstance().run(object : Task.Modal(null, tr("message.testing"), true) {
            init { setCancelText(tr("dialog.button.cancel")) }
            override fun run(indicator: ProgressIndicator) {
                ApiClient().complete(config, "Reply with OK only.") { indicator.checkCanceled() }
            }
            override fun onSuccess() { status.text = tr("message.test.success") }
            override fun onThrowable(error: Throwable) {
                status.text = tr("message.error")
                status.toolTipText = error.message
                Messages.showDialog(error.message.orEmpty(), tr("message.error"),
                    arrayOf(tr("dialog.button.ok")), 0, Messages.getErrorIcon())
            }
            override fun onCancel() { status.text = " " }
        })
    }

    private fun exportSites() {
        val dialog = ApiSiteExportDialog()
        if (!dialog.showAndGet()) return
        if (dialog.includeApiKeys) {
            val confirmation = Messages.showYesNoDialog(
                panel,
                tr("aicommit.api.export.keys.warning"),
                tr("aicommit.api.export.keys.confirm"),
                Messages.getWarningIcon()
            )
            if (confirmation != Messages.YES) return
        }
        try {
            val descriptor = FileSaverDescriptor(
                tr("aicommit.api.export.title"),
                tr("aicommit.api.export.title"),
                "json"
            )
            val wrapper = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, panel)
                .save(null as VirtualFile?, "generate-commit-message-sites.json") ?: return
            Files.writeString(wrapper.file.toPath(), model.exportJson(dialog.includeApiKeys), StandardCharsets.UTF_8)
            Messages.showInfoMessage(panel, tr("aicommit.api.export.success", wrapper.file.path), tr("message.success"))
        } catch (error: Exception) {
            Messages.showErrorDialog(panel, tr("aicommit.api.transfer.error", error.message.orEmpty()), tr("message.error"))
        }
    }

    private fun importSites() {
        try {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("json").apply {
                title = tr("aicommit.api.import.title")
                description = tr("aicommit.api.import.title")
            }
            val file = FileChooser.chooseFile(descriptor, null, null) ?: return
            val document = ApiSiteTransfer.decode(String(file.contentsToByteArray(), StandardCharsets.UTF_8))
            val result = model.importSites(document)
            refresh()
            Messages.showInfoMessage(
                panel,
                tr("aicommit.api.import.success", result.added, result.overwritten, result.importedKeys, result.preservedKeys),
                tr("message.success")
            )
        } catch (error: Exception) {
            Messages.showErrorDialog(panel, tr("aicommit.api.transfer.error", error.message.orEmpty()), tr("message.error"))
        }
    }

    fun isModified(settings: AiCommitSettings) = model.isModified(settings)
    fun apply(settings: AiCommitSettings) = model.apply(settings)
    fun reset(settings: AiCommitSettings) { model.reset(settings); refresh() }
    fun hasActiveConfig() = model.configs.any { it.id == model.activeId }

    companion object {
        private val CACHE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    }
}
