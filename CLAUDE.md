# ReBeatBox

A cyber/glitch-styled Java desktop music application. Import MIDI files for auto-playback with falling-notes visualization, trigger notes live via keyboard and drum pad, all wrapped in a cyberpunk aesthetic powered by Radiance (Swing).

## Project State

See: `.planning/PROJECT.md` and `.planning/STATE.md`

**Phase 1 — Foundation:** App shell + MIDI engine + transport controls. → `/gsd-discuss-phase 1`

## Tech Stack

- Java 17 + Gradle
- Radiance 8.5.0 (Swing theming + animation + components)
- javax.sound.midi (JDK built-in MIDI engine)
- Gervill synthesizer (SoundFont-based audio)

## GSD Workflow

This project uses the Get Shit Done (GSD) planning workflow. All planning artifacts live in `.planning/`.

- `/gsd-progress` — Check project status
- `/gsd-discuss-phase N` — Start a phase
- `/gsd-plan-phase N` — Plan a phase
- `/gsd-execute-phase` — Execute planned work

### GSD Toolchain

The GSD CLI entry point is `node .claude/get-shit-done/bin/gsd-tools.cjs`. The `gsd-sdk` binary is NOT installed globally — always use the local Node script. GSD MUST NOT pollute the user's global Claude Code installation; all GSD state lives in `.planning/` and `.claude/get-shit-done/` within this repo.

## Environment Preferences

- **Shell:** Use PowerShell (`pwsh`) for all command execution. Do NOT use Bash unless the operation is genuinely impossible in PowerShell.
- **Available tools:** `gh` (GitHub CLI), `rg` (ripgrep) are available on PATH.
- **Search:** Use Grep tool (wraps `rg`) for content search, Glob for file patterns. Never use `Bash(grep ...)` or `Bash(find ...)`.
