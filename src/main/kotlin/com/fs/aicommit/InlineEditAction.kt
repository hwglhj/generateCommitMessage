package com.fs.aicommit

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import javax.swing.JPanel

/**
 * AI 行内修改（类似 Cursor 的 Cmd+K）：
 * 划中代码 -> 在选区旁弹出指令输入框 -> 调用 AI Commit 配置的 CLI ->
 * 用 WriteCommandAction 直接替换选区（单一 undo 单元，Cmd+Z 可撤销）。
 */
class InlineEditAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            editor != null && editor.selectionModel.hasSelection() && editor.document.isWritable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        InlineEditRunner.promptAndRun(project, editor)
    }
}

/** AI 生成注释：无需输入指令，直接为选中代码添加注释（仅命令面板/快捷键，无右键菜单入口） */
class InlineCommentAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible =
            editor != null && editor.selectionModel.hasSelection() && editor.document.isWritable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        InlineEditRunner.run(project, editor, COMMENT_INSTRUCTION, "AI 生成注释")
    }

    companion object {
        private val COMMENT_INSTRUCTION = """
            为这段代码添加高质量注释：
            - 保持原代码逻辑、结构和格式完全不变，只添加注释
            - 函数/类/方法使用该语言惯用的文档注释风格（如 KDoc、Javadoc、JSDoc、docstring）
            - 关键或不易理解的逻辑行添加简短行内注释
            - 注释使用简体中文，简洁专业，不要为显而易见的代码添加注释
        """.trimIndent()
    }
}

private object InlineEditRunner {

    /** 选区前后各带多少行上下文，帮助模型理解代码环境 */
    private const val CONTEXT_LINES = 30

