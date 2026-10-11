package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.SimpleCommitIcons
import javax.swing.Icon

class GeneratingState : AbstractState() {
    override fun process(event: AnActionEvent): AbstractState {
        // TODO: stop the coroutine job in service
        return IdleState()
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.GENERATING
    }
}