package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

class IdleState : AbstractState() {

    override fun process(
        event: AnActionEvent,
        service: SimpleCommitService
    ): AbstractState {
        return startGeneratingNewCommitMessage(event, service)
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.IDLE
    }

}