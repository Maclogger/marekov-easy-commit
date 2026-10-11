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
import java.io.StringWriter

object ChangesLoader {

    /**
     * Reads the changes checked in the commit window. Cheap, safe to call from update().
     */
    fun loadChanges(e: AnActionEvent): List<Change> {
        val workflowUi = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return emptyList()
        return loadVersionedSelectedChanges(workflowUi) + loadUnversionedSelectedChanges(workflowUi)
    }

    /**
     * Builds a unified diff of the changes. Reads file contents, so never call it on the EDT.
     */
    fun buildDiff(project: Project, changes: List<Change>): String {
        val basePath = project.basePath ?: return ""
        val textChanges = changes.filterNot { change ->
            listOfNotNull(change.beforeRevision, change.afterRevision).any { IdeaTextPatchBuilder.isBinaryRevision(it) }
        }
        val patches = IdeaTextPatchBuilder.buildPatch(project, textChanges, basePath, false)

        val writer = StringWriter()
        UnifiedDiffWriter.write(project, patches, writer, "\n", CommitContext())
        return writer.toString()
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
