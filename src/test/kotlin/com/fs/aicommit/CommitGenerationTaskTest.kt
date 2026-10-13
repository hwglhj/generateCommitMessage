package com.fs.aicommit

import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.application.ModalityState
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException

class CommitGenerationTaskTest {
    private fun task(gate: GenerationGate, events: MutableList<String>, open: Boolean = true) =
        CommitGenerationTask(null, gate.acquire()!!,
            generate = { "feat: example" },
            generated = { events.add(it) },
            failed = { events.add("failure") },
            canceled = { events.add("canceled") },
            showModelSwitcher = {
                assertFalse("Switching must be unlocked before the popup opens", gate.isRunning)
                events.add("switcher")
            },
            canUpdateUi = { open })

    @Test
    fun `generation failure unlocks switching and automatically opens the picker`() {
        val gate = GenerationGate()
        val events = mutableListOf<String>()
        val task = task(gate, events)
        assertTrue(gate.isRunning)
        assertNull(gate.acquire())
        task.onThrowable(ApiException("HTTP 503"))
        task.onFinished()
        assertEquals(listOf("failure", "switcher"), events)
        assertFalse(gate.isRunning)
    }

    @Test
    fun `success writes the result without showing a picker`() {
        val gate = GenerationGate()
        val events = mutableListOf<String>()
        val task = task(gate, events)
        task.run(EmptyProgressIndicator(ModalityState.NON_MODAL))
        task.onSuccess()
        task.onFinished()
        assertEquals(listOf("feat: example"), events)
        assertFalse(gate.isRunning)
    }

    @Test
    fun `cancellation never shows the failure picker`() {
        for (error in listOf(ProcessCanceledException(), CancellationException())) {
            val gate = GenerationGate()
            val events = mutableListOf<String>()
            val task = task(gate, events)
            task.onThrowable(error)
            task.onFinished()
            assertEquals(listOf("canceled"), events)
            assertFalse(gate.isRunning)
        }
        val gate = GenerationGate()
        val events = mutableListOf<String>()
        task(gate, events).onCancel()
        assertEquals(listOf("canceled"), events)
        assertFalse(gate.isRunning)
    }

    @Test
    fun `closed project suppresses notifications and popup but still unlocks`() {
        val gate = GenerationGate()
        val events = mutableListOf<String>()
        val task = task(gate, events, open = false)
        task.onThrowable(ApiException("offline"))
        task.onFinished()
        assertTrue(events.isEmpty())
        assertFalse(gate.isRunning)
    }

    @Test
    fun `finishing an old task cannot release a newer request`() {
        val gate = GenerationGate()
        val task = task(gate, mutableListOf())
        task.onThrowable(ApiException("offline"))
        val next = gate.acquire()!!
        task.onFinished()
        assertTrue(gate.isRunning)
        next.close()
        assertFalse(gate.isRunning)
    }
}
