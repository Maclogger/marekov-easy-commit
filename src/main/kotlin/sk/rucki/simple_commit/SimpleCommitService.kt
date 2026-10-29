package sk.rucki.simple_commit

import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.changes.Change
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import sk.rucki.simple_commit.generators.ClaudeCliCommitMessageGenerator
import sk.rucki.simple_commit.generators.ICommitMessageGenerator
import sk.rucki.simple_commit.states.AbstractState
import sk.rucki.simple_commit.states.CreatedState
import sk.rucki.simple_commit.states.ErrorState
import sk.rucki.simple_commit.states.IdleState
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class SimpleCommitService(
    val project: Project,
    private val coroutineScope: CoroutineScope,
) {

    companion object {
        fun getInstance(project: Project?): SimpleCommitService? = project?.service()
    }

    @Volatile
    var state: AbstractState = IdleState()
        private set

    @VisibleForTesting
    internal var generator: ICommitMessageGenerator = ClaudeCliCommitMessageGenerator()

    private var job: Job? = null

    fun actionPerformed(e: AnActionEvent) {
        moveToAnotherState(state.process(e, this))
    }

    fun startCommitMessageGeneration(changes: List<Change>, commitMessageArea: CommitMessageI) {
        cancelJob()
        job = coroutineScope.launch {
            val finalState = try {
                val diff = withContext(Dispatchers.IO) { ChangesLoader.buildDiff(project, changes) }
                // Empty when only binary files are checked; Claude would make up a message
                if (diff.isBlank()) throw IllegalStateException("The checked files have no text changes to describe.")
                val generatedCommitMessage = generator.generate(diff)
                withContext(Dispatchers.EDT) {
                    commitMessageArea.setCommitMessage(generatedCommitMessage)
                }
                CreatedState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ErrorState(e.message)
            }
            withContext(Dispatchers.EDT) {   // a canceled job never gets in here
                job = null
                moveToAnotherState(finalState)
            }
        }
    }

    fun cancelJob() {
        job?.cancel()
        job = null
    }

    private fun moveToAnotherState(newState: AbstractState) {
        if (newState === state) return
        state = newState
        state.initialize(this)
        // Ask the toolbars to call update() now, so the icon changes immediately
        ActivityTracker.getInstance().inc()
    }
}
