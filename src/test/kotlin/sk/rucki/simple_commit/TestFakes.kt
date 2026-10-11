package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommitMessageI
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.TestActionEvent
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.vcsUtil.VcsUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import sk.rucki.simple_commit.generators.ICommitMessageGenerator
import java.lang.reflect.Proxy

/** Records the text the plugin writes into the commit message field. */
class FakeCommitMessageArea : CommitMessageI {
    var message: String? = null

    override fun setCommitMessage(currentDescription: String?) {
        message = currentDescription
    }
}

/**
 * A commit window that only knows which changes are checked.
 * Any other call fails the test, so we notice when the plugin starts using more of the API.
 */
fun fakeCommitWorkflowUi(
    includedChanges: List<Change> = emptyList(),
    includedUnversionedFiles: List<FilePath> = emptyList(),
): CommitWorkflowUi =
    Proxy.newProxyInstance(CommitWorkflowUi::class.java.classLoader, arrayOf(CommitWorkflowUi::class.java)) { proxy, method, args ->
        when (method.name) {
            "getIncludedChanges" -> includedChanges
            "getIncludedUnversionedFiles" -> includedUnversionedFiles
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.get(0)
            "toString" -> "FakeCommitWorkflowUi"
            else -> throw UnsupportedOperationException("FakeCommitWorkflowUi.${method.name}")
        }
    } as CommitWorkflowUi

/** Creates an action event as if the button was clicked in the commit window. */
fun commitWindowEvent(
    action: AnAction,
    project: Project,
    workflowUi: CommitWorkflowUi?,
    commitMessageArea: CommitMessageI?,
): AnActionEvent {
    val dataContext = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(VcsDataKeys.COMMIT_WORKFLOW_UI, workflowUi)
        .add(VcsDataKeys.COMMIT_MESSAGE_CONTROL, commitMessageArea)
        .build()
    return TestActionEvent.createTestEvent(action, dataContext)
}

/** A modification of a text file, without touching the disk. */
fun modifiedFileChange(project: Project, relativePath: String, before: String, after: String): Change {
    val filePath = VcsUtil.getFilePath("${project.basePath}/$relativePath", false)
    return Change(
        SimpleContentRevision(before, filePath, "before"),
        SimpleContentRevision(after, filePath, "after"),
    )
}

/** Returns a fixed message and remembers the diff it was given. */
class RecordingGenerator(private val message: String) : ICommitMessageGenerator {
    var receivedDiff: String? = null

    override suspend fun generate(diff: String): String {
        receivedDiff = diff
        return message
    }
}

class FailingGenerator(private val errorMessage: String) : ICommitMessageGenerator {
    override suspend fun generate(diff: String): String {
        throw IllegalStateException(errorMessage)
    }
}

/** Never finishes on its own, so the test can cancel it. */
class HangingGenerator : ICommitMessageGenerator {
    val started = CompletableDeferred<Unit>()

    @Volatile
    var wasCancelled = false

    override suspend fun generate(diff: String): String {
        started.complete(Unit)
        try {
            awaitCancellation()
        } finally {
            wasCancelled = true
        }
    }
}
