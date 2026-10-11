package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommitMessageI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sk.rucki.simple_commit.diff_loading.ChangesLoader
import sk.rucki.simple_commit.generators.FakeCommitMessageGenerator
import sk.rucki.simple_commit.generators.ICommitMessageGenerator
import sk.rucki.simple_commit.states.AbstractState
import sk.rucki.simple_commit.states.CreatedState
import sk.rucki.simple_commit.states.ErrorState
import sk.rucki.simple_commit.states.IdleState
import kotlin.coroutines.cancellation.CancellationException

@Service(Service.Level.PROJECT)
class SimpleCommitService(private val coroutineScope: CoroutineScope) {

    companion object {
        fun getInstance(project: Project?): SimpleCommitService? = project?.service()
    }

    private var state: AbstractState = IdleState()
        get() = field

    val generator: ICommitMessageGenerator = FakeCommitMessageGenerator()

    private var job: Job? = null

    fun actionPerformed(e: AnActionEvent) {
        val newState = state.process(e, this)
        moveToAnotherState(newState, e)
    }

    fun startCommitMessageGeneration(commitMessageArea: CommitMessageI, event: AnActionEvent) {
        cancelJob()
        job = coroutineScope.launch {
            val finalState = try {
                val diff = withContext(Dispatchers.IO) {
                    ChangesLoader.loadDiff(event)
                }
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
                moveToAnotherState(finalState, event)
                job = null
            }
        }
    }

    fun cancelJob() {
        job?.cancel()
        job = null
    }

    fun moveToAnotherState(newState: AbstractState, event: AnActionEvent) {
        state = newState
        state.initialize(event, this)
    }

}