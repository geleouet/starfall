# Starfall MCP server

A thin wrapper over the debug API in `dev.starfall.debug`. Full documentation of the loop lives
in [`docs/feedback-loop.md`](../docs/feedback-loop.md).

## It does not work in the session that created it

Claude Code loads MCP servers at startup. These tools appear after a restart, and not before.
Until then use `tools/sfctl.mjs`, which speaks the same HTTP API from a shell with no restart
and does everything the tools below do.

## Registration

Already committed as `.mcp.json` at the repository root:

```json
{
  "mcpServers": {
    "starfall": {
      "command": "node",
      "args": ["mcp/starfall-mcp.mjs"],
      "env": {
        "STARFALL_DEBUG_PORT": "7671",
        "STARFALL_SCENE": "smoke",
        "STARFALL_AUTOSTART": "1"
      }
    }
  }
}
```

## Tools

| tool | endpoint |
|---|---|
| `list_scenes` | `GET /scenes` |
| `get_state` | `GET /state` |
| `load_scene` | `POST /scene` |
| `set_time` | `POST /time` |
| `step` | `POST /step` |
| `set_camera` | `POST /camera` |
| `trigger_animation` | `POST /event` |
| `capture_frame` | `POST /frame` |
| `capture_sequence` | `POST /capture` |
| `measure_frame` | `POST /measure` |
| `analyse_capture` | `POST /analyse` |

## Design

- **No dependencies.** MCP is spoken over stdio as newline-delimited JSON-RPC 2.0, implemented
  directly. There is nothing to `npm install`, so the loop runs on a machine with no network.
- **No logic.** Every tool is one HTTP call. Behaviour changes go in `DebugServer` and all three
  clients — MCP, `sfctl`, `curl` — get them at once.
- **Starts the game on demand.** `./gw debugServer` is spawned if nothing is listening, so the
  first call after a cold start can take up to a minute. `STARFALL_AUTOSTART=0` disables that.

## Testing it without a client

```bash
printf '%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{}}}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_scenes","arguments":{}}}' \
  | node mcp/starfall-mcp.mjs
```

The server drains in-flight requests before exiting on stdin close, so a piped script like this
gets its answers.
