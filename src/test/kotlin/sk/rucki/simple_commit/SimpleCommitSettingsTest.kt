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
