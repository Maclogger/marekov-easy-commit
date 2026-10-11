package sk.rucki.simple_commit.states

import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.Icon

abstract class AbstractState {
    abstract fun process(event: AnActionEvent): AbstractState
    open fun canBeProcessed(e: AnActionEvent): Boolean {
        return true
    }
    abstract fun getIcon(): Icon
}