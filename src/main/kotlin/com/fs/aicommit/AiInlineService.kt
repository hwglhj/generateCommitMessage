package com.fs.aicommit

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * AI Inline Reference 服务
 * 提供实时代码补全建议
 */
@Service
class AiInlineService {

    private val settings = AiCommitSettings.getInstance()
    private val logger = Logger.getInstance(AiInlineService::class.java)
    private val gson = Gson()

    /**
     * 获取代码补全建议
     * @param file 当前文件
     * @param prefix 光标前的代码
     * @param suffix 光标后的代码
     * @return 补全建议列表
     */
    fun getCompletions(file: VirtualFile, prefix: String, suffix: String): List<String> {
        try {
            val sdk = settings.inlineSdk()
            val model = settings.inlineModelFor(sdk)
            val prompt = buildPrompt(file, prefix, suffix)

            return getCompletionsFromCli(sdk, model, prompt)
        } catch (e: Exception) {
            logger.warn("AI Inline completion failed", e)
            return emptyList()
        }
    }

    /**
     * 使用 CLI 获取补全
     */
    private fun getCompletionsFromCli(
        sdk: SdkDefinition,
        model: String,
        prompt: String
    ): List<String> {
        val cliPath = SdkManager.detect(sdk, null).path ?: return emptyList()

        // 拆分模型名和思考等级
        val (baseModel, thinking) = SdkDefinition.splitModelThinking(sdk, model)

        val command = mutableListOf(cliPath)

        when (sdk) {
            SdkDefinition.CLAUDE_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
                if (thinking.isNotBlank()) {
                    command.add("--thinking")
                    command.add(thinking)
                }
                command.add("--no-stream")
            }
            SdkDefinition.CODEX_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
            SdkDefinition.CURSOR_SDK -> {
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
            SdkDefinition.OPENCODE_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
            SdkDefinition.GEMINI_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
            SdkDefinition.GROK_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
            SdkDefinition.OMP_SDK -> {
                command.add("chat")
                if (baseModel.isNotBlank()) {
                    command.add("--model")
                    command.add(baseModel)
                }
            }
        }

        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            process.outputStream.write(prompt.toByteArray())
            process.outputStream.close()

            val output = BufferedReader(InputStreamReader(process.inputStream))
                .readText()

            process.waitFor()

            parseCompletions(output)
        } catch (e: Exception) {
            logger.warn("CLI execution failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * 构建提示词
     */
    private fun buildPrompt(file: VirtualFile, prefix: String, suffix: String): String {
        val language = file.extension ?: "text"
        val customPrompt = settings.inlineCustomPrompt

        val basePrompt = """
You are a professional code completion assistant. Analyze the code context and provide accurate code completions.

File: ${file.name}
Language: $language

Code before cursor:
```
${prefix.takeLast(500)}
```

Code after cursor:
```
${suffix.take(100)}
```

Requirements:
- Provide ONLY the completion text, no explanations
- Keep completions concise and relevant
- Follow the project's coding style
- Prioritize readability and maintainability

${if (customPrompt.isNotBlank()) "\nAdditional instructions:\n$customPrompt" else ""}

Complete the code:
        """.trimIndent()

        return basePrompt
    }

    /**
     * 解析 CLI 输出为补全建议列表
     */
    private fun parseCompletions(output: String): List<String> {
        // 简单处理：将输出按行分割，过滤空行
        return output.lines()
            .filter { it.isNotBlank() }
            .take(5) // 最多返回 5 条建议
    }

    companion object {
        fun getInstance(): AiInlineService =
            ApplicationManager.getApplication().getService(AiInlineService::class.java)
    }
}
