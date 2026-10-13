package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * "CLI + 模型 + 思考等级 + 刷新/测试"组合面板，可在设置页中复用
 * 用于 AI Commit 的 CLI 配置。
 *
 * - 模型下拉可搜索输入，也可手动输入任意模型名
 * - 思考等级随 CLI 动态计算，跟随模型存储（如 "claude-opus-4-5 high"）；
 *   Gemini 的等级在模型名里，Cursor/OpenCode/Grok 不支持，此时隐藏该下拉
 * - 模型列表缓存跨面板共享（同一 CLI 只拉取一次）
 */
class ProviderModelPanel(title: String, private val cliPath: () -> String = { "" }) {

    private val providerCombo = ComboBox(DefaultComboBoxModel(PROVIDERS.map { it.second }.toTypedArray()))
    private val modelCombo = ComboBox<String>().apply {
        isEditable = true
        preferredSize = Dimension(JBUI.scale(300), preferredSize.height)
    }
    private val thinkingCombo = ComboBox<String>().apply {
        preferredSize = Dimension(JBUI.scale(160), preferredSize.height)
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focus: Boolean): java.awt.Component =
                super.getListCellRendererComponent(list, if (value is String && value.isNotEmpty()) tr("aicommit.thinking.$value") else tr("aicommit.thinking.default"), index, selected, focus)
        }
    }
    private val thinkingLabel = JBLabel(tr("aicommit.thinking.label"))
    private val statusLabel = JBLabel("")
    private var disposed = false

    /** 每个 provider 会话内记住的模型与思考等级（内存缓存） */
    private val selection = ProviderModelSelection()
    private var resetting = false

    /** 过滤重建下拉内容时抑制 DocumentListener 递归 */
    private var suppressFilter = false

    private var previousProvider: String = AiCommitSettings.PROVIDER_CLAUDE

    /** 正在后台拉取中的 provider，避免快速切换时重复请求 */
    private val loadingProviders = mutableSetOf<String>()

    val panel: JPanel

    init {
        installSearchFilter()

        providerCombo.addActionListener {
            if (resetting) return@addActionListener
            // 切换 CLI 前先记住当前输入，再载入目标 CLI 的模型与思考等级
            rememberCurrent(previousProvider)
            previousProvider = selectedProvider()
            selection.select(previousProvider)
            applyModelsToCombo(selectedProvider())
            applyThinkingToCombo(selectedProvider())
            ensureModelsLoadedAsync(selectedProvider())
        }

        val refreshButton = JButton(tr("aicommit.button.refresh")).apply {
            toolTipText = tr("tooltip.refresh.models")
            addActionListener { fetchModels() }
        }
        val testButton = JButton(tr("aicommit.button.test")).apply {
            toolTipText = tr("tooltip.test.connection")
            addActionListener { testCurrent() }
        }
        val buttonLine = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(refreshButton)
            add(testButton)
            add(statusLabel)
        }

        panel = JPanel(BorderLayout()).apply {
            border = TitledBorder(title)
            add(
                FormBuilder.createFormBuilder()
                    .addLabeledComponent(JBLabel(tr("aicommit.cli.label")), providerCombo)
                    .addLabeledComponent(
                        JBLabel(tr("aicommit.model.label")).apply {
                            toolTipText = tr("tooltip.model.input")
                        },
                        modelCombo
                    )
                    .addLabeledComponent(
                        thinkingLabel.apply {
                            toolTipText = tr("tooltip.thinking.level")
                        },
                        thinkingCombo
                    )
                    .addComponent(buttonLine)
                    .panel,
                BorderLayout.CENTER
            )
        }
    }

    // ---------------------------------------------------------------
    // 对外：与 Configurable 生命周期对接
    // ---------------------------------------------------------------

    fun selectedProvider(): String {
        val label = providerCombo.selectedItem
        return PROVIDERS.firstOrNull { it.second == label }?.first ?: AiCommitSettings.PROVIDER_CLAUDE
    }

    /** "模型名 [思考等级]" 组合串（写入设置的最终形态），会先记住当前输入 */
    fun composedModel(): String {
        rememberCurrent(selectedProvider())
        return selection.composedModel()
    }

    fun isModified(provider: String, model: String): Boolean =
        selectedProvider() != provider || composedModel() != model

    fun dispose() { disposed = true }

    fun reset(provider: String, model: String) {
        resetting = true
        selection.reset(provider, model)
        try {
            providerCombo.selectedItem = PROVIDERS.firstOrNull { it.first == provider }?.second ?: PROVIDERS.first().second
        } finally {
            resetting = false
        }
        previousProvider = selectedProvider()
        selection.select(previousProvider)
        applyModelsToCombo(selectedProvider())
        applyThinkingToCombo(selectedProvider())
        ensureModelsLoadedAsync(selectedProvider())
    }

    // ---------------------------------------------------------------
    // 可搜索下拉框：编辑框输入即过滤下拉候选
    // ---------------------------------------------------------------

    private fun installSearchFilter() {
        val editor = modelCombo.editor.editorComponent as? JTextField ?: return
        editor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onChanged()
            override fun removeUpdate(e: DocumentEvent) = onChanged()
            override fun changedUpdate(e: DocumentEvent) = onChanged()

            private fun onChanged() {
                if (suppressFilter) return
                SwingUtilities.invokeLater { if (!disposed) filterPopup(editor.text) }
            }
        })
    }

    private fun filterPopup(input: String) {
        if (suppressFilter) return
        val all = sharedModelCache[selectedProvider()] ?: return
        val keyword = input.trim()
        val filtered = if (keyword.isEmpty()) all
        else all.filter { it.contains(keyword, ignoreCase = true) }
        val editor = modelCombo.editor.editorComponent as? JTextField
        val caretPosition = editor?.caretPosition ?: input.length

        suppressFilter = true
        try {
            val model = DefaultComboBoxModel(filtered.toTypedArray())
            modelCombo.model = model
            // 恢复用户正在输入的文本（setModel 会清掉编辑框）
            modelCombo.editor.item = input
            editor?.caretPosition = caretPosition.coerceIn(0, input.length)
            if (modelCombo.isShowing && filtered.isNotEmpty()) {
                modelCombo.hidePopup()
                modelCombo.showPopup()
            }
        } finally {
            suppressFilter = false
        }
    }

    // ---------------------------------------------------------------
    // 模型 / 思考等级加载
    // ---------------------------------------------------------------

    private fun sdkOf(provider: String): SdkDefinition = SdkDefinition.fromProvider(provider)

    private fun currentModelText(): String =
        (modelCombo.editor.item?.toString() ?: "").trim()

    private fun currentThinking(): String {
        val v = thinkingCombo.selectedItem?.toString() ?: ""
        return if (v == THINKING_DEFAULT_LABEL) "" else v
    }

    private fun rememberCurrent(provider: String) {
        selection.select(provider)
        selection.remember(currentModelText(), currentThinking())
    }

    /** 把指定 provider 的候选列表和已选模型写入下拉框 */
    private fun applyModelsToCombo(provider: String) {
        val sdk = sdkOf(provider)
        val list = sharedModelCache[provider] ?: listOf(sdk.defaultModel).filter { it.isNotBlank() }
        val selected = selection.model
        suppressFilter = true
        try {
            val model = DefaultComboBoxModel(list.toTypedArray())
            if (selected.isNotBlank() && model.getIndexOf(selected) < 0) model.insertElementAt(selected, 0)
            modelCombo.model = model
            modelCombo.selectedItem = selected
        } finally {
            suppressFilter = false
        }
    }

    /** 思考等级候选随 CLI 动态计算；不支持的 CLI（Cursor/Gemini/Grok）隐藏下拉 */
    private fun applyThinkingToCombo(provider: String) {
        val sdk = sdkOf(provider)
        val supported = sdk.thinkingLevels.isNotEmpty()
        thinkingLabel.isVisible = supported
        thinkingCombo.isVisible = supported
        if (!supported) return
        val items = arrayOf(THINKING_DEFAULT_LABEL) + sdk.thinkingLevels
        thinkingCombo.model = DefaultComboBoxModel(items)
        val remembered = selection.thinking
        thinkingCombo.selectedItem = if (remembered in sdk.thinkingLevels) remembered else THINKING_DEFAULT_LABEL
    }

    /** 后台刷新：每次切换供应商都重新拉取模型列表（先显示缓存，拉取完成后更新） */
    private fun ensureModelsLoadedAsync(provider: String) {
        if (!loadingProviders.add(provider)) return
        statusLabel.text = tr("message.model.loading")
        val executable = cliPath()
        ApplicationManager.getApplication().executeOnPooledThread {
            val models = SdkManager.fetchModels(sdkOf(provider), executable)
            // 用纯 Swing 事件队列回到 EDT：设置对话框是模态窗口，
            // Application.invokeLater 会按 ModalityState 排队，回调可能被推迟到对话框关闭，
            // 状态就一直卡在"正在拉取"；这里只更新本面板的 Swing 组件，无需平台模态语义
            SwingUtilities.invokeLater {
                if (disposed) return@invokeLater
                loadingProviders.remove(provider)
                if (models.isNotEmpty()) sharedModelCache[provider] = models
                if (provider == selectedProvider()) {
                    statusLabel.text = if (models.isNotEmpty()) tr("message.model.loaded", models.size) else tr("message.model.retained")
                    rememberCurrent(provider)
                    applyModelsToCombo(provider)
                }
            }
        }
    }

    /** 手动刷新模型列表 */
    private fun fetchModels() {
        val provider = selectedProvider()
        sharedModelCache.remove(provider)
        val executable = cliPath()
        ProgressManager.getInstance().run(object : Task.Modal(null, tr("message.model.loading"), false) {
            var models: List<String> = emptyList()
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                models = SdkManager.fetchModels(sdkOf(provider), executable)
            }

            override fun onSuccess() {
                sharedModelCache[provider] = models
                statusLabel.text = tr("message.model.loaded", models.size)
                rememberCurrent(provider)
                applyModelsToCombo(provider)
            }
        })
    }

    private fun testCurrent() {
        val provider = selectedProvider()
        val sdk = sdkOf(provider)
        val model = composedModel().ifBlank { sdk.defaultModel }
        val executable = cliPath()
        ProgressManager.getInstance().run(object : Task.Modal(null, tr("cli.testing", sdk.displayName, model), true) {
            init { setCancelText(tr("dialog.button.cancel")) }
            var error: String? = null
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                error = AiCommitService(null).testSdk(sdk, model, executable, indicator)
            }

            override fun onSuccess() {
                if (error == null) {
                    Messages.showDialog(tr("cli.test.success", sdk.displayName, model), tr("aicommit.button.test"),
                        arrayOf(tr("dialog.button.ok")), 0, Messages.getInformationIcon())
                } else {
                    Messages.showDialog(tr("message.test.failed", error), tr("aicommit.button.test"),
                        arrayOf(tr("dialog.button.ok")), 0, Messages.getErrorIcon())
                }
            }
        })
    }

    companion object {
        private const val THINKING_DEFAULT_LABEL = ""

        /** provider id -> 设置页展示标签 */
        private val PROVIDERS = listOf(
            AiCommitSettings.PROVIDER_CLAUDE to "Claude",
            AiCommitSettings.PROVIDER_CODEX to "Codex",
            AiCommitSettings.PROVIDER_CURSOR to "Cursor",
            AiCommitSettings.PROVIDER_OPENCODE to "OpenCode",
            AiCommitSettings.PROVIDER_GEMINI to "Gemini (agy)",
            AiCommitSettings.PROVIDER_GROK to "Grok",
            AiCommitSettings.PROVIDER_OMP to "OMP",
            AiCommitSettings.PROVIDER_PI to "Pi"
        )

        /** 模型列表缓存：跨面板共享，同一 CLI 只拉取一次 */
        private val sharedModelCache = mutableMapOf<String, List<String>>()

        /** 返回已加载的模型副本，不触发 CLI 或网络请求。 */
        internal fun cachedModels(provider: String): List<String> = sharedModelCache[provider]?.toList().orEmpty()
    }
}
