package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

internal object CliProcess {
    data class Result(val exitCode: Int, val stdout: String, val stderr: String)

    fun prepareCommand(command: List<String>): List<String> {
        require(command.isNotEmpty())
        if (!System.getProperty("os.name").startsWith("Windows", true)) return command
        val file = File(command.first())
        if (file.extension.lowercase() !in listOf("cmd", "bat")) return command
        // npm shims forward through cmd.exe, which changes multiline prompts and shell characters.
        if (file.isFile && file.length() < 65536) {
            val match = Regex("\"%(?:dp0%|~dp0)[\\\\/]?([^\"\\r\\n]+\\.(?:[cm]?js|exe))\"\\s+%\\*", RegexOption.IGNORE_CASE)
                .find(file.readText())
            if (match != null) {
                val target = File(file.absoluteFile.parentFile, match.groupValues[1])
                if (target.isFile) {
                    val prefix = if (target.extension.equals("exe", true)) listOf(target.path) else {
                        val node = File(file.absoluteFile.parentFile, "node.exe").takeIf { it.isFile }
                            ?: SdkManager.pathEnv().split(File.pathSeparator).map { File(it, "node.exe") }.firstOrNull { it.isFile }
                            ?: throw IllegalStateException(tr("cli.node.missing"))
                        listOf(node.path, target.path)
                    }
                    return prefix + command.drop(1)
                }
            }
        }
        if (command.any { arg -> arg.any { it == '"' || it == '%' || it == '\r' || it == '\n' } }) {
            throw IllegalArgumentException(tr("cli.batch.arguments"))
        }
        val shell = System.getenv("COMSPEC") ?: "cmd.exe"
        return listOf(shell, "/d", "/v:off", "/s", "/c", "\"" + command.joinToString(" ") { "\"$it\"" } + "\"")
    }

    fun builder(command: List<String>, directory: File? = null): ProcessBuilder {
        val prepared = prepareCommand(command)
        val batchFallback = System.getProperty("os.name").startsWith("Windows", true) &&
            File(command.first()).extension.lowercase() in listOf("cmd", "bat") &&
            prepared.first().equals(System.getenv("COMSPEC") ?: "cmd.exe", true)
        // The cmd.exe fallback already contains the quoting required by /s /c.
        val builder = if (batchFallback) ProcessBuilder(prepared) else GeneralCommandLine(prepared).toProcessBuilder()
        return builder.apply {
            directory?.let { directory(it) }
            environment()["PATH"] = SdkManager.mergePath(listOfNotNull(File(command.first()).parent), SdkManager.pathEnv())
        }
    }

    fun run(
        command: List<String>, stdin: String? = null, directory: File? = null,
        timeoutMillis: Long = 180000, checkCanceled: () -> Unit = {}, onTick: (Long) -> Unit = {}
    ): Result {
        checkCanceled()
        val stdout = Files.createTempFile("ai-commit-out-", ".txt").toFile()
        val stderr = Files.createTempFile("ai-commit-err-", ".txt").toFile()
        var process: Process? = null
        var writer: Thread? = null
        try {
            val running = builder(command, directory).redirectOutput(stdout).redirectError(stderr).start()
            process = running
            writer = thread(name = "ai-commit-stdin", isDaemon = true) {
                try {
                    running.outputStream.use { if (stdin != null) it.write(stdin.toByteArray(UTF_8)) }
                } catch (_: java.io.IOException) {
                    // The child can close stdin early or be terminated by cancellation.
                }
            }
            val start = System.nanoTime()
            while (!running.waitFor(100, TimeUnit.MILLISECONDS)) {
                checkCanceled()
                val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                onTick(elapsed / 1000)
                if (elapsed >= timeoutMillis) throw TimeoutException(tr("cli.timeout", timeoutMillis / 1000))
            }
            checkCanceled()
            return Result(running.exitValue(), stdout.readText(UTF_8), stderr.readText(UTF_8))
        } finally {
            process?.let { running ->
                if (running.isAlive) {
                    running.descendants().use { descendants -> descendants.toList().asReversed().forEach { it.destroyForcibly() } }
                    running.destroyForcibly()
                    running.waitFor(2, TimeUnit.SECONDS)
                }
                try { running.outputStream.close() } catch (_: java.io.IOException) { }
            }
            writer?.join(1000)
            stdout.delete()
            stderr.delete()
        }
    }
}
