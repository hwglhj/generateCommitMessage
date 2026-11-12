package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.ui.popup.ActionPopupStep
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.popup.WizardPopup
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.ListPopupModel
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.SwingConstants

/** 每次展开时重新读取当前设置，避免站点子菜单持有过期模型列表。 */
internal class LivePopupActionGroup(
    text: String,
    private val currentText: (() -> String)? = null,
    private val childrenProvider: () -> Array<AnAction>,
) : ActionGroup(text, true) {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> = childrenProvider()

    override fun update(event: AnActionEvent) {
        currentText?.let { event.presentation.text = it() }
    }
}

internal fun quickModelPopupDataContext(project: Project, component: Component): DataContext =
    SimpleDataContext.builder()
        .setParent(SimpleDataContext.getProjectContext(project))
        .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, component)
        .build()

/** 由站点子菜单识别的刷新操作；执行后保留当前 Action Popup。 */
internal class ApiSiteRefreshAction(
    private val configId: String,
    private val project: Project,
    private val refresh: (AnActionEvent) -> Unit,
) : AnAction(
    if (QuickModelAction.isApiRefreshRunning(configId)) tr("action.quick.model.refreshing")
    else tr("action.quick.model.refresh.site"),
    null,
    AllIcons.Actions.Refresh,
), DumbAware {
    override fun actionPerformed(event: AnActionEvent) = refresh(event)

    override fun update(event: AnActionEvent) {
        event.presentation.text = if (QuickModelAction.isApiRefreshRunning(configId)) {
            tr("action.quick.model.refreshing")
        } else {
            tr("action.quick.model.refresh.site")
        }
        event.presentation.isEnabled = !project.isDisposed && !QuickModelAction.isApiRefreshRunning(configId)
    }
}

/** 用最新 ActionGroup 内容替换已经显示的站点子菜单。 */
internal object QuickModelPopupItems {
    fun <T> replaceValues(current: MutableList<T>, latest: List<T>, sync: () -> Unit) {
        current.clear()
        current.addAll(latest)
        sync()
    }

    fun replace(
        step: ActionPopupStep,
        model: ListPopupModel<PopupFactoryImpl.ActionItem>,
        group: ActionGroup,
        dataContext: DataContext,
        actionPlace: String = ActionPlaces.POPUP,
    ) {
        val items = ActionPopupStep.createActionItems(
            group,
            dataContext,
            false,
            true,
            true,
            false,
            actionPlace,
            PresentationFactory(),
        )
        replaceValues(step.values, items, model::syncModel)
    }
}

internal interface QuickModelPopupRefreshHandle {
    fun replace(group: ActionGroup)

    companion object {
        val NONE: QuickModelPopupRefreshHandle = object : QuickModelPopupRefreshHandle {
            override fun replace(group: ActionGroup) = Unit
        }
    }
}

/**
 * IntelliJ 的 ActionGroup 子菜单默认会在执行普通 Action 后关闭。
 * 刷新站点和模型生成期间保留弹窗，生成成功后由请求关闭。
 * 且不依赖已从新版 IDE 删除的 KeepingPopupOpenAction 内部接口。
 */
internal class QuickModelActionPopup(
    title: String,
    group: ActionGroup,
    private val popupDataContext: DataContext,
    private val keepModelSelectionOpen: Boolean = false,
) : PopupFactoryImpl.ActionGroupPopup(
    title,
    group,
    popupDataContext,
    false,
    false,
    true,
    false,
    null,
    -1,
    null,
    null,
), CommitModelSwitcher {
    init {
        if (keepModelSelectionOpen) {
            registerAction("retryModelWithSpace", KeyEvent.VK_SPACE, 0, object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent?) {
                    performKeepingPopupAction(this@QuickModelActionPopup, popupDataContext, true, true, null)
                }
            })
        }
    }

    override val isOpen: Boolean get() = !isDisposed

    override fun setGenerating(generating: Boolean) {
        if (isDisposed) return
        setAdText(tr(if (generating) "commit.generating" else "action.quick.model.failure.hint"), SwingConstants.LEFT)
        list.isEnabled = !generating
        (listStep as? ActionPopupStep)?.updateStepItems(list)
        list.repaint()
        (myChild as? KeepingActionListPopup)?.let {
            it.list.isEnabled = !generating
            it.updateActions()
        }
    }

    override fun close() {
        if (!isDisposed) cancel()
    }

    override fun handleSelect(handleFinalChoices: Boolean) {
        if (!performKeepingPopupAction(this, popupDataContext, keepModelSelectionOpen, handleFinalChoices, null)) {
            super.handleSelect(handleFinalChoices)
        }
    }

    override fun handleSelect(handleFinalChoices: Boolean, inputEvent: InputEvent?) {
        if (!performKeepingPopupAction(this, popupDataContext, keepModelSelectionOpen, handleFinalChoices, inputEvent)) {
            super.handleSelect(handleFinalChoices, inputEvent)
        }
    }

    override fun createPopup(parent: WizardPopup, step: PopupStep<*>, parentValue: Any?): WizardPopup {
        if (step is ActionPopupStep) {
            return KeepingActionListPopup(project, parent, step, parentValue, popupDataContext, keepModelSelectionOpen).apply {
                list.isEnabled = !keepModelSelectionOpen || !CommitGenerationTask.gate.isRunning
            }
        }
        return super.createPopup(parent, step, parentValue)
    }

    fun openSubmenuHandle(): QuickModelPopupRefreshHandle =
        (myChild as? KeepingActionListPopup)?.refreshHandle() ?: QuickModelPopupRefreshHandle.NONE
}

