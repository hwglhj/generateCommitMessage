package com.fs.aicommit

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 支持选择 Claude、Codex、Cursor、OpenCode、Gemini(agy)、Grok 或 OMP CLI；
 * 模型为单一输入，随所选 CLI 生效，可在末尾追加思考等级（如 "claude-opus-4-5 high"）。
 * 认证复用各 CLI 自身的登录凭证，不存储 API Key。
 */
@State(
    name = "AiCommitSettings",
    storages = [Storage("ai-commit.xml")]
)
class AiCommitSettings : PersistentStateComponent<AiCommitSettings> {

    /** 激活的 CLI */
    var activeProvider: String = PROVIDER_CLAUDE

    /** 使用的模型（随所选 CLI 生效，可带思考等级后缀；留空用该 CLI 默认模型） */
    var model: String = ""

    /** 行内 AI 修改使用的 CLI（与 AI Commit 独立配置；留空表示尚未配置，升级时迁移为跟随 AI Commit） */
    var inlineProvider: String = ""

    /** 行内 AI 修改使用的模型（含思考等级后缀，随 [inlineProvider] 生效） */
    var inlineModel: String = ""

    /** 行内 AI 修改的附加提示词 */
    var inlineCustomPrompt: String = ""

    /** 旧版本的按 CLI 模型字段，仅用于升级迁移，勿再写入 */
    var claudeModel: String = ""
    var codexModel: String = ""
    var ompModel: String = ""

    /** 用户附加提示词（叠加在内置提示词之后，优先遵循） */
    var customPrompt: String = ""

    /** 传给 AI 的 diff 最大长度（字符数），过长会截断 */
    var maxDiffLength: Int = DEFAULT_MAX_DIFF_LENGTH

    fun activeSdk(): SdkDefinition = SdkDefinition.fromProvider(activeProvider)

    /** 行内修改的 CLI；尚未单独配置时跟随 AI Commit */
    fun inlineSdk(): SdkDefinition =
        if (inlineProvider.isBlank()) activeSdk() else SdkDefinition.fromProvider(inlineProvider)

    fun modelFor(sdk: SdkDefinition): String = model.trim().ifBlank { sdk.defaultModel }

    fun inlineModelFor(sdk: SdkDefinition): String =
        if (inlineProvider.isBlank()) modelFor(sdk) else inlineModel.trim().ifBlank { sdk.defaultModel }

    override fun getState(): AiCommitSettings = this

    override fun loadState(state: AiCommitSettings) {
        XmlSerializerUtil.copyBean(state, this)
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
    }

    companion object {
        const val PROVIDER_CLAUDE = "claude"
        const val PROVIDER_CODEX = "codex"
        const val PROVIDER_CURSOR = "cursor"
        const val PROVIDER_OPENCODE = "opencode"
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_GROK = "grok"
        const val PROVIDER_OMP = "omp"
        const val DEFAULT_MAX_DIFF_LENGTH = 4000

        fun getInstance(): AiCommitSettings =
            ApplicationManager.getApplication().getService(AiCommitSettings::class.java)
    }
}
