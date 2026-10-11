package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.VcsDataKeys
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

abstract class AbstractState {
    open fun initialize(event: AnActionEvent, service: SimpleCommitService) {
    }
    abstract fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState
    open fun canBeProcessed(e: AnActionEvent): Boolean {
        return true
    }
    abstract fun getIcon(): Icon

    protected fun getCommitMessageArea(event: AnActionEvent): CommitMessageI? {
        return event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
    }

    protected fun startGeneratingNewCommitMessage(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        val commitMessageArea = getCommitMessageArea(event) ?: return this
        service.startCommitMessageGeneration(commitMessageArea, event)
        return GeneratingState()
    }
}