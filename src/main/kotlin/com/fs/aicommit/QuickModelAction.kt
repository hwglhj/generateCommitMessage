package com.fs.aicommit

import com.fs.i18n.SubMsgBundle
import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingConstants

/** 提交工具栏中的快速模型选择入口。 */
class QuickModelAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val settings = AiCommitSettings.getInstance()
        SubMsgBundle.language = settings.interfaceLanguage
        e.presentation.text = tr("action.quick.model")
        e.presentation.description = if (CommitGenerationTask.gate.isRunning) tr("action.quick.model.busy") else tr("action.quick.model.desc")
        e.presentation.isVisible = e.project != null
        e.presentation.isEnabled = e.project != null && !CommitGenerationTask.gate.isRunning
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        showPopup(project, e.inputEvent?.component ?: e.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT))
    }

    companion object {
        internal data class Choice(
            val mode: String,
            val provider: String,
            val configId: String,
            val model: String,
            val label: String,
            val active: Boolean,
        )

        internal data class ApiSiteChoices(
            val configId: String,
            val label: String,
            val currentModel: String,
            val active: Boolean,
            val choices: List<Choice>,
        )

        internal data class ApiRefreshResult(
            val modelsByConfigId: Map<String, List<String>>,
            val failed: Int,
        )

        internal fun refreshApiModels(
            settings: AiCommitSettings,
            fetch: (ApiProviderConfig) -> List<String>
        ): Int {
            if (settings.mode != AiCommitSettings.MODE_API) return 0
            val result = loadApiModels(settings.apiConfigs, fetch)
            applyApiModels(settings, result)
            return result.failed
        }

        internal fun loadApiModels(
            configs: List<ApiProviderConfig>,
            fetch: (ApiProviderConfig) -> List<String>
        ): ApiRefreshResult {
            var failed = 0
            val modelsByConfigId = linkedMapOf<String, List<String>>()
            configs.forEach { config ->
                try {
                    val fetched = fetch(config.deepCopy())
                    modelsByConfigId[config.id] = sequenceOf(config.model)
                        .plus(fetched.asSequence())
                        .plus(config.models.asSequence())
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .toList()
                } catch (error: ProcessCanceledException) {
                    throw error
                } catch (_: Exception) {
                    failed++
                }
            }
            return ApiRefreshResult(modelsByConfigId, failed)
        }

        internal fun applyApiModels(settings: AiCommitSettings, result: ApiRefreshResult) {
            settings.apiConfigs.forEach { config ->
                result.modelsByConfigId[config.id]?.let { config.models = it.toMutableList() }
            }
        }

        internal fun apiSites(settings: AiCommitSettings): List<ApiSiteChoices> =
            settings.apiConfigs.map { config ->
                val siteLabel = config.name.ifBlank { config.baseUrl }
                ApiSiteChoices(
                    configId = config.id,
                    label = siteLabel,
                    currentModel = config.model.trim(),
                    active = config.id == settings.activeApiConfigId,
                    choices = config.availableModels().map { model ->
                        Choice(
                            mode = AiCommitSettings.MODE_API,
                            provider = config.provider,
                            configId = config.id,
                            model = model,
                            label = model,
                            active = config.id == settings.activeApiConfigId && model == config.model.trim(),
                        )
                    }
                )
            }

        internal fun siteMenuLabel(site: ApiSiteChoices): String =
            if (site.active) "✓ ${site.label} — ${site.currentModel.ifBlank { tr("action.quick.model.default") }}"
            else site.label

        internal fun choices(settings: AiCommitSettings, cliCandidates: List<String> = SdkManager.quickModelCandidates(settings.activeSdk())): List<Choice> {
            if (settings.mode == AiCommitSettings.MODE_API) {
                return apiSites(settings).flatMap { site ->
                    site.choices.map { choice ->
                        choice.copy(label = "${site.label} — ${choice.label.ifBlank { tr("action.quick.model.default") }}")
                    }
                }
            }

            val sdk = settings.activeSdk()
            val currentRaw = settings.model.trim().ifBlank { sdk.defaultModel }
            val (currentModel, thinking) = SdkDefinition.splitModelThinking(sdk, currentRaw)
            val models = sequenceOf(currentModel, *cliCandidates.toTypedArray())
                .map { it.trim() }
                .filter { it.isNotBlank() || currentRaw.isBlank() }
                .map { candidate ->
                    if (thinking.isNotBlank() && SdkDefinition.splitModelThinking(sdk, candidate).second.isBlank()) "$candidate $thinking" else candidate
                }
                .distinct()
                .toList()
            return models.map { model ->
                Choice(
                    mode = AiCommitSettings.MODE_CLI,
                    provider = settings.activeProvider,
                    configId = "",
                    model = model,
                    label = model.ifBlank { tr("action.quick.model.default") },
                    active = model == currentRaw,
                )
            }
        }

        internal fun select(settings: AiCommitSettings, choice: Choice): Boolean {
            if (CommitGenerationTask.gate.isRunning) return false
            if (choice.mode == AiCommitSettings.MODE_API) {
                val config = settings.apiConfigs.firstOrNull { it.id == choice.configId }
                if (settings.mode != AiCommitSettings.MODE_API || config == null || choice.model !in config.availableModels()) return false
                settings.activeApiConfigId = choice.configId
                config.model = choice.model
                return true
            }
            if (settings.mode != AiCommitSettings.MODE_CLI || settings.activeProvider != choice.provider) return false
            settings.model = choice.model
            return true
        }

        internal fun showPopup(project: Project, anchor: Component?, afterFailure: Boolean = false, refreshApi: Boolean = true) {
            if (project.isDisposed || CommitGenerationTask.gate.isRunning) return
            val settings = AiCommitSettings.getInstance()
            SubMsgBundle.language = settings.interfaceLanguage
            if (settings.mode == AiCommitSettings.MODE_API && refreshApi) {
                if (!apiRefreshRunning.compareAndSet(false, true)) return
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, tr("message.model.loading"), true) {
                    private var result = ApiRefreshResult(emptyMap(), 0)

                    override fun run(indicator: ProgressIndicator) {
                        val snapshots = settings.apiConfigs.map { ApiCredentials.resolve(it.deepCopy()) }
                        result = loadApiModels(snapshots) { config ->
                            indicator.text2 = config.name.ifBlank { config.baseUrl }
                            ApiClient().fetchModels(ApiCredentials.resolve(config)) { indicator.checkCanceled() }
                        }
                    }

                    override fun onSuccess() {
                        apiRefreshRunning.set(false)
                        applyApiModels(settings, result)
                        showPopup(project, anchor, afterFailure, refreshApi = false)
                    }

                    override fun onCancel() {
                        apiRefreshRunning.set(false)
                        showPopup(project, anchor, afterFailure, refreshApi = false)
                    }

                    override fun onThrowable(error: Throwable) {
                        apiRefreshRunning.set(false)
                        showPopup(project, anchor, afterFailure, refreshApi = false)
                    }
                })
                return
            }
            val group = DefaultActionGroup()
            val hasChoices = if (settings.mode == AiCommitSettings.MODE_API) {
                val sites = apiSites(settings)
                sites.forEach { site ->
                    val siteGroup = DefaultActionGroup(siteMenuLabel(site), true)
                    if (site.choices.isEmpty()) {
                        siteGroup.add(disabledAction(tr("action.quick.model.empty.site")))
                    } else {
                        site.choices.forEach { choice -> siteGroup.add(modelToggle(settings, project, choice)) }
                    }
                    group.add(siteGroup)
                }
                sites.any { it.choices.isNotEmpty() }
            } else {
                val cliChoices = choices(settings)
                cliChoices.forEach { choice -> group.add(modelToggle(settings, project, choice)) }
                cliChoices.isNotEmpty()
            }
            if (!hasChoices && group.childActionsOrStubs.isEmpty()) {
                group.add(object : AnAction(tr("action.quick.model.empty")), DumbAware {
                    override fun actionPerformed(event: AnActionEvent) = Unit
                    override fun update(event: AnActionEvent) { event.presentation.isEnabled = false }
                })
            }
            group.addSeparator()
            group.add(object : AnAction(tr("action.quick.model.settings")), DumbAware {
                override fun actionPerformed(event: AnActionEvent) {
                    if (!project.isDisposed) ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, GenerateCommitMessageConfigurable::class.java)
                }
            })
            val title = if (afterFailure) tr("action.quick.model.failure.title") else tr("action.quick.model.title")
            val popup = JBPopupFactory.getInstance().createActionGroupPopup(
                title, group, SimpleDataContext.getProjectContext(project),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true,
            )
            popup.setAdText(tr("action.quick.model.hint"), SwingConstants.LEFT)
            if (anchor?.isShowing == true) popup.showUnderneathOf(anchor)
            else popup.showCenteredInCurrentWindow(project)
        }

        private fun disabledAction(label: String): AnAction = object : AnAction(label), DumbAware {
            override fun actionPerformed(event: AnActionEvent) = Unit
            override fun update(event: AnActionEvent) { event.presentation.isEnabled = false }
        }

        private fun modelToggle(settings: AiCommitSettings, project: Project, choice: Choice): ToggleAction =
            object : ToggleAction(choice.label), DumbAware {
                override fun isSelected(event: AnActionEvent): Boolean =
                    if (choice.mode == AiCommitSettings.MODE_API) {
                        val config = settings.apiConfigs.firstOrNull { it.id == choice.configId }
                        settings.mode == choice.mode && settings.activeApiConfigId == choice.configId &&
                            config?.model?.trim() == choice.model
                    } else {
                        settings.mode == choice.mode && settings.activeProvider == choice.provider &&
                            settings.modelFor(settings.activeSdk()) == choice.model
                    }

                override fun setSelected(event: AnActionEvent, state: Boolean) {
                    if (!project.isDisposed) select(settings, choice)
                }

                override fun update(event: AnActionEvent) {
                    super.update(event)
                    event.presentation.isEnabled = !project.isDisposed && !CommitGenerationTask.gate.isRunning
                }
            }

        private val apiRefreshRunning = AtomicBoolean(false)

    }
}
