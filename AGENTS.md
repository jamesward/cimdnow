# AGENTS.md: cimdnow

## Agent tooling

- Follow the `zen-of-projects` Skill (extract with `./sbt extractSkillsJars` into the gitignored `.kiro/skills/`); this file records only project-specific facts and exceptions.
- MCP server `sbt-mcp-cimdnow` (sbt-mcp) listens on `http://127.0.0.1:5100/`. Kiro uses the HTTP entry in `.kiro/settings/mcp.json`; start sbt first. Claude Code uses `.mcp.json`, which runs `.claude/sbt-mcp-stdio.sh` (approved in `.claude/settings.json`). That stdio bridge starts a foreground sbt in cloud sessions (`CLAUDE_CODE_REMOTE=true`), and locally only connects to an sbt you already started. Its tools are deferred: load them with ToolSearch (search `sbt-mcp-cimdnow`). Diagnostics go to `/tmp/sbt-mcp-stdio.log` and `/tmp/sbt-mcp-server.log`.
- Maintenance routine: `.factory/MAINTENANCE.md` (weekly), following the `zen-of-projects` Skill.

## Exceptions to the Skill

- Scala stays on 3.9.0: Scala Native 0.5.12 has no final `scala3lib_native0.5_3` for 3.10.0 (only 3.10.0-RC1..RC3). Bump Scala once Scala Native publishes it (CI's `nativeLink` fails with "Not found" otherwise).
