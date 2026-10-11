package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.SimpleCommitService
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import javax.swing.Icon

class CreatedState : AbstractState() {

    override fun process(
        event: AnActionEvent,
        service: SimpleCommitService
    ): AbstractState {
        return startGeneratingNewCommitMessage(event, service)
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.CREATED
    }
}