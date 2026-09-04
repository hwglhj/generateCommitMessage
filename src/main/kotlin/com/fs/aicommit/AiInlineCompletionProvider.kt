package com.fs.aicommit

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

/**
 * AI Inline 代码补全提供器
 * 在用户编码时提供 AI 驱动的补全建议
 */
class AiInlineCompletionProvider : CompletionContributor() {

    private val logger = Logger.getInstance(AiInlineCompletionProvider::class.java)

    init {
        // 注册补全提供器，对所有 PSI 元素生效
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    try {
                        val editor = parameters.editor
                        val file = parameters.originalFile.virtualFile ?: return

                        // 获取光标前后的代码
                        val document = editor.document
                        val offset = parameters.offset
                        val prefix = document.text.substring(0, offset)
                        val suffix = document.text.substring(offset)

                        // 异步获取补全建议
                        ApplicationManager.getApplication().executeOnPooledThread {
                            try {
                                val service = AiInlineService.getInstance()
                                val completions = service.getCompletions(file, prefix, suffix)

                                // 将建议添加到补全列表
                                ApplicationManager.getApplication().invokeLater {
                                    completions.forEach { completion ->
                                        val lookupElement = LookupElementBuilder.create(completion)
                                            .withTypeText("AI", true)
                                            .withIcon(com.intellij.icons.AllIcons.Actions.Lightning)
                                        result.addElement(lookupElement)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.warn("AI completion failed", e)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Completion provider error", e)
                    }
                }
            }
        )
    }
}
