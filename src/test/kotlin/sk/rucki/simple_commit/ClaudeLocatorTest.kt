package sk.rucki.simple_commit

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import sk.rucki.simple_commit.generators.findClaude
import java.io.File
import kotlin.io.path.createTempDirectory

/** Puts fake `claude` files into temp folders and checks which one is found. */
class ClaudeLocatorTest : BasePlatformTestCase() {

    private lateinit var first: File
    private lateinit var second: File

    override fun setUp() {
        super.setUp()
        first = createTempDirectory("first").toFile()
        second = createTempDirectory("second").toFile()
    }

    override fun tearDown() {
        try {
            first.deleteRecursively()
            second.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun `test finds claude in the first folder that has it`() {
        fakeFile(first, "claude")
        fakeFile(second, "claude")

        assertEquals(File(first, "claude").path, find())
    }

    fun `test looks in the next folder when the first one has no claude`() {
        fakeFile(second, "claude")

        assertEquals(File(second, "claude").path, find())
    }

    fun `test returns null when claude is nowhere`() {
        assertNull(find())
    }

    fun `test skips a claude that is not executable`() {
        if (SystemInfo.isWindows) return   // on Windows every file counts as executable

        fakeFile(first, "claude").setExecutable(false)
        fakeFile(second, "claude")

        assertEquals(File(second, "claude").path, find())
    }

    fun `test skips a folder named claude`() {
        File(first, "claude").mkdir()
        fakeFile(second, "claude")

        assertEquals(File(second, "claude").path, find())
    }

    fun `test windows prefers claude exe over claude cmd`() {
        fakeFile(first, "claude.cmd")
        fakeFile(second, "claude.exe")

        assertEquals(File(second, "claude.exe").path, find(isWindows = true))
    }

    fun `test windows falls back to claude cmd`() {
        fakeFile(second, "claude.cmd")

        assertEquals(File(second, "claude.cmd").path, find(isWindows = true))
    }

    fun `test windows ignores claude without an extension`() {
        fakeFile(first, "claude")

        assertNull(find(isWindows = true))
    }

    fun `test linux ignores claude exe`() {
        fakeFile(first, "claude.exe")

        assertNull(find(isWindows = false))
    }

    // --- helpers ---

    private fun find(isWindows: Boolean = false) = findClaude(listOf(first.path, second.path), isWindows)

    private fun fakeFile(dir: File, name: String): File =
        File(dir, name).apply {
            writeText("")
            setExecutable(true)
        }
}
