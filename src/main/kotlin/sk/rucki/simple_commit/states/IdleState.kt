package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import kotlinx.coroutines.CoroutineScope
import sk.rucki.simple_commit.SimpleCommitIcons
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import sk.rucki.simple_commit.generators.ICommitMessageGenerator
import javax.swing.Icon

class IdleState(val commitMessageGenerator: ICommitMessageGenerator, val coroutineScope: CoroutineScope) : AbstractState() {

    private val changesLoader: ChangesLoader
        get() = ChangesLoader()

    override fun process(event: AnActionEvent): AbstractState {
        // TODO: start the generation job
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