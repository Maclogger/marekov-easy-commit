package sk.rucki.simple_commit.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBFont
import sk.rucki.simple_commit.icons.SimpleCommitIcons
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.ui.dsl.builder.toNullableProperty

/** Settings → Tools → Marekov Easy Commit */
class SimpleCommitConfigurable : BoundConfigurable("Marekov Easy Commit") {

    private val settings = SimpleCommitSettings.getInstance().state
    private lateinit var promptArea: JBTextArea

    override fun createPanel(): DialogPanel = panel {
        row {
            icon(SimpleCommitIcons.MASCOT)
            panel {
                row { label("Marekov Easy Commit").applyToComponent { font = JBFont.h1().asBold() } }
                row { comment("One click, one commit message. Written by basic Claude Subscription.") }
            }
        }
        separator()
        row("Model:") {
            comboBox(ClaudeModel.entries, textListCellRenderer { it?.displayName })
                .bindItem(settings::model.toNullableProperty())
        }
        row("Timeout (seconds):") {
            intTextField(1..600).bindIntText(settings::timeoutSeconds)
        }
        row("Max diff characters:") {
            intTextField(1_000..1_000_000).bindIntText(settings::maxDiffChars)
                .comment("Longer diffs are cut off before they are sent to Claude.")
        }
        row("System prompt:") {}
        row {
            promptArea = textArea()
                .bindText(settings::systemPrompt)
                .rows(10)
                .align(Align.FILL)
                .validationOnApply { if (it.text.isBlank()) error("The prompt must not be empty.") else null }
                .component
        }.resizableRow()
        row {
            link("Reset prompt to default") {
                // Fills the field only; it is saved with Apply / OK like any other edit
                promptArea.text = DEFAULT_SYSTEM_PROMPT
            }
        }
    }
}
