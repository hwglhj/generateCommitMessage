package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*

class ApiConfigDialog(private val original: ApiProviderConfig = ApiProviderConfig()) : DialogWrapper(true) {
    private val nameField = JBTextField(original.name, 32)
    private val providerIds = listOf(ApiProviderConfig.OPENAI_COMPATIBLE, ApiProviderConfig.ANTHROPIC, ApiProviderConfig.CUSTOM)
    private val providerBox = ComboBox(arrayOf("OpenAI Compatible", "Anthropic", tr("apiconfig.provider.custom")))
    private val urlField = JBTextField(original.baseUrl, 40)
    private val keyField = JPasswordField(original.apiKey, 32)
    private var cachedModels: List<String> = original.availableModels()
    private var modelsFetchedAt: Long = original.modelsFetchedAt
    private var cachedProvider: String = original.provider.trim()
    private var cachedBaseUrl: String = original.baseUrl.trim()
    private val modelBox = ComboBox<String>().apply {
        isEditable = true
        model = DefaultComboBoxModel(cachedModels.toTypedArray())
        editor.item = original.model
    }
    private val reasoningBox = JBCheckBox(tr("apiconfig.reasoning"), original.reasoningCompatible)
    private val status = JBLabel(" ")
    private val fetchButton = JButton(tr("apiconfig.model.fetch"))

    init {
        title = tr(if (original.name.isBlank()) "apiconfig.title.add" else "apiconfig.title.edit")
        setOKButtonText(tr("dialog.button.ok"))
        setCancelButtonText(tr("dialog.button.cancel"))
        providerBox.selectedIndex = providerIds.indexOf(original.provider).coerceAtLeast(0)
        reasoningBox.isEnabled = providerBox.selectedIndex != 1
        providerBox.addActionListener {
            if (urlField.text in listOf("https://api.openai.com/v1", "https://api.anthropic.com/v1")) {
                urlField.text = if (providerBox.selectedIndex == 1) "https://api.anthropic.com/v1" else "https://api.openai.com/v1"
            }
            reasoningBox.isEnabled = providerBox.selectedIndex != 1
        }
        fetchButton.addActionListener { fetchModels() }
        init()
    }

    fun config(): ApiProviderConfig {
        val currentModel = modelBox.editor.item?.toString().orEmpty().trim()
        val provider = providerIds[providerBox.selectedIndex]
        val baseUrl = urlField.text.trim()
        val keepCache = cacheMatches(provider, baseUrl, cachedProvider, cachedBaseUrl)
        return original.deepCopy().copy(
            name = nameField.text.trim(), provider = provider,
            baseUrl = baseUrl, apiKey = String(keyField.password).trim(),
            model = currentModel,
            models = mergeCurrentWithCache(currentModel, if (keepCache) cachedModels else emptyList()).toMutableList(),
            modelsFetchedAt = if (keepCache) modelsFetchedAt else 0L,
            reasoningCompatible = reasoningBox.isSelected
        )
    }

    override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent(tr("apiconfig.name"), nameField)
        .addLabeledComponent(tr("apiconfig.provider"), providerBox)
        .addLabeledComponent(tr("apiconfig.baseurl"), urlField)
        .addLabeledComponent(tr("apiconfig.apikey"), keyField)
        .addLabeledComponent(tr("apiconfig.model"), modelBox)
        .addComponent(reasoningBox)
        .addComponent(JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply { add(fetchButton) })
        .addComponent(status).panel.apply { preferredSize = Dimension(580, preferredSize.height) }

    override fun doValidate(): ValidationInfo? = config().validationError()?.let { ValidationInfo(it) }

    private fun fetchModels() {
        val snapshot = config().apply { if (name.isBlank()) name = "API" }
        snapshot.validationError(requireModel = false)?.let { setErrorText(it); return }
        fetchButton.isEnabled = false
        status.text = tr("message.model.loading")
        ProgressManager.getInstance().run(object : Task.Modal(null, tr("message.model.loading"), true) {
            init { setCancelText(tr("dialog.button.cancel")) }
            private var models = emptyList<String>()
            override fun run(indicator: ProgressIndicator) {
                models = ApiClient().fetchModels(snapshot) { indicator.checkCanceled() }
            }
            override fun onSuccess() {
                val selected = modelBox.editor.item
                val update = ApiModelCache.createUpdate(snapshot, models, System.currentTimeMillis())
                if (update == null) {
                    status.text = tr("message.model.retained")
                    return
                }
                cachedModels = mergeCurrentWithCache(selected?.toString().orEmpty(), update.remoteModels)
                modelsFetchedAt = update.fetchedAt
                cachedProvider = snapshot.provider.trim()
                cachedBaseUrl = snapshot.baseUrl.trim()
                modelBox.model = DefaultComboBoxModel(
                    mergeCurrentWithCache(selected?.toString().orEmpty(), cachedModels).toTypedArray()
                )
                modelBox.editor.item = selected
                status.text = tr("message.model.loaded", cachedModels.size)
            }
            override fun onThrowable(error: Throwable) {
                status.text = tr("message.model.failed.retained")
                setErrorText(tr("message.model.failed", error.message.orEmpty()))
            }
            override fun onCancel() { status.text = tr("message.model.retained") }
            override fun onFinished() { fetchButton.isEnabled = true }
        })
    }

    companion object {
        internal fun mergeCurrentWithCache(current: String, cached: List<String>): List<String> =
            sequenceOf(current)
                .plus(cached.asSequence())
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .toList()

        internal fun cacheMatches(
            provider: String,
            baseUrl: String,
            cachedProvider: String,
            cachedBaseUrl: String,
        ): Boolean = provider.trim() == cachedProvider.trim() && baseUrl.trim() == cachedBaseUrl.trim()
    }
}
