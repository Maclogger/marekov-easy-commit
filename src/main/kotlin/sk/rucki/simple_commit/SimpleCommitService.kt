package sk.rucki.simple_commit

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class SimpleCommitService {

    @Volatile
    var state: SimpleCommitState = SimpleCommitState.DISABLED


    fun selectedFilesCountChanged(newCount: Int) {
        val atLeastOneFileSelected = newCount >= 1
        if (atLeastOneFileSelected) {
            this.state = SimpleCommitState.AVAILABLE
            return
        }
        this.state = SimpleCommitState.DISABLED
    }

    fun generateCommitMessage() {
        if (!this.canGenerateCommitMessage()) {
            throw IllegalStateException("Cannot generate commit message")
        }
    }

    private fun canGenerateCommitMessage(): Boolean {
        return this.state === SimpleCommitState.AVAILABLE
    }

}