package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import sk.rucki.simple_commit.generators.FakeCommitMessageGenerator
import sk.rucki.simple_commit.states.AbstractState
import sk.rucki.simple_commit.states.IdleState

@Service(Service.Level.PROJECT)
class SimpleCommitService(private val coroutineScope: CoroutineScope) {

    companion object {
        fun getInstance(project: Project?): SimpleCommitService? = project?.service()
    }

    var state: AbstractState = IdleState(FakeCommitMessageGenerator(), coroutineScope)
        get() = field

    private var job: Job? = null

    fun startJob() {
        if (job !== null) {
            job.cancel()
        }

        job = coroutineScope.launch {
            try {
                val diff = createDiffFromChanges()
                val generatedCommitMessage = commitMessageGenerator.generate(diff)
                withContext(Dispatchers.EDT) {
                    setTheTextOfTheCommitWindowTextArea(event, generatedCommitMessage)
                }
            } catch (e: CancellationException) {
                println("CancellationException caught")
                throw e
            } catch (e: Exception) {
                println("Exception caught: ${e.message}")
                return ErrorState()
            }
        }

    }


    fun setTheTextOfTheCommitWindowTextArea(e: AnActionEvent, text: String) {
        val commitMessageControlTextArea = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        if (commitMessageControlTextArea === null) {
            println("Commit message control text area is null.")
            return
        }
        commitMessageControlTextArea.setCommitMessage(text)
    }

}