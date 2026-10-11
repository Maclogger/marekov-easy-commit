package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import sk.rucki.simple_commit.SimpleCommitIcons
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import javax.swing.Icon

class IdleState() : AbstractState() {

    private val changesLoader: ChangesLoader
        get() = ChangesLoader()

    override fun process(event: AnActionEvent): AbstractState {
        // TODO: start the coroutine job in service
        return GeneratingState()
    }

    override fun canBeProcessed(e: AnActionEvent): Boolean {
        val changes = changesLoader.loadChanges(e)
        return changes.isNotEmpty()
    }

    override fun getIcon(): Icon {
        return SimpleCommitIcons.IDLE
    }

}