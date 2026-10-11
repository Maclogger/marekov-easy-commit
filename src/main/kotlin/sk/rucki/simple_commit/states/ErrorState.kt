package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

class ErrorState(val errorMessage: String?) : AbstractState() {

    override fun initialize(event: AnActionEvent, service: SimpleCommitService) {
        super.initialize(event, service)
        Messages.showErrorDialog(service.project, "Generation of simple commit message failed. ${errorMessage}", "Marekov Commit")
    }

    override fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        return this.startGeneratingNewCommitMessage(event, service)
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.ERROR
    }
}