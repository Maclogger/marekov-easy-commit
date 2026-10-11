package sk.rucki.simple_commit.states

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import javax.swing.Icon

class ErrorState(val errorMessage: String?) : AbstractState() {

    override fun initialize(service: SimpleCommitService) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Marekov Commit")
            .createNotification(
                "Generation of simple commit message failed",
                errorMessage ?: "Unknown error",
                NotificationType.ERROR,
            )
            .notify(service.project)
    }

    override fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        return this.startGeneratingNewCommitMessage(event, service)
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.ERROR
    }
}
