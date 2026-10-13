package com.fs.aicommit

import org.junit.Assert.*
import org.junit.Test

class ExternalEditorTest {
    @Test
    fun `Windows candidates include per-user installations for each editor`() {
        for (editor in listOf("code", "cursor", "antigravity", "windsurf")) {
            val paths = ExternalEditor.candidates(editor, "Windows 11", "C:/Users/Test", mapOf(
                "LOCALAPPDATA" to "C:/Users/Test/AppData/Local", "ProgramFiles" to "C:/Program Files"
            ))
            assertTrue(paths.any { it.replace('\\', '/').contains("AppData/Local/Programs/") && it.endsWith(".exe") })
        }
    }

    @Test
    fun `editor command keeps file path and cursor position in one argument`() {
        assertEquals(listOf("C:/Program Files/Cursor/Cursor.exe", "--goto", "D:/project space/文件.kt:12:8"),
            ExternalEditor.command("C:/Program Files/Cursor/Cursor.exe", "D:/project space/文件.kt", 12, 8))
    }
}
