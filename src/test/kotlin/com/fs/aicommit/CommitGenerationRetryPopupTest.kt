package com.fs.aicommit

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.WizardPopup
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.util.ui.UIUtil
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.awt.event.MouseEvent
import java.awt.event.KeyEvent
import javax.swing.JPanel

@RunWith(JUnit38ClassRunner::class)
class CommitGenerationRetryPopupTest : LightPlatformTestCase() {
    fun testCliKeyboardSelectionKeepsPickerUntilSuccess() {
        val flow = Flow(api = false)
        flow.startFailure()
        val popup = flow.popups.single()

        select(popup, "working high", mouse = false)

        assertFalse(popup.isDisposed)
        assertFalse(popup.list.isEnabled)
        assertEquals(1, flow.tasks.size)
        assertTrue(CommitGenerationTask.gate.isRunning)
        flow.finishNext()

        assertTrue("The CLI picker must close after success", popup.isDisposed)
        assertEquals(listOf("fix: working high"), flow.results)
    }

    fun testApiMouseSelectionReusesRootAndSubmenuAfterFailureAndClosesBothOnSuccess() {
        val flow = Flow(api = true)
        flow.startFailure()
        val root = flow.popups.single()
        val rootStep = root.listStep as ActionPopupStep
        val site = rootStep.values.first()
        val childStep = rootStep.onChosen(site, false) as ActionPopupStep
        val child = QuickModelActionPopup::class.java.getDeclaredMethod(
            "createPopup", WizardPopup::class.java, PopupStep::class.java, Any::class.java,
        ).apply { isAccessible = true }.invoke(root, root, childStep, site) as KeepingActionListPopup
        // Headless equivalent of ListPopupImpl.showNextStepPopup's parent-child linkage.
        WizardPopup::class.java.getDeclaredField("myChild").apply { isAccessible = true }.set(root, child)

        select(child, "also-broken", mouse = true)
        assertFalse(root.isDisposed)
        assertFalse(child.isDisposed)
        assertFalse(root.list.isEnabled)
        assertFalse(child.list.isEnabled)
        select(child, "working", mouse = true)
        assertEquals("Repeated clicks must not enqueue a second request", 1, flow.tasks.size)
        assertEquals("also-broken", flow.settings.activeApiConfig()!!.model)
        flow.finishNext()
        UIUtil.dispatchAllInvocationEvents()

        assertEquals("A failed retry must not open a replacement picker", 1, flow.popups.size)
        assertFalse(root.isDisposed)
        assertFalse(child.isDisposed)
        assertTrue(root.list.isEnabled)
        assertTrue(child.list.isEnabled)
        assertTrue(childStep.values.first { it.text == "working" }.isEnabled)
        assertTrue("The site label should reflect the newly selected model", rootStep.values.first().text.contains("also-broken"))

        selectWithSpace(child, "working")
        assertFalse(child.isDisposed)
        flow.finishNext()

        assertTrue(root.isDisposed)
        assertTrue(child.isDisposed)
        assertEquals(listOf("broken", "also-broken", "working"), flow.models)
        assertEquals(listOf("fix: working"), flow.results)
    }

    fun testHoveringModelDoesNotStartRetry() {
        val flow = Flow(api = false)
        flow.startFailure()
        val popup = flow.popups.single()
        select(popup, "working high", mouse = false, finalChoice = false)
        assertTrue(flow.tasks.isEmpty())
        assertFalse(popup.isDisposed)
    }

    fun testSpaceRetriesCliWithoutClosingPicker() {
        val flow = Flow(api = false)
        flow.startFailure()
        val popup = flow.popups.single()
        selectWithSpace(popup, "working high")
        selectWithSpace(popup, "working high")

        assertFalse(popup.isDisposed)
        assertEquals(1, flow.tasks.size)
        flow.finishNext()
        assertTrue(popup.isDisposed)
    }

    private fun selectWithSpace(popup: ListPopupImpl, label: String) {
        val step = popup.listStep as ActionPopupStep
        popup.list.selectedIndex = step.values.indexOfFirst { it.text == label }.also { assertTrue(it >= 0) }
        popup.dispatch(KeyEvent(popup.list, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' '))
    }

    private fun select(popup: ListPopupImpl, label: String, mouse: Boolean, finalChoice: Boolean = true) {
        val step = popup.listStep as ActionPopupStep
        popup.list.selectedIndex = step.values.indexOfFirst { it.text == label }.also { assertTrue(it >= 0) }
        if (mouse) {
            popup.handleSelect(finalChoice, MouseEvent(popup.list, MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1))
        } else popup.handleSelect(finalChoice)
    }

    private inner class Flow(private val api: Boolean) {
        val settings = AiCommitSettings().apply {
            mode = if (api) AiCommitSettings.MODE_API else AiCommitSettings.MODE_CLI
            model = "broken high"
            apiConfigs = mutableListOf(ApiProviderConfig(id = "primary", name = "Primary", model = "broken",
                models = mutableListOf("broken", "also-broken", "working")))
            activeApiConfigId = "primary"
        }
        val tasks = mutableListOf<CommitGenerationTask>()
        val popups = mutableListOf<QuickModelActionPopup>()
        val results = mutableListOf<String>()
        val models = mutableListOf<String>()
        val request = CommitGenerationRequest(
            project, settings,
            generate = { snapshot, _ ->
                val model = if (api) snapshot.activeApiConfig()!!.model else snapshot.model
                models.add(model)
                if (model.contains("broken")) throw IllegalStateException("unavailable")
                "fix: $model"
            },
            generated = { results.add(it) }, failed = {}, canceled = {},
            showModelSwitcher = { retry ->
                QuickModelAction.createPopup(project, settings, JPanel(), retry).also {
                    popups.add(it)
                    Disposer.register(testRootDisposable, it)
                }
            },
            canUpdateUi = { !project.isDisposed },
            runTask = { task ->
                tasks.add(task)
                Disposer.register(testRootDisposable) { task.onFinished() }
            },
        )

        fun startFailure() {
            // CLI quick choices use a cached catalog; provide a candidate without network access.
            if (!api) settings.model = "working high"
            request.start()
            if (api) {
                finishNext()
                return
            }
            val first = tasks.removeAt(0)
            first.onThrowable(IllegalStateException("Initial request failed"))
            first.onFinished()
        }

        fun finishNext() {
            val task = tasks.removeAt(0)
            try {
                task.run(EmptyProgressIndicator(ModalityState.NON_MODAL))
                task.onSuccess()
            } catch (error: Exception) {
                task.onThrowable(error)
            } finally {
                task.onFinished()
            }
        }
    }
}
