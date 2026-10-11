package sk.rucki.simple_commit

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.vcs.commit.CommitWorkflowUi

class ChangesLoader {
    companion object {
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
            return workflowUi.getIncludedUnversionedFiles().map {filePath ->
                Change(null, CurrentContentRevision.create(filePath))
            }
        }

        private fun loadVersionedSelectedChanges(workflowUi: CommitWorkflowUi): List<Change> {
            return workflowUi.getIncludedChanges()
        }
    }
}