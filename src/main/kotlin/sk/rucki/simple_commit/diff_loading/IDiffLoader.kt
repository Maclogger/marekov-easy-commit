package sk.rucki.simple_commit.diff_loading

import com.intellij.openapi.actionSystem.AnActionEvent

interface IDiffLoader {
    fun loadDiff(e: AnActionEvent): String
}