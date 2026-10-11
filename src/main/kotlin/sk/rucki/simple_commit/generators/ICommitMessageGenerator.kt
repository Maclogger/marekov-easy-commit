package sk.rucki.simple_commit.generators

interface ICommitMessageGenerator {
    suspend fun generate(diff: String): String
}