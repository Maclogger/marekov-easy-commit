package sk.rucki.simple_commit

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import sk.rucki.simple_commit.settings.DEFAULT_SYSTEM_PROMPT
import sk.rucki.simple_commit.settings.SimpleCommitSettings
import sk.rucki.simple_commit.states.CreatedState
import sk.rucki.simple_commit.states.ErrorState
import java.io.File

/**
 * Calls the REAL `claude`: the plugin never reads the branch itself, Claude Code tells it to the model
 * when it runs in a git repo. Only a real call shows that this still works after a Claude Code update.
 *
 * Costs one Haiku call and needs a logged-in `claude`, so it runs only on request:
 * `SIMPLE_COMMIT_REAL_CLAUDE=1 ./gradlew test --tests '*CurrentBranchTest'`
 */
class CurrentBranchTest : BasePlatformTestCase() {

    private val BRANCH = "ABC-123-login-form"

    private lateinit var projectDir: File
    private lateinit var scope: CoroutineScope
    private lateinit var service: SimpleCommitService
    private lateinit var originalPrompt: String

    override fun setUp() {
        super.setUp()
        projectDir = File(project.basePath!!).apply { mkdirs() }
        git("init", "-q", "-b", BRANCH)
        git("-c", "user.email=test@test", "-c", "user.name=test", "commit", "-q", "--allow-empty", "-m", "init")

        // The real settings service, as the user's settings page saves it
        val settings = SimpleCommitSettings.getInstance().state
        originalPrompt = settings.systemPrompt
        settings.systemPrompt = DEFAULT_SYSTEM_PROMPT +
                "\n- Start the first line with the current git branch name in square brackets, exactly as git shows it."

        // The real generator, wired by the service itself
        scope = CoroutineScope(SupervisorJob())
        service = SimpleCommitService(project, scope)
        project.replaceService(SimpleCommitService::class.java, service, testRootDisposable)
    }

    override fun tearDown() {
        try {
            scope.cancel()
            SimpleCommitSettings.getInstance().state.systemPrompt = originalPrompt
            File(projectDir, ".git").deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    override fun shouldRunTest(): Boolean =
        System.getenv("SIMPLE_COMMIT_REAL_CLAUDE") == "1" && !SystemInfo.isWindows && super.shouldRunTest()

    fun `test the message starts with the current branch`() {
        val area = FakeCommitMessageArea()
        val change = modifiedFileChange(project, "src/Login.kt", "val user = null\n", "val user = loadUser()\n")

        val action = SimpleCommitAction()
        action.actionPerformed(commitWindowEvent(action, project, fakeCommitWorkflowUi(listOf(change)), area))
        PlatformTestUtil.waitWithEventsDispatching(
            { "Generation did not finish, the state is ${service.state}" },
            { service.state is CreatedState || service.state is ErrorState },
            60,
        )

        (service.state as? ErrorState)?.let { fail("Generation failed: ${it.errorMessage}") }
        val firstLine = area.message!!.lines().first()
        assertTrue("The branch is missing in: ${area.message}", firstLine.contains(BRANCH))
    }

    private fun git(vararg args: String): String {
        val process = ProcessBuilder("git", *args).directory(projectDir).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText().trim()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed: $output" }
        return output
    }
}
