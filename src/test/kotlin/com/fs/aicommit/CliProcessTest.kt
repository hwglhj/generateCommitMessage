package com.fs.aicommit

import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CliProcessTest {
    @Test
    fun `ordinary Windows batch files run from paths containing spaces`() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"))
        val directory = Files.createTempDirectory("batch space ").toFile()
        try {
            for (extension in listOf("cmd", "bat")) {
                val script = File(directory, "01-version.$extension").apply {
                    writeText("@echo off\r\nif not \"%~1\"==\"--version\" exit /b 2\r\necho version 1.2.3\r\n")
                }
                val result = CliProcess.run(listOf(script.path, "--version"), timeoutMillis = 10000)
                assertEquals(result.stderr, 0, result.exitCode)
                assertEquals("version 1.2.3", result.stdout.trim())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `Windows npm wrapper is bypassed so multiline and shell syntax stay literal`() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"))
        val directory = Files.createTempDirectory("cli space ").toFile()
        try {
            val script = File(directory, "01-args.js").apply {
                writeText("process.stdout.write(JSON.stringify(process.argv.slice(2)));")
            }
            val shim = File(directory, "02-shim.cmd").apply {
                writeText("@echo off\r\nnode \"%~dp0${script.name}\" %*\r\n")
            }
            val argument = "first\nsecond & echo injected %PATH% \"quoted\" 中文"
            val result = CliProcess.run(listOf(shim.path, argument), timeoutMillis = 10000)
            assertEquals(0, result.exitCode)
            assertEquals(argument, com.google.gson.JsonParser.parseString(result.stdout).asJsonArray[0].asString)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `process timeout returns while output is still open`() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"))
        val started = System.nanoTime()
        try {
            CliProcess.run(listOf("cmd.exe", "/d", "/c", "ping -n 10 127.0.0.1 > nul"), timeoutMillis = 200)
            fail("Expected timeout")
        } catch (_: java.util.concurrent.TimeoutException) {
            assertTrue((System.nanoTime() - started) / 1_000_000 < 5000)
        }
    }
}