internal class KeepingActionListPopup(
    project: Project?,
    parent: WizardPopup,
    step: ListPopupStep<*>,
    parentValue: Any?,
    private val popupDataContext: DataContext,
    private val keepModelSelectionOpen: Boolean = false,
) : ListPopupImpl(project, parent, step, parentValue) {
    init {
        if (keepModelSelectionOpen) {
            registerAction("retryModelWithSpace", KeyEvent.VK_SPACE, 0, object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent?) {
                    performKeepingPopupAction(this@KeepingActionListPopup, popupDataContext, true, true, null)
                }
            })
        }
    }

    override fun handleSelect(handleFinalChoices: Boolean) {
        if (!performKeepingPopupAction(this, popupDataContext, keepModelSelectionOpen, handleFinalChoices, null)) {
            super.handleSelect(handleFinalChoices)
        }
    }

    override fun handleSelect(handleFinalChoices: Boolean, inputEvent: InputEvent?) {
        if (!performKeepingPopupAction(this, popupDataContext, keepModelSelectionOpen, handleFinalChoices, inputEvent)) {
            super.handleSelect(handleFinalChoices, inputEvent)
        }
    }

    fun updateActions() {
        if (isDisposed) return
        (listStep as? ActionPopupStep)?.updateStepItems(list)
        list.repaint()
    }

    fun refreshHandle(): QuickModelPopupRefreshHandle = object : QuickModelPopupRefreshHandle {
        override fun replace(group: ActionGroup) {
            if (isDisposed) return
            val actionStep = listStep as? ActionPopupStep ?: return
            @Suppress("UNCHECKED_CAST")
            val model = list.model as? ListPopupModel<PopupFactoryImpl.ActionItem> ?: return
            val selectedIndex = list.selectedIndex
            QuickModelPopupItems.replace(actionStep, model, group, popupDataContext)
            if (model.size > 0) list.selectedIndex = selectedIndex.coerceIn(0, model.size - 1)
            list.revalidate()
            list.repaint()
            pack(true, true)
        }
    }
}

/** 只拦截本插件需要保持打开的操作，不让原生选择逻辑提前销毁弹窗。 */
private fun performKeepingPopupAction(
    popup: ListPopupImpl,
    dataContext: DataContext,
    keepModelSelectionOpen: Boolean,
    finalChoice: Boolean,
    inputEvent: InputEvent?,
): Boolean {
    if (keepModelSelectionOpen && CommitGenerationTask.gate.isRunning) return true
    val step = popup.listStep as? ActionPopupStep ?: return false
    val item = popup.list.selectedValue as? PopupFactoryImpl.ActionItem ?: return false
    if (item.action !is ApiSiteRefreshAction && !(keepModelSelectionOpen && item.action is ToggleAction)) return false
    if (!finalChoice || popup.isDisposed) return true
    val event = AnActionEvent.createFromAnAction(item.action, inputEvent, ActionPlaces.POPUP, dataContext)
    // 列表展示可能早于任务状态变化；执行前再更新，避免重复点击使用旧的 enabled 状态。
    item.action.update(event)
    if (event.presentation.isEnabled && event.presentation.isVisible) {
        ActionUtil.performActionDumbAwareWithCallbacks(item.action, event)
    }
    if (!popup.isDisposed) {
        step.updateStepItems(popup.list)
        popup.list.repaint()
    }
    return true
}
