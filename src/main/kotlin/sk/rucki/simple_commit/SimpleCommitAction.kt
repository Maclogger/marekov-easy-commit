package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsDataKeys

internal class SimpleCommitAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val service = SimpleCommitService.getInstance(e.project) ?: return
        val changes = ChangesLoader.loadChanges(e)
        service.selectedFilesCountChanged(changes.size)

        e.presentation.isEnabled = service.state.canGenerateNewMessage()
        e.presentation.icon = service.state.icon
    }

    override fun actionPerformed(e: AnActionEvent) {
        println("Simple commit action fired.")

        val changes = ChangesLoader.loadChanges(e)
        setTheTextOfTheCommitWindowTextArea(e, "Included changes: ${changes.size}")
    }

    private fun setTheTextOfTheCommitWindowTextArea(e: AnActionEvent, text: String) {
        val commitMessageControlTextArea = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (commitMessageControlTextArea === null) {
            println("Commit message control text area is null.")
            return
        }
        commitMessageControlTextArea.setCommitMessage(text)
    }

}