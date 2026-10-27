package sk.rucki.simple_commit.states

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.SimpleCommitService
import sk.rucki.simple_commit.settings.SimpleCommitConfigurable
import javax.swing.Icon

class ErrorState(val errorMessage: String?) : AbstractState() {

    override fun initialize(service: SimpleCommitService) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Marekov Easy Commit")
            .createNotification(
                "Commit message generation failed",
                errorMessage ?: "Unknown error",
                NotificationType.ERROR,
            )
            .addAction(NotificationAction.createSimpleExpiring("Open settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(service.project, SimpleCommitConfigurable::class.java)
            })
            .notify(service.project)
    }

    override fun process(event: AnActionEvent, service: SimpleCommitService): AbstractState {
        return this.startGeneratingNewCommitMessage(event, service)
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.ERROR
    }
}
