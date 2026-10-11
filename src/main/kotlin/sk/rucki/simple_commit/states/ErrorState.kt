package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

class ErrorState : AbstractState() {
    override fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        TODO("Not yet implemented")
    }
    override fun getIcon(): Icon {
        return SimpleCommitIcons.ERROR
    }
}