package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import sk.rucki.simple_commit.SimpleCommitService
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import javax.swing.Icon

abstract class AbstractState {
    /** Called once when the service enters this state, on the EDT. */
    open fun initialize(service: SimpleCommitService) {
    }

    /** Called on click, on the EDT. Returns the next state. */
    abstract fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState

    /** Called from update() on a background thread, must be fast. */
    open fun canBeProcessed(e: AnActionEvent): Boolean {
        return ChangesLoader.loadChanges(e).isNotEmpty()
    }

    abstract fun getIcon(): Icon

    protected fun startGeneratingNewCommitMessage(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        val commitMessageArea = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) ?: return this
        val changes = ChangesLoader.loadChanges(event)
        if (changes.isEmpty()) return this

        // Clear the old message only when a new one is really being generated
        commitMessageArea.setCommitMessage("")

        service.startCommitMessageGeneration(changes, commitMessageArea)
        return GeneratingState()
    }
}
