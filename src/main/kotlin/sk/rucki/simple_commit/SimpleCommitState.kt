package sk.rucki.simple_commit

import javax.swing.Icon

enum class SimpleCommitState(val icon: Icon) {
    DISABLED(SimpleCommitIcons.DISABLED),
    AVAILABLE(SimpleCommitIcons.AVAILABLE),
    GENERATING(SimpleCommitIcons.GENERATING),
    CREATED(SimpleCommitIcons.CREATED),
    ERROR(SimpleCommitIcons.ERROR);
    fun canGenerateNewMessage() = this == AVAILABLE || this == ERROR || this == CREATED
}