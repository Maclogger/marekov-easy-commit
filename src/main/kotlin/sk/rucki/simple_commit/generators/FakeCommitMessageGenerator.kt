package sk.rucki.simple_commit.generators

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class FakeCommitMessageGenerator : ICommitMessageGenerator {
    override suspend fun generate(diff: String): String {
        delay(3_000.milliseconds)
        return "feat: fake commit message"
    }
}