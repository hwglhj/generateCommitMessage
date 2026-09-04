package com.fs.aicommit

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
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * "CLI + 模型 + 思考等级 + 刷新/测试"组合面板，可在设置页中复用
 * （AI Commit 与行内 AI 修改各持有一份，配置互相独立）。
 *
 * - 模型下拉可搜索输入，也可手动输入任意模型名
 * - 思考等级随 CLI 动态计算，跟随模型存储（如 "claude-opus-4-5 high"）；
 *   Gemini 的等级在模型名里，Cursor/OpenCode/Grok 不支持，此时隐藏该下拉
 * - 模型列表缓存跨面板共享（同一 CLI 只拉取一次）
 */
class ProviderModelPanel(title: String) {

    private val providerCombo = ComboBox(DefaultComboBoxModel(PROVIDERS.map { it.second }.toTypedArray()))
    private val modelCombo = ComboBox<String>().apply {
        isEditable = true
        preferredSize = Dimension(JBUI.scale(300), preferredSize.height)
    }
    private val thinkingCombo = ComboBox<String>().apply {
        preferredSize = Dimension(JBUI.scale(160), preferredSize.height)
    }
    private val thinkingLabel = JBLabel("思考等级:")
    private val statusLabel = JBLabel("")

    /** 每个 provider 会话内记住的模型与思考等级（内存缓存） */
    private val selectedModels = mutableMapOf<String, String>()
    private val selectedThinking = mutableMapOf<String, String>()

    /** 过滤重建下拉内容时抑制 DocumentListener 递归 */
    private var suppressFilter = false

    private var previousProvider: String = AiCommitSettings.PROVIDER_CLAUDE

    /** 正在后台拉取中的 provider，避免快速切换时重复请求 */
    private val loadingProviders = mutableSetOf<String>()

    val panel: JPanel

    init {
        installSearchFilter()

        providerCombo.addActionListener {
            // 切换 CLI 前先记住当前输入，再载入目标 CLI 的模型与思考等级
            rememberCurrent(previousProvider)
            previousProvider = selectedProvider()
            applyModelsToCombo(selectedProvider())
            applyThinkingToCombo(selectedProvider())
            ensureModelsLoadedAsync(selectedProvider())
        }

        val refreshButton = JButton("刷新模型列表").apply {
            toolTipText = "刷新所选 CLI 的可用模型列表"
            addActionListener { fetchModels() }
        }
        val testButton = JButton("测试").apply {
            toolTipText = "真实调用一次 CLI，验证登录凭证与所选模型可用"
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
                    .addLabeledComponent(JBLabel("CLI:"), providerCombo)
                    .addLabeledComponent(
                        JBLabel("模型:").apply {
                            toolTipText = "支持直接输入搜索过滤，也可手动输入任意模型名；留空使用所选 CLI 的默认模型"
                        },
                        modelCombo
                    )
                    .addLabeledComponent(
                        thinkingLabel.apply {
                            toolTipText = "随模型存储（如 \"xxx high\"）；Gemini 的思考等级在模型名里，Cursor/Grok 不支持"
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
        val provider = selectedProvider()
        val base = selectedModels[provider].orEmpty()
        val thinking = selectedThinking[provider].orEmpty()
        return if (base.isNotBlank() && thinking.isNotBlank()) "$base $thinking" else base
    }

    fun isModified(provider: String, model: String): Boolean =
        selectedProvider() != provider || composedModel() != model

    fun reset(provider: String, model: String) {
        val sdk = sdkOf(provider)
        // 存储形态为 "模型名 [思考等级]"，拆开填充两个下拉
        val (base, thinking) = SdkDefinition.splitModelThinking(sdk, model)
        selectedModels[provider] = base
        selectedThinking[provider] = thinking
        providerCombo.selectedItem = PROVIDERS.firstOrNull { it.first == provider }?.second ?: PROVIDERS.first().second
        previousProvider = selectedProvider()
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
                SwingUtilities.invokeLater { filterPopup(editor.text) }
            }
        })
    }

