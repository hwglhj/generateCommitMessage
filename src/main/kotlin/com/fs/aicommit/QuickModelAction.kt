package com.fs.aicommit

import com.fs.i18n.SubMsgBundle
import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.awt.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JPanel
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

        internal fun showPopup(project: Project, anchor: Component?, afterFailure: Boolean = false) {
            if (project.isDisposed || CommitGenerationTask.gate.isRunning) return
            val settings = AiCommitSettings.getInstance()
            SubMsgBundle.language = settings.interfaceLanguage
            val group = DefaultActionGroup()
            var popup: QuickModelActionPopup? = null
            val hasChoices = if (settings.mode == AiCommitSettings.MODE_API) {
                val sites = apiSites(settings)
                sites.forEach { site ->
                    lateinit var siteGroup: LivePopupActionGroup
                    siteGroup = LivePopupActionGroup(siteMenuLabel(site)) {
                        val currentSite = apiSites(settings).firstOrNull { it.configId == site.configId }
                        buildList<AnAction> {
                            add(ApiSiteRefreshAction(site.configId, project) {
                                val handle = popup?.openSubmenuHandle() ?: QuickModelPopupRefreshHandle.NONE
                                refreshApiSite(project, site.configId, siteGroup, handle)
                            })
                            add(Separator.getInstance())
                            if (currentSite == null || currentSite.choices.isEmpty()) {
                                add(disabledAction(tr("action.quick.model.empty.site")))
                            } else {
                                currentSite.choices.forEach { choice -> add(modelToggle(settings, project, choice)) }
                            }
                            val config = settings.apiConfigs.firstOrNull { it.id == site.configId }
                            if (config != null) {
                                add(Separator.getInstance())
                                add(disabledAction(siteStatus(config)))
                            }
                        }.toTypedArray()
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
            val contextComponent = anchor ?: WindowManager.getInstance().getFrame(project)?.rootPane ?: JPanel()
            popup = QuickModelActionPopup(title, group, quickModelPopupDataContext(project, contextComponent))
            popup.setAdText(tr("action.quick.model.hint"), SwingConstants.LEFT)
            if (anchor?.isShowing == true) popup.showUnderneathOf(anchor)
            else popup.showCenteredInCurrentWindow(project)
        }

        private fun refreshApiSite(
            project: Project,
            configId: String,
            siteGroup: ActionGroup,
            popupHandle: QuickModelPopupRefreshHandle,
        ) {
            if (project.isDisposed || !beginApiRefresh(configId)) return
            val settings = AiCommitSettings.getInstance()
            val source = settings.apiConfigs.firstOrNull { it.id == configId }
            if (source == null) {
                endApiRefresh(configId)
                return
            }
            val snapshot = source.deepCopy()
            val label = source.name.ifBlank { source.baseUrl }
            apiRefreshMessages[configId] = tr("message.model.refreshing.site", label)
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
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
                    when {
                        candidate == null -> apiRefreshMessages[configId] = tr("message.model.retained")
                        ApiModelCache.apply(settings.apiConfigs, candidate) -> apiRefreshMessages.remove(configId)
                        else -> apiRefreshMessages[configId] = tr("message.model.discarded")
                    }
                }

                override fun onCancel() {
                    apiRefreshMessages[configId] = tr("message.model.retained")
                }

                override fun onThrowable(error: Throwable) {
                    LOG.warn("Failed to refresh API models for $configId: ${error.message}")
                    apiRefreshMessages[configId] = tr("message.model.failed.retained")
                }

                override fun onFinished() {
                    endApiRefresh(configId)
                    if (!project.isDisposed) popupHandle.replace(siteGroup)
                }
            })
        }

        private fun cacheStatus(config: ApiProviderConfig): String {
            val count = config.availableModels().size
            if (count == 0) return tr("action.quick.model.cache.empty")
            if (config.modelsFetchedAt <= 0L) return tr("action.quick.model.cache.unknown", count)
            val time = CACHE_TIME_FORMAT.format(
                Instant.ofEpochMilli(config.modelsFetchedAt).atZone(ZoneId.systemDefault())
            )
            return tr("action.quick.model.cache", count, time)
        }

        private fun siteStatus(config: ApiProviderConfig): String {
            val cache = cacheStatus(config)
            return apiRefreshMessages[config.id]?.let { "$it · $cache" } ?: cache
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

        internal fun beginApiRefresh(configId: String): Boolean = apiRefreshRunning.add(configId)

        internal fun endApiRefresh(configId: String) {
            apiRefreshRunning.remove(configId)
        }

        internal fun isApiRefreshRunning(configId: String): Boolean = configId in apiRefreshRunning

        private val apiRefreshRunning = ConcurrentHashMap.newKeySet<String>()
        private val apiRefreshMessages = ConcurrentHashMap<String, String>()
        private val CACHE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
        private val LOG = Logger.getInstance(QuickModelAction::class.java)

    }
}
