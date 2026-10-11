package sk.rucki.simple_commit.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

const val DEFAULT_SYSTEM_PROMPT = """You write git commit messages in the Conventional Commits format.

Structure:
<type>(<scope>): <summary>
- <change>
- <change>

Rules:
- type: one of feat, fix, refactor, perf, docs, style, test, build, ci, chore.
- scope: a short lowercase name of the affected area (module, feature or component), e.g. lang, auth, ui.
  Leave out the parentheses when the change has no single clear area.
- summary: imperative mood, lowercase start, no period at the end; the whole first line at most 100 characters.
- After a blank line, 1 to 5 bullet points starting with "- " that describe the important changes.
- Describe only what is in the diff, do not invent anything.
- Output ONLY the commit message. No explanations, no quotes, no Markdown code blocks.

Example:
feat(lang): add Polish language
- add Polish translations for all UI texts
- add Polish to the language picker
- fall back to English for missing keys"""

/**
 * Plugin settings, shared by all projects. The IDE saves them to simpleCommit.xml in its config directory.
 */
@Service(Service.Level.APP)
@State(name = "SimpleCommitSettings", storages = [Storage("simpleCommit.xml")])
class SimpleCommitSettings : PersistentStateComponent<SimpleCommitSettings.State> {

    data class State(
        var systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
        var model: ClaudeModel = ClaudeModel.HAIKU,
        var timeoutSeconds: Int = 30,
        var maxDiffChars: Int = 100_000,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): SimpleCommitSettings = service()
    }
}
