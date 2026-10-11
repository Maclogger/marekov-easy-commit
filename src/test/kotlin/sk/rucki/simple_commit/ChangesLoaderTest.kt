package sk.rucki.simple_commit

import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.SimpleContentRevision
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcsUtil.VcsUtil
import sk.rucki.simple_commit.diff_loading.ChangesLoader

class ChangesLoaderTest : BasePlatformTestCase() {

    private val action = SimpleCommitAction()

    fun `test loadChanges returns checked changes and unversioned files`() {
        val modified = modifiedFileChange(project, "src/Foo.kt", "old\n", "new\n")
        val unversioned = VcsUtil.getFilePath("${project.basePath}/src/New.kt", false)
        val event = commitWindowEvent(
            action, project,
            fakeCommitWorkflowUi(listOf(modified), listOf(unversioned)),
            FakeCommitMessageArea(),
        )

        val changes = ChangesLoader.loadChanges(event)

        assertEquals(2, changes.size)
        assertSame(modified, changes[0])
        assertNull("Unversioned file is a new file", changes[1].beforeRevision)
        assertEquals(unversioned, changes[1].afterRevision?.file)
    }

    fun `test loadChanges returns nothing outside the commit window`() {
        val event = commitWindowEvent(action, project, workflowUi = null, commitMessageArea = null)

        assertEmpty(ChangesLoader.loadChanges(event))
    }

    fun `test buildDiff contains removed and added lines with a relative path`() {
        val change = modifiedFileChange(project, "src/Foo.kt", "val a = 1\n", "val a = 2\n")

        val diff = ChangesLoader.buildDiff(project, listOf(change))

        assertTrue(diff, diff.contains("src/Foo.kt"))
        assertTrue(diff, diff.contains("-val a = 1"))
        assertTrue(diff, diff.contains("+val a = 2"))
        assertFalse("Paths must be relative to the project", diff.contains(project.basePath!!))
    }

    fun `test buildDiff shows a new file as added lines`() {
        val filePath = VcsUtil.getFilePath("${project.basePath}/src/New.kt", false)
        val change = Change(null, SimpleContentRevision("fun hello() = 42\n", filePath, "after"))

        val diff = ChangesLoader.buildDiff(project, listOf(change))

        assertTrue(diff, diff.contains("src/New.kt"))
        assertTrue(diff, diff.contains("+fun hello() = 42"))
    }

    fun `test buildDiff of no changes is empty`() {
        assertEquals("", ChangesLoader.buildDiff(project, emptyList()).trim())
    }
}
