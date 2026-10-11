package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

class GeneratingState : AbstractState() {
    override fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        service.cancelJob()
        return IdleState()
    }

    override fun canBeProcessed(e: AnActionEvent): Boolean {
        return true
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.GENERATING
    }
}
