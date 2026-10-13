package com.fs.terminal

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * 支持的 CLI 工具:每个工具对应一个右侧边栏图标、一个终端标签页。
 * 命令与高权限参数对齐 vsplugins (extension.js#getCliCommand)。
 */
enum class CliTool(
    val id: String,            // ToolWindow id / 标签页名
    val baseCommand: String,
    val highPermissionCommand: String,
    val highPermissionDesc: String,
    val supportsHighPermission: Boolean = true
) {
    CLAUDE(
        "Claude", "claude",
        "claude --permission-mode bypassPermissions",
        "Claude 高权限启动 (--permission-mode bypassPermissions)"
    ),
    CODEX(
        "Codex", "codex",
        "codex --sandbox danger-full-access --ask-for-approval never",
        "Codex 高权限启动 (--sandbox danger-full-access --ask-for-approval never)"
    ),
    CURSOR(
        "Cursor", "cursor-agent",
        "cursor-agent --force",
        "Cursor 高权限启动 (cursor-agent --force)"
    ),
    OPENCODE(
        "OpenCode", "opencode",
        "opencode --auto",
        "OpenCode 高权限启动 (opencode --auto)"
    ),
    ANTIGRAVITY(
        "Antigravity", "agy",
        "agy --dangerously-skip-permissions",
        "Antigravity 高权限启动 (--dangerously-skip-permissions)"
    ),
    GROK(
        "Grok", "grok",
        "grok --always-approve",
        "Grok 高权限启动 (--always-approve)"
    ),
    OMP(
        "OMP", "omp",
        "omp --auto-approve",
        "Oh My Pi 高权限启动 (--auto-approve)"
    ),
    PI(
        "Pi", "pi", "pi",
        "Pi 使用当前进程权限启动",
        supportsHighPermission = false
    );

    /** 与 plugin.xml 中 ToolWindow 图标一致,用于终端标签页 */
    val icon: Icon by lazy { IconLoader.getIcon("/icons/${id.lowercase()}.svg", CliTool::class.java) }

    fun command(highPermission: Boolean): String =
        if (supportsHighPermission && highPermission) highPermissionCommand else baseCommand

    companion object {
        fun byToolWindowId(id: String): CliTool? = values().firstOrNull { it.id == id }
    }
}
