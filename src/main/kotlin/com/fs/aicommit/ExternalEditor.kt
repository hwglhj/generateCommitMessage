package com.fs.aicommit

import java.io.File

internal object ExternalEditor {
    private val names = mapOf("code" to "VS Code", "cursor" to "Cursor", "antigravity" to "Antigravity", "windsurf" to "Windsurf")
    fun name(editor: String): String = names[editor] ?: names.getValue("code")

    fun candidates(editor: String, osName: String, home: String, env: Map<String, String>): List<String> {
        val id = editor.takeIf { it in names } ?: "code"
        val product = if (id == "code") "Microsoft VS Code" else names.getValue(id)
        val executable = if (id == "code") "Code.exe" else "${names.getValue(id)}.exe"
        fun value(key: String) = env.entries.firstOrNull { it.key.equals(key, true) }?.value
        val paths = mutableListOf<String>()
        if (osName.startsWith("Windows", true)) {
            value("LOCALAPPDATA")?.let { paths.add(File(it, "Programs/$product/$executable").path) }
            for (key in listOf("ProgramFiles", "ProgramFiles(x86)")) {
                value(key)?.let { paths.add(File(it, "$product/$executable").path) }
            }
        } else if (osName.contains("Mac", true)) {
            val app = if (id == "code") "Visual Studio Code" else product
            paths.add("/Applications/$app.app/Contents/Resources/app/bin/$id")
            paths.add(File(home, "Applications/$app.app/Contents/Resources/app/bin/$id").path)
        } else {
            paths.add("/usr/bin/$id")
            paths.add("/usr/local/bin/$id")
        }
        val separator = if (osName.startsWith("Windows", true)) ";" else ":"
        value("PATH").orEmpty().split(separator).filter { it.isNotBlank() }.forEach { directory ->
            if (osName.startsWith("Windows", true)) {
                paths.add(File(directory, "../$executable").path)
                paths.add(File(directory, executable).path)
                paths.add(File(directory, "$id.cmd").path)
            } else paths.add(File(directory, id).path)
        }
        return paths.distinct()
    }

    fun find(editor: String): String? = candidates(editor, System.getProperty("os.name"), System.getProperty("user.home"), System.getenv())
        .firstOrNull { File(it).isFile }

    fun command(executable: String, file: String, line: Int, column: Int): List<String> =
        listOf(executable, "--goto", "$file:${line.coerceAtLeast(1)}:${column.coerceAtLeast(1)}")
}
