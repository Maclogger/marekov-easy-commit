package sk.rucki.simple_commit

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import sk.rucki.simple_commit.states.CreatedState
import sk.rucki.simple_commit.states.ErrorState
import sk.rucki.simple_commit.states.GeneratingState
import sk.rucki.simple_commit.states.IdleState

/**
 * Tests the whole feature the way the IDE drives it: update() and actionPerformed() of the action,
 * the state machine, the background generation and the result written into the commit message field.
 */
class SimpleCommitFeatureTest : BasePlatformTestCase() {

    private val action = SimpleCommitAction()
    private lateinit var scope: CoroutineScope
    private lateinit var service: SimpleCommitService
    private lateinit var commitMessageArea: FakeCommitMessageArea

    override fun setUp() {
        super.setUp()
        // A fresh service for every test: the light test project is shared between tests
        scope = CoroutineScope(SupervisorJob())
        service = SimpleCommitService(project, scope)
        project.replaceService(SimpleCommitService::class.java, service, testRootDisposable)
        commitMessageArea = FakeCommitMessageArea()
    }

    override fun tearDown() {
        try {
            scope.cancel()
        } finally {
            super.tearDown()
        }
    }

    // --- update() ---

    fun `test button is disabled when no change is checked`() {
        val event = clickEvent(changes = emptyList())

        action.update(event)

        assertFalse(event.presentation.isEnabled)
        assertSame(SimpleCommitIcons.IDLE, event.presentation.icon)
    }

    fun `test button is enabled when a change is checked`() {
        val event = clickEvent()

        action.update(event)

        assertTrue(event.presentation.isEnabled)
        assertSame(SimpleCommitIcons.IDLE, event.presentation.icon)
    }

    fun `test button stays enabled while generating so it can cancel`() {
        service.generator = HangingGenerator()
        click()

        val event = clickEvent(changes = emptyList())
        action.update(event)

        assertTrue(event.presentation.isEnabled)
        assertSame(SimpleCommitIcons.GENERATING, event.presentation.icon)
    }

    // --- actionPerformed() ---

    fun `test click generates the message and writes it into the commit window`() {
        val generator = RecordingGenerator("feat: add greeting")
        service.generator = generator

        click(changes = listOf(modifiedFileChange(project, "src/Greeting.kt", "\n", "fun greet() = \"hi\"\n")))
        assertInstanceOf(service.state, GeneratingState::class.java)

        waitForState<CreatedState>()
        assertEquals("feat: add greeting", commitMessageArea.message)
        assertTrue(generator.receivedDiff!!, generator.receivedDiff!!.contains("+fun greet() = \"hi\""))
        assertSame(SimpleCommitIcons.CREATED, updatedPresentationIcon())
    }

    fun `test click with no checked change does nothing`() {
        service.generator = RecordingGenerator("unused")

        click(changes = emptyList())

        assertInstanceOf(service.state, IdleState::class.java)
        assertNull(commitMessageArea.message)
    }

    fun `test click outside the commit message field does nothing`() {
        service.generator = RecordingGenerator("unused")

        service.actionPerformed(commitWindowEvent(action, project, fakeCommitWorkflowUi(listOf(someChange())), null))

        assertInstanceOf(service.state, IdleState::class.java)
    }

    fun `test failing generation shows an error notification`() {
        service.generator = FailingGenerator("claude is not installed")
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                notifications += notification
            }
        })

        click()

        val error = waitForState<ErrorState>()
        assertEquals("claude is not installed", error.errorMessage)
        assertEquals("The old message is cleared, nothing new is written", "", commitMessageArea.message)
        assertSame(SimpleCommitIcons.ERROR, updatedPresentationIcon())

        PlatformTestUtil.waitWithEventsDispatching("Error notification was not shown", { notifications.isNotEmpty() }, 10)
        val notification = notifications.single()
        assertEquals(NotificationType.ERROR, notification.type)
        assertEquals("claude is not installed", notification.content)
    }

    fun `test only binary changes show an error instead of calling claude`() {
        val generator = RecordingGenerator("unused")
        service.generator = generator

        click(changes = listOf(modifiedFileChange(project, "logo.png", "old", "new")))

        val error = waitForState<ErrorState>()
        assertEquals("The checked files have no text changes to describe.", error.errorMessage)
        assertNull("Claude must not be called with an empty diff", generator.receivedDiff)
    }

    fun `test second click while generating cancels the generation`() {
        val generator = HangingGenerator()
        service.generator = generator

        click()
        PlatformTestUtil.waitWithEventsDispatching("Generation did not start", { generator.started.isCompleted }, 10)

        click()

        assertInstanceOf(service.state, IdleState::class.java)
        PlatformTestUtil.waitWithEventsDispatching("Generation was not cancelled", { generator.wasCancelled }, 10)
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertTrue("A cancelled job must not change the state", service.state is IdleState)
        assertEquals("The old message is cleared, nothing new is written", "", commitMessageArea.message)
    }

    fun `test click after a created message generates a new one`() {
        service.generator = RecordingGenerator("first")
        click()
        waitForState<CreatedState>()

        service.generator = RecordingGenerator("second")
        click()

        waitForState<CreatedState>()
        assertEquals("second", commitMessageArea.message)
    }

    fun `test click after an error tries again`() {
        service.generator = FailingGenerator("boom")
        click()
        waitForState<ErrorState>()

        service.generator = RecordingGenerator("recovered")
        click()

        waitForState<CreatedState>()
        assertEquals("recovered", commitMessageArea.message)
    }

    // --- helpers ---

    private fun someChange(): Change = modifiedFileChange(project, "src/Foo.kt", "old\n", "new\n")

    private fun clickEvent(changes: List<Change> = listOf(someChange())) =
        commitWindowEvent(action, project, fakeCommitWorkflowUi(changes), commitMessageArea)

    private fun click(changes: List<Change> = listOf(someChange())) {
        action.actionPerformed(clickEvent(changes))
    }

    private fun updatedPresentationIcon() = clickEvent().also { action.update(it) }.presentation.icon

    /** The generation finishes on the EDT, so keep dispatching events until the state arrives. */
    private inline fun <reified T> waitForState(): T {
        PlatformTestUtil.waitWithEventsDispatching(
            { "Expected ${T::class.simpleName}, but the state is ${service.state::class.simpleName}" },
            { service.state is T },
            10,
        )
        return service.state as T
    }
}
