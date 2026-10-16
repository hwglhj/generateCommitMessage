package com.fs.aicommit

import com.fs.i18n.SubMsgBundle.message as tr
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
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
import java.awt.event.InputEvent

/** 每次展开时重新读取当前设置，避免站点子菜单持有过期模型列表。 */
internal class LivePopupActionGroup(
    text: String,
    private val childrenProvider: () -> Array<AnAction>,
) : ActionGroup(text, true) {
    override fun getChildren(event: AnActionEvent?): Array<AnAction> = childrenProvider()
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
 * 这里沿用原生 Action Popup，只为站点子菜单补上“保持打开”和原地替换列表的能力，
 * 且不依赖已从新版 IDE 删除的 KeepingPopupOpenAction 内部接口。
 */
internal class QuickModelActionPopup(
    title: String,
    group: ActionGroup,
    private val popupDataContext: DataContext,
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
) {
    override fun createPopup(parent: WizardPopup, step: PopupStep<*>, parentValue: Any?): WizardPopup {
        if (step is ActionPopupStep) {
            return KeepingActionListPopup(project, parent, step, parentValue, popupDataContext)
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
) : ListPopupImpl(project, parent, step, parentValue) {
    override fun handleSelect(handleFinalChoices: Boolean) {
        if (!performKeepingAction(null)) super.handleSelect(handleFinalChoices)
    }

    override fun handleSelect(handleFinalChoices: Boolean, inputEvent: InputEvent?) {
        if (!performKeepingAction(inputEvent)) super.handleSelect(handleFinalChoices, inputEvent)
    }

    private fun performKeepingAction(inputEvent: InputEvent?): Boolean {
        val actionStep = listStep as? ActionPopupStep ?: return false
        val item = list.selectedValue as? PopupFactoryImpl.ActionItem ?: return false
        if (!actionStep.isSelectable(item) || item.action !is ApiSiteRefreshAction) return false
        val event = AnActionEvent.createFromAnAction(
            item.action,
            inputEvent,
            ActionPlaces.POPUP,
            popupDataContext,
        )
        ActionUtil.performActionDumbAwareWithCallbacks(item.action, event)
        actionStep.updateStepItems(list)
        return true
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
