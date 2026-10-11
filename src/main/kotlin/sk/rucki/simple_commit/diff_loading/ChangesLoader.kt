package sk.rucki.simple_commit.diff_loading

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder
import com.intellij.openapi.diff.impl.patch.UnifiedDiffWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.vcs.commit.CommitWorkflowUi
import java.nio.file.Path
import java.io.StringWriter

class ChangesLoader : IDiffLoader {

    override fun loadDiff(e: AnActionEvent): String {
        val changes = loadChanges(e)
        return this.fromChangesToString(e.project, changes)
    }

    private fun fromChangesToString(project: Project?, changes: List<Change>): String {
        if (project === null) {
            throw IllegalStateException("Project is null in fromChangesToString")
        }

        val basePath = Path.of(project.basePath ?: return "")
        val patches = IdeaTextPatchBuilder.buildPatch(project, changes, basePath, false)

        val writer = StringWriter()
        UnifiedDiffWriter.write(project, patches, writer, "\n", CommitContext())
        return writer.toString()
    }


    fun loadChanges(e: AnActionEvent): List<Change> {
        val workflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
        if (workflowUi === null) {
            println("Commit workflow UI is null.")
            return emptyList()
        }

        val loadVersionedSelectedChanges = loadVersionedSelectedChanges(workflowUi)
        val loadUnversionedSelectedChanges = loadUnversionedSelectedChanges(workflowUi)

        return loadVersionedSelectedChanges + loadUnversionedSelectedChanges
    }

    private fun loadUnversionedSelectedChanges(workflowUi: CommitWorkflowUi): List<Change> {
        return workflowUi.getIncludedUnversionedFiles().map { filePath ->
            Change(null, CurrentContentRevision.create(filePath))
        }
    }

    private fun loadVersionedSelectedChanges(workflowUi: CommitWorkflowUi): List<Change> {
        return workflowUi.getIncludedChanges()
    }
}