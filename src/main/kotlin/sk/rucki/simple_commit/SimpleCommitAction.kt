package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

internal class SimpleCommitAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val service = SimpleCommitService.getInstance(e.project) ?: return
        val state = service.state
        e.presentation.icon = state.getIcon()
        e.presentation.isEnabled = state.canBeProcessed(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        println("Simple commit action fired.")
        val service = SimpleCommitService.getInstance(e.project) ?: return
        service.state.process(e)
    }

}