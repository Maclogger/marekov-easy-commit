package sk.rucki.simple_commit.generators

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.EnvironmentUtil
import java.io.File

/** Finds `claude`: first on the PATH, then in the places where its installers usually put it. */
fun findClaude(): String? {
    val names = if (SystemInfo.isWindows) listOf("claude.exe", "claude.cmd") else listOf("claude")
    val dirs = pathDirs() + knownDirs()

    for (name in names) {
        for (dir in dirs) {
            val file = File(dir, name)
            if (file.canExecute()) return file.path
        }
    }
    return null
}

/** The PATH of the user's shell (an IDE started from the desktop may have a different PATH). */
private fun pathDirs(): List<String> {
    val path = EnvironmentUtil.getValue("PATH") ?: System.getenv("PATH") ?: return emptyList()
    return path.split(File.pathSeparator)
}

private fun knownDirs(): List<String> {
    val home = System.getProperty("user.home")

    if (SystemInfo.isWindows) {
        val appData = System.getenv("APPDATA")
        val localAppData = System.getenv("LOCALAPPDATA")
        return listOf(
            "$home/.local/bin",                      // native installer
            "$appData/npm",                          // npm install -g
            "$localAppData/Microsoft/WinGet/Links",  // winget
            "$home/scoop/shims",                     // scoop
        )
    }

    return listOf(
        "$home/.local/bin",       // native installer
        "$home/.claude/local",    // older local install
        "$home/.npm-global/bin",  // npm with a custom prefix
        "/opt/homebrew/bin",      // Homebrew on macOS
        "/usr/local/bin",
        "/usr/bin",
    )
}
