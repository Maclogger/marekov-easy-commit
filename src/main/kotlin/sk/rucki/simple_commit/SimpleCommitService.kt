package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.AnActionEvent
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
import sk.rucki.simple_commit.states.AbstractState
import sk.rucki.simple_commit.states.IdleState

@Service(Service.Level.PROJECT)
class SimpleCommitService(private val coroutineScope: CoroutineScope) {

    companion object {
        fun getInstance(project: Project?): SimpleCommitService? = project?.service()
    }

    var state: AbstractState = IdleState()
        get() = field

    private var job: Job? = null

    fun actionPerformed(e: AnActionEvent) {
        state = state.process(e, this)
    }

    fun startCommitMessageGeneration(commitMessageArea: CommitMessageI) {
        cancelJob()
        job = coroutineScope.launch {
            val result = try {
                val diff = withContext(Dispatchers.IO) { ChangesLoader }
                val message = generator.generate(diff)
                withContext(Dispatchers.EDT) { commitMessage.setCommitMessage(message) }
                CreatedState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ErrorState(e.message)
            }
            withContext(Dispatchers.EDT) {   // a cancelled job never gets in here
                state = result
                job = null
            }
        }

    }

    fun cancelJob() {
        job?.cancel()
        job = null
    }


}