package sk.rucki.simple_commit

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import sk.rucki.simple_commit.settings.ClaudeModel
import sk.rucki.simple_commit.settings.SimpleCommitConfigurable
import sk.rucki.simple_commit.settings.SimpleCommitSettings

class SimpleCommitSettingsTest : BasePlatformTestCase() {

    fun `test settings survive saving and loading`() {
        val saved = SimpleCommitSettings.State(systemPrompt = "Be brief.", model = ClaudeModel.SONNET, timeoutSeconds = 45, maxDiffChars = 5_000)

        val loaded = XmlSerializer.deserialize(XmlSerializer.serialize(saved), SimpleCommitSettings.State::class.java)

        assertEquals(saved, loaded)
    }

    fun `test loading new settings keeps the same state object`() {
        val settings = SimpleCommitSettings.getInstance()
        val stateBefore = settings.state
        try {
            settings.loadState(SimpleCommitSettings.State(model = ClaudeModel.OPUS, timeoutSeconds = 99))

            assertSame(stateBefore, settings.state)
            assertEquals(ClaudeModel.OPUS, stateBefore.model)
            assertEquals(99, stateBefore.timeoutSeconds)
        } finally {
            settings.loadState(SimpleCommitSettings.State())   // the service is shared by all tests
        }
    }

    fun `test settings page opens and shows the stored values unmodified`() {
        val configurable = SimpleCommitConfigurable()
        try {
            assertNotNull(configurable.createComponent())
            configurable.reset()
            assertFalse(configurable.isModified)
        } finally {
            configurable.disposeUIResources()
        }
    }
}