    /** 在选区旁弹出指令输入框（IDEA 支持贴着代码弹窗，比顶部输入框更接近 Cursor 体验） */
    fun promptAndRun(project: Project, editor: Editor) {
        val field = JBTextField(40).apply {
            emptyText.text = "描述如何修改选中的代码，回车执行，Esc 取消"
        }
        // 标题旁展示当前使用的 CLI 与模型（含思考等级后缀），来源于"行内 AI 修改"设置
        val settings = AiCommitSettings.getInstance()
        val sdk = settings.inlineSdk()
        val model = settings.inlineModelFor(sdk).ifBlank { "默认模型" }
        val header = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("AI 行内修改").apply { font = font.deriveFont(Font.BOLD) }, BorderLayout.WEST)
            add(JBLabel("${sdk.displayName} · $model").apply {
                foreground = JBColor.GRAY
                font = JBUI.Fonts.smallFont()
            }, BorderLayout.EAST)
        }
        val panel = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            border = JBUI.Borders.empty(8)
            add(header, BorderLayout.NORTH)
            add(field, BorderLayout.CENTER)
        }
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, field)
            .setRequestFocus(true)
            .setMovable(true)
            .setCancelOnClickOutside(true)
            // 降回普通窗口层级：重量级 popup 在 macOS 上是 NSPopUpMenuWindowLevel，
            // 会压住输入法候选词面板（Search Everywhere 曾有同样的 bug，平台为此加了该 API）
            .setNormalWindowLevel(true)
            .createPopup()
        field.addActionListener {
            val instruction = field.text.trim()
            popup.closeOk(null)
            if (instruction.isNotEmpty()) run(project, editor, instruction, "AI 行内修改")
        }
        popup.show(JBPopupFactory.getInstance().guessBestPopupLocation(editor))
    }

    fun run(project: Project, editor: Editor, instruction: String, actionLabel: String) {
        val document = editor.document
        val selStart = editor.selectionModel.selectionStart
        val selEnd = editor.selectionModel.selectionEnd
        if (selStart >= selEnd) return
        val selectedCode = document.getText(TextRange(selStart, selEnd))

        // RangeMarker 让选区范围跟随等待期间的文档变动自动偏移
        val marker = document.createRangeMarker(selStart, selEnd)
        val highlighter = addPendingHighlight(editor, selStart, selEnd)

        val prompt = buildPrompt(editor, instruction, selStart, selEnd, selectedCode)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "$actionLabel...", true) {
            var newCode: String = ""

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val settings = AiCommitSettings.getInstance()
                val sdk = settings.inlineSdk()
                val raw = AiCommitService(project).runPrompt(prompt, indicator, sdk, settings.inlineModelFor(sdk))
                newCode = extractCode(raw)
            }

            override fun onSuccess() {
                if (newCode.isBlank()) {
                    notify(project, "AI 未返回有效代码，已放弃修改", NotificationType.WARNING)
                    return
                }
                if (newCode == selectedCode) {
                    notify(project, "AI 认为无需修改", NotificationType.INFORMATION)
                    return
                }
                // 等待期间代码可能被改动：范围失效或内容不一致时放弃替换，结果放入剪贴板
                if (!marker.isValid ||
                    document.getText(TextRange(marker.startOffset, marker.endOffset)) != selectedCode
                ) {
                    CopyPasteManager.getInstance().setContents(StringSelection(newCode))
                    notify(project, "等待期间代码已变化，已放弃替换（结果在剪贴板中）", NotificationType.WARNING)
                    return
                }
                val start = marker.startOffset
                WriteCommandAction.runWriteCommandAction(project, actionLabel, null, {
                    document.replaceString(start, marker.endOffset, newCode)
                })
                // 选中新代码便于查看/继续修改
                editor.selectionModel.setSelection(start, start + newCode.length)
                editor.caretModel.moveToOffset(start + newCode.length)
            }

            override fun onThrowable(error: Throwable) {
                notify(project, "$actionLabel 失败: ${error.message ?: error.javaClass.simpleName}", NotificationType.ERROR)
            }

            override fun onFinished() {
                editor.markupModel.removeHighlighter(highlighter)
                marker.dispose()
            }
        })
    }

    /** 等待期间给选区加虚线框高亮 */
    private fun addPendingHighlight(editor: Editor, start: Int, end: Int): RangeHighlighter {
        val attributes = TextAttributes(null, null, JBColor.BLUE, EffectType.BOXED, Font.PLAIN)
        return editor.markupModel.addRangeHighlighter(
            start, end, HighlighterLayer.SELECTION - 1, attributes, HighlighterTargetArea.EXACT_RANGE
        )
    }

    private fun buildPrompt(
        editor: Editor,
        instruction: String,
        selStart: Int,
        selEnd: Int,
        selectedCode: String
    ): String {
        val document = editor.document
        val startLine = document.getLineNumber(selStart)
        val endLine = document.getLineNumber(selEnd)
        val beforeStart = document.getLineStartOffset((startLine - CONTEXT_LINES).coerceAtLeast(0))
        val afterEnd = document.getLineEndOffset((endLine + CONTEXT_LINES).coerceAtMost(document.lineCount - 1))
        val before = document.getText(TextRange(beforeStart, selStart))
        val after = document.getText(TextRange(selEnd, afterEnd))
        val file = FileDocumentManager.getInstance().getFile(document)
        val fileName = file?.name ?: "未知文件"
        val language = file?.fileType?.name ?: "未知语言"

        return """
你是一位资深工程师，负责按用户指令修改一段代码。

## 用户指令

$instruction

## 文件信息

- 文件名：$fileName
- 语言：$language

## 选中代码之前的上下文（仅供参考，不要修改，不要输出）

```
$before
```

## 需要修改的代码（只修改这一段）

```
$selectedCode
```

## 选中代码之后的上下文（仅供参考，不要修改，不要输出）

```
$after
```

## 输出要求（必须严格遵守）

- 只输出替换"需要修改的代码"的最终代码，**必须用 <code></code> 标签包裹**
- 标签外不要有任何内容：不要解释、不要分析、不要 markdown 围栏
- 保持与原代码一致的缩进风格（选中代码的首行缩进要保留）
- 不要重复输出上下文部分的代码
        """.trimIndent()
    }

    /** 提取 <code></code> 中的代码；兜底剥离 markdown 围栏 */
    private fun extractCode(output: String): String {
        var text = output
        val start = text.indexOf("<code>")
        val end = text.lastIndexOf("</code>")
        if (start != -1 && end > start) {
            text = text.substring(start + "<code>".length, end)
        }
        text = text
            .replace(Regex("^\\s*```[\\w-]*\\n"), "")
            .replace(Regex("\\n```\\s*$"), "")
        return text.trimStart('\n').trimEnd()
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("RestfulToolkit.AiCommit")
                .createNotification(content, type)
                .notify(project)
        }
    }
}