    private fun filterPopup(input: String) {
        if (suppressFilter) return
        val all = sharedModelCache[selectedProvider()] ?: return
        val keyword = input.trim()
        val filtered = if (keyword.isEmpty()) all
        else all.filter { it.contains(keyword, ignoreCase = true) }

        suppressFilter = true
        try {
            val model = DefaultComboBoxModel(filtered.toTypedArray())
            modelCombo.model = model
            // 恢复用户正在输入的文本（setModel 会清掉编辑框）
            modelCombo.editor.item = keyword
            (modelCombo.editor.editorComponent as? JTextField)?.caretPosition = keyword.length
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
        val text = currentModelText()
        if (text.isNotEmpty()) selectedModels[provider] = text
        selectedThinking[provider] = currentThinking()
    }

    /** 把指定 provider 的候选列表和已选模型写入下拉框 */
    private fun applyModelsToCombo(provider: String) {
        val sdk = sdkOf(provider)
        val list = sharedModelCache[provider] ?: listOf(sdk.defaultModel).filter { it.isNotBlank() }
        val selected = selectedModels[provider]?.ifBlank { null } ?: sdk.defaultModel
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
        val remembered = selectedThinking[provider].orEmpty()
        thinkingCombo.selectedItem = if (remembered in sdk.thinkingLevels) remembered else THINKING_DEFAULT_LABEL
    }

    /** 后台刷新：每次切换供应商都重新拉取模型列表（先显示缓存，拉取完成后更新） */
    private fun ensureModelsLoadedAsync(provider: String) {
        if (!loadingProviders.add(provider)) return
        statusLabel.text = "正在拉取模型列表..."
        ApplicationManager.getApplication().executeOnPooledThread {
            val models = SdkManager.fetchModels(sdkOf(provider))
            // 用纯 Swing 事件队列回到 EDT：设置对话框是模态窗口，
            // Application.invokeLater 会按 ModalityState 排队，回调可能被推迟到对话框关闭，
            // 状态就一直卡在"正在拉取"；这里只更新本面板的 Swing 组件，无需平台模态语义
            SwingUtilities.invokeLater {
                loadingProviders.remove(provider)
                if (models.isNotEmpty()) sharedModelCache[provider] = models
                if (provider == selectedProvider()) {
                    statusLabel.text = if (models.isNotEmpty()) "已加载 ${models.size} 个模型" else "拉取模型列表失败，保留原有列表"
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
        ProgressManager.getInstance().run(object : Task.Modal(null, "正在刷新模型列表...", false) {
            var models: List<String> = emptyList()
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                models = SdkManager.fetchModels(sdkOf(provider))
            }

            override fun onSuccess() {
                sharedModelCache[provider] = models
                statusLabel.text = "已加载 ${models.size} 个模型"
                rememberCurrent(provider)
                applyModelsToCombo(provider)
            }
        })
    }

    private fun testCurrent() {
        val provider = selectedProvider()
        val sdk = sdkOf(provider)
        val model = composedModel().ifBlank { sdk.defaultModel }
        ProgressManager.getInstance().run(object : Task.Modal(null, "正在测试 ${sdk.displayName}（$model）...", false) {
            var error: String? = null
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                error = AiCommitService(null).testSdk(sdk, model)
            }

            override fun onSuccess() {
                if (error == null) {
                    Messages.showInfoMessage("${sdk.displayName} 调用成功，模型 $model 可用。", "测试")
                } else {
                    Messages.showErrorDialog("测试失败: $error", "测试")
                }
            }
        })
    }

    companion object {
        private const val THINKING_DEFAULT_LABEL = "跟随默认"

        /** provider id -> 设置页展示标签 */
        private val PROVIDERS = listOf(
            AiCommitSettings.PROVIDER_CLAUDE to "Claude",
            AiCommitSettings.PROVIDER_CODEX to "Codex",
            AiCommitSettings.PROVIDER_CURSOR to "Cursor",
            AiCommitSettings.PROVIDER_OPENCODE to "OpenCode",
            AiCommitSettings.PROVIDER_GEMINI to "Gemini (agy)",
            AiCommitSettings.PROVIDER_GROK to "Grok",
            AiCommitSettings.PROVIDER_OMP to "OMP"
        )

        /** 模型列表缓存：跨面板共享，同一 CLI 只拉取一次 */
        private val sharedModelCache = mutableMapOf<String, List<String>>()
    }
}
