package com.fs.aicommit

import org.junit.Assert.*
import org.junit.Test

class SdkManagerTest {
    @Test
    fun `pi sdk uses its own default model and thinking levels`() {
        val sdk = SdkDefinition.fromProvider("pi")
        assertEquals(SdkDefinition.PI_SDK, sdk)
        assertEquals("pi", sdk.binName)
        assertEquals("", sdk.defaultModel)
        assertEquals(listOf("off", "minimal", "low", "medium", "high", "xhigh", "max"), sdk.thinkingLevels)
    }

    @Test
    fun `windows executable candidates include npm command shims`() {
        val names = SdkManager.executableNames("claude", "Windows 11")
        assertTrue(names.contains("claude.exe"))
        assertTrue(names.contains("claude.cmd"))
        assertTrue(names.contains("claude.bat"))
        assertEquals(listOf("claude"), SdkManager.executableNames("claude", "Linux"))
    }

    @Test
    fun `windows path merge preserves drive letters and removes duplicates`() {
        assertEquals("C:\\tools;C:\\node;D:\\bin", SdkManager.mergePath(
            listOf("C:\\tools", "C:\\node"), "C:\\node;D:\\bin", "Windows 11"
        ))
        assertEquals("/usr/bin:/bin", SdkManager.mergePath(listOf("/usr/bin"), "/usr/bin:/bin", "Linux"))
    }

    @Test
    fun `windows installation directories include npm and nvm windows`() {
        val dirs = SdkManager.installationDirectories("Windows 11", "C:\\Users\\test", mapOf(
            "APPDATA" to "C:\\Users\\test\\AppData\\Roaming",
            "LOCALAPPDATA" to "C:\\Users\\test\\AppData\\Local",
            "NVM_SYMLINK" to "C:\\Program Files\\nodejs"
        ))
        assertTrue(dirs.any { it.replace('\\', '/').endsWith("AppData/Roaming/npm") })
        assertTrue(dirs.contains("C:\\Program Files\\nodejs"))
    }

    @Test
    fun `windows installation directories include pi managed launcher`() {
        val dirs = SdkManager.installationDirectories("Windows 11", "C:\\Users\\test", emptyMap())
        assertTrue(dirs.contains("C:\\Users\\test\\.pi\\agent\\bin"))
    }

    @Test
    fun `pi model table is parsed into provider model selectors`() {
        val output = """
            provider   model                       context  max-out  thinking  images
            anthropic  claude-sonnet-4-6          200K     64K      yes       yes
            openai     gpt-5.5                    400K     128K     yes       yes
            anthropic  claude-sonnet-4-6          200K     64K      yes       yes
        """.trimIndent()

        assertEquals(
            listOf("anthropic/claude-sonnet-4-6", "openai/gpt-5.5"),
            SdkManager.parsePiModelTable(output)
        )
    }

    @Test
    fun `pi model parser ignores messages and malformed rows`() {
        assertTrue(SdkManager.parsePiModelTable("No models available. Run /login first.").isEmpty())
        assertTrue(SdkManager.parsePiModelTable("provider  model  context").isEmpty())
    }
}
