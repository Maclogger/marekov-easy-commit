package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.SimpleCommitIcons
import javax.swing.Icon

class ErrorState : AbstractState() {
    override fun process(event: AnActionEvent): AbstractState {
        TODO("Not yet implemented")
    }
    override fun getIcon(): Icon {
        return SimpleCommitIcons.ERROR
    }
}