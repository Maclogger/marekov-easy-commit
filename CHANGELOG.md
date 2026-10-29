<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Marekov Easy Commit Changelog

## Unreleased

## 1.0.1 - 2026-10-29

### Added

- Finds `claude` also outside the PATH, in the usual install locations of the native installer, npm, Homebrew, winget and Scoop
- Windows support: finds both `claude.exe` and npm's `claude.cmd`

### Fixed

- The system prompt and settings are passed to Claude in files, so line breaks and quotes in the prompt are no longer broken on Windows
- Checking only binary files shows an error instead of letting Claude make up a message for an empty diff
- An empty answer from Claude shows an error instead of an empty commit message
- A failure without any output from Claude now shows the exit code
- Changes on the settings page are no longer lost when the settings are reloaded while the page is open (e.g. by Settings Sync)

## 1.0.0 - 2026-10-27

### Added

- Button in the commit window that generates a commit message for the checked changes with Claude Code
- Messages in the Conventional Commits format with a short bullet point body
- Clicking the button again cancels a running generation
- Settings page (Settings | Tools | Marekov Easy Commit) for the model, system prompt, timeout and maximum diff size
- Error notification with a shortcut to the settings when the generation fails
