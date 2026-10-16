package com.fs.aicommit

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.ui.popup.ActionPopupStep
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import javax.swing.JPanel

@RunWith(JUnit38ClassRunner::class)
class QuickModelPopupIntegrationTest : LightPlatformTestCase() {
    fun testRefreshActionDoesNotLinkRemovedKeepingPopupOpenAction() {
        assertFalse(
            ApiSiteRefreshAction::class.java.interfaces.any {
                it.name == "com.intellij.ui.popup.KeepingPopupOpenAction"
            }
        )
    }

    fun testRefreshActionKeepsNestedPopupOpenAndReplacesVisibleModels() {
        val context = quickModelPopupDataContext(project, JPanel())
        var refreshInvoked = false
        val originalGroup = DefaultActionGroup(
            ApiSiteRefreshAction("site", project) { refreshInvoked = true },
            action("old-model"),
        )
        val step = ActionPopupStep.createActionsStep(
            originalGroup,
            context,
            false,
            true,
            true,
            null,
            false,
            false,
            { context },
            ActionPlaces.POPUP,
            null,
            -1,
            null,
        ) as ActionPopupStep
        val parent = QuickModelActionPopup("Models", DefaultActionGroup(), context)
        val popup = KeepingActionListPopup(project, parent, step, null, context)

        try {
            popup.list.selectedIndex = 0
            popup.handleSelect(true)

            assertTrue(refreshInvoked)
            assertFalse(popup.isDisposed)

            popup.refreshHandle().replace(
                DefaultActionGroup(
                    ApiSiteRefreshAction("site", project) {},
                    action("new-a"),
                    action("new-b"),
                )
            )

            val refreshedStep = popup.listStep as ActionPopupStep
            val refreshedLabels = refreshedStep.values.map(refreshedStep::getTextFor)
            assertEquals(3, refreshedLabels.size)
            assertEquals(listOf("new-a", "new-b"), refreshedLabels.drop(1))
        } finally {
            Disposer.dispose(popup)
            Disposer.dispose(parent)
        }
    }

    private fun action(text: String): AnAction = object : AnAction(text) {
        override fun actionPerformed(event: AnActionEvent) = Unit
    }
}
