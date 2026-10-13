package com.fs.aicommit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.fs.i18n.SubMsgBundle

/**
 * AI Commit 配置（应用级持久化，文件 ai-commit.xml）。
 *
 * 支持选择 Claude、Codex、Cursor、OpenCode、Gemini(agy)、Grok、OMP 或 Pi CLI；
 * 模型为单一输入，随所选 CLI 生效，可在末尾追加思考等级（如 "claude-opus-4-5 high"）。
 * CLI 模式复用本机登录，API 模式使用独立的提供商配置。
 */
@State(
    name = "AiCommitSettings",
    storages = [Storage("ai-commit.xml")]
)
class AiCommitSettings : PersistentStateComponent<AiCommitSettings> {

    var mode: String = MODE_CLI
    var interfaceLanguage: String = LANGUAGE_ZH
    var apiConfigs: MutableList<ApiProviderConfig> = mutableListOf()
    var activeApiConfigId: String = ""
    var cliPath: String = ""
    var externalEditor: String = "code"

    /** 激活的 CLI */
    var activeProvider: String = PROVIDER_CLAUDE

    /** 使用的模型（随所选 CLI 生效，可带思考等级后缀；留空用该 CLI 默认模型） */
    var model: String = ""

    /** 用户附加提示词（叠加在内置提示词之后，优先遵循） */
    var customPrompt: String = ""

    /** 是否把仓库近期提交记录作为风格参考传给 AI */
    var includeRecentCommits: Boolean = true

    /** 传给 AI 的 diff 最大长度（字符数），过长会截断 */
    var maxDiffLength: Int = DEFAULT_MAX_DIFF_LENGTH

    /** 生成的 commit message 语言：[LANGUAGE_ZH] 或 [LANGUAGE_EN] */
    var outputLanguage: String = LANGUAGE_ZH

    /** 旧版本的按 CLI 模型字段，仅用于升级迁移，勿再写入 */
    var claudeModel: String = ""
    var codexModel: String = ""
    var ompModel: String = ""

    fun activeSdk(): SdkDefinition = SdkDefinition.fromProvider(activeProvider)

    fun modelFor(sdk: SdkDefinition): String = model.trim().ifBlank { sdk.defaultModel }

    fun activeApiConfig(): ApiProviderConfig? = apiConfigs.firstOrNull { it.id == activeApiConfigId }

    /** 创建一次生成任务使用的独立配置快照，避免提交过程中切换设置影响在途请求。 */
    fun generationSnapshot(): AiCommitSettings = AiCommitSettings().also { snapshot ->
        snapshot.mode = mode
        snapshot.interfaceLanguage = interfaceLanguage
        snapshot.apiConfigs = apiConfigs.map { it.deepCopy() }.toMutableList()
        snapshot.activeApiConfigId = activeApiConfigId
        snapshot.cliPath = cliPath
        snapshot.externalEditor = externalEditor
        snapshot.activeProvider = activeProvider
        snapshot.model = model
        snapshot.customPrompt = customPrompt
        snapshot.includeRecentCommits = includeRecentCommits
        snapshot.maxDiffLength = maxDiffLength
        snapshot.outputLanguage = outputLanguage
    }

    override fun getState(): AiCommitSettings = this

    override fun loadState(state: AiCommitSettings) {
        XmlSerializerUtil.copyBean(state, this)
        apiConfigs = apiConfigs.map { config ->
            config.deepCopy().apply { normalizeModels() }
        }.toMutableList()
        // 旧版本按 CLI 分字段存模型：迁移当前激活 CLI 的模型到单一字段后清空旧值
        if (model.isBlank()) {
            model = when (activeProvider.lowercase()) {
                PROVIDER_CODEX -> codexModel
                PROVIDER_OMP -> ompModel
                else -> claudeModel
            }.trim()
        }
        claudeModel = ""
        codexModel = ""
        ompModel = ""
        // 手工改过配置文件或未来新增语言时，未知值回退到中文
        if (outputLanguage != LANGUAGE_ZH && outputLanguage != LANGUAGE_EN) {
            outputLanguage = LANGUAGE_ZH
        }
        if (interfaceLanguage != LANGUAGE_ZH && interfaceLanguage != LANGUAGE_EN) interfaceLanguage = LANGUAGE_ZH
        if (mode != MODE_CLI && mode != MODE_API) mode = MODE_CLI
        if (activeApiConfig() == null) activeApiConfigId = apiConfigs.firstOrNull()?.id.orEmpty()
        maxDiffLength = maxDiffLength.coerceAtLeast(MIN_MAX_DIFF_LENGTH)
        SubMsgBundle.language = interfaceLanguage
    }

    companion object {
        const val MODE_CLI = "CLI"
        const val MODE_API = "API"
        const val PROVIDER_CLAUDE = "claude"
        const val PROVIDER_CODEX = "codex"
        const val PROVIDER_CURSOR = "cursor"
        const val PROVIDER_OPENCODE = "opencode"
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_GROK = "grok"
        const val PROVIDER_OMP = "omp"
        const val PROVIDER_PI = "pi"

        const val LANGUAGE_ZH = "zh"
        const val LANGUAGE_EN = "en"

        const val DEFAULT_MAX_DIFF_LENGTH = 5000
        /** 低于该值 diff 基本没有可用信息，设置页与服务端都以此为下限 */
        const val MIN_MAX_DIFF_LENGTH = 500
        /** 作为风格参考读取的近期提交条数 */
        const val RECENT_COMMITS_COUNT = 10

        fun getInstance(): AiCommitSettings =
            ApplicationManager.getApplication().getService(AiCommitSettings::class.java)
    }
}
