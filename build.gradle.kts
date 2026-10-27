import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    // IntelliJ 2025.3 runs on Java 21; build with it regardless of JAVA_HOME
    jvmToolchain(21)
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.6.1")
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.platform.vcs.impl")
    }
}

intellijPlatform {
    pluginConfiguration {
        // "What's New" on the Marketplace page, taken from CHANGELOG.md for the current version
        changeNotes = provider {
            with(changelog) {
                renderItem(getOrNull(project.version.toString()) ?: getUnreleased(), Changelog.OutputType.HTML)
            }
        }
        ideaVersion {
            sinceBuild = "253"
            // No untilBuild: the plugin stays installable in newer IDEs
        }
    }

    pluginVerification {
        // The latest 2025.3.x release of the most important IDEs (each is a ~1 GB download, cached afterwards)
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2025.3.6.1")
            create(IntelliJPlatformType.PhpStorm, "2025.3.6.1")
            create(IntelliJPlatformType.WebStorm, "2025.3.6.1")
        }
    }

    publishing {
        // Token from https://plugins.jetbrains.com/author/me/tokens, used by ./gradlew publishPlugin
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks.buildPlugin {
    archiveBaseName = "marekov-easy-commit"
}
