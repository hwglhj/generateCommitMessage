package com.fs.terminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * CLI 工具高权限配置(应用级持久化)。
 */
@State(name = "CliToolsSettings", storages = [Storage("fs-cli-tools.xml")])
class CliToolsSettings : PersistentStateComponent<CliToolsSettings.SettingsState> {

    class SettingsState {
        var claudeHighPermission: Boolean = false
        var codexHighPermission: Boolean = false
        var cursorHighPermission: Boolean = false
        var opencodeHighPermission: Boolean = false
        var antigravityHighPermission: Boolean = false
        var grokHighPermission: Boolean = false
        var ompHighPermission: Boolean = false
        var showClaudeIcon: Boolean = true
        var showCodexIcon: Boolean = true
        var showCursorIcon: Boolean = true
        var showOpenCodeIcon: Boolean = true
        var showAntigravityIcon: Boolean = true
        var showGrokIcon: Boolean = true
        var showOmpIcon: Boolean = true
        var showPiIcon: Boolean = true
    }

    private var state = SettingsState()

    override fun getState(): SettingsState = state

    override fun loadState(state: SettingsState) {
        this.state = state
    }

    fun isHighPermission(tool: CliTool): Boolean = when (tool) {
        CliTool.CLAUDE -> state.claudeHighPermission
        CliTool.CODEX -> state.codexHighPermission
        CliTool.CURSOR -> state.cursorHighPermission
        CliTool.OPENCODE -> state.opencodeHighPermission
        CliTool.ANTIGRAVITY -> state.antigravityHighPermission
        CliTool.GROK -> state.grokHighPermission
        CliTool.OMP -> state.ompHighPermission
        CliTool.PI -> false
    }

    fun setHighPermission(tool: CliTool, value: Boolean) = when (tool) {
        CliTool.CLAUDE -> state.claudeHighPermission = value
        CliTool.CODEX -> state.codexHighPermission = value
        CliTool.CURSOR -> state.cursorHighPermission = value
        CliTool.OPENCODE -> state.opencodeHighPermission = value
        CliTool.ANTIGRAVITY -> state.antigravityHighPermission = value
        CliTool.GROK -> state.grokHighPermission = value
        CliTool.OMP -> state.ompHighPermission = value
        CliTool.PI -> Unit
    }

    fun isShowIcon(tool: CliTool): Boolean = when (tool) {
        CliTool.CLAUDE -> state.showClaudeIcon
        CliTool.CODEX -> state.showCodexIcon
        CliTool.CURSOR -> state.showCursorIcon
        CliTool.OPENCODE -> state.showOpenCodeIcon
        CliTool.ANTIGRAVITY -> state.showAntigravityIcon
        CliTool.GROK -> state.showGrokIcon
        CliTool.OMP -> state.showOmpIcon
        CliTool.PI -> state.showPiIcon
    }

    fun setShowIcon(tool: CliTool, value: Boolean) = when (tool) {
        CliTool.CLAUDE -> state.showClaudeIcon = value
        CliTool.CODEX -> state.showCodexIcon = value
        CliTool.CURSOR -> state.showCursorIcon = value
        CliTool.OPENCODE -> state.showOpenCodeIcon = value
        CliTool.ANTIGRAVITY -> state.showAntigravityIcon = value
        CliTool.GROK -> state.showGrokIcon = value
        CliTool.OMP -> state.showOmpIcon = value
        CliTool.PI -> state.showPiIcon = value
    }

    companion object {
        fun getInstance(): CliToolsSettings =
            ApplicationManager.getApplication().getService(CliToolsSettings::class.java)
    }
}
