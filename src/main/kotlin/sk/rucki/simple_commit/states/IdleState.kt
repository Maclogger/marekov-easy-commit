package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import javax.swing.Icon

class IdleState : AbstractState() {

    override fun process(
        event: AnActionEvent,
        service: SimpleCommitService
    ): AbstractState {
        return startGeneratingNewCommitMessage(event, service)
    }

    override fun canBeProcessed(e: AnActionEvent): Boolean {
        val changes = ChangesLoader.loadChanges(e)
        return changes.isNotEmpty()
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.IDLE
    }

}