#!/usr/bin/env node
/**
 * Starfall MCP server — a thin wrapper over the debug API in dev.starfall.debug.
 *
 * There is no logic in here. Every tool is one HTTP call to an endpoint that already exists,
 * because the endpoints were designed for this and because the same calls have to remain
 * drivable from a shell (tools/sfctl.mjs) and from curl. If a behaviour ever needs to change,
 * it changes in DebugServer and all three clients get it.
 *
 * **This server cannot be used in the session that created it.** Claude Code loads MCP servers
 * at startup, so the tools below appear only after a restart. Until then, tools/sfctl.mjs does
 * exactly the same things from Bash — that is the deliverable with value today, and this one
 * is the same thing wrapped for later.
 *
 * Speaks MCP over stdio as newline-delimited JSON-RPC 2.0, implemented directly rather than
 * through @modelcontextprotocol/sdk. That keeps the whole feedback loop installable with no
 * network fetch, which is the same reasoning behind the JDK-only JSON in dev.starfall.analysis.
 */

import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const PROJECT = path.resolve(HERE, '..');
const PORT = Number(process.env.STARFALL_DEBUG_PORT || 7671);
const AUTOSTART = process.env.STARFALL_AUTOSTART !== '0';
const BOOT_SCENE = process.env.STARFALL_SCENE || 'smoke';
const PROTOCOL_VERSION = '2025-06-18';

let serverProcess = null;

// --------------------------------------------------------------------- debug API

async function health(timeoutMs = 1500) {
  try {
    const res = await fetch(`http://127.0.0.1:${PORT}/health`, {
      signal: AbortSignal.timeout(timeoutMs),
    });
    return res.ok;
  } catch {
    return false;
  }
}

/**
 * Starts ./gw debugServer if nothing is listening.
 *
 * Set STARFALL_AUTOSTART=0 to require an externally managed server instead — useful when you
 * want to watch the window, since an autostarted one runs hidden.
 */
async function ensureServer() {
  if (await health()) return;
  if (!AUTOSTART) {
    throw new Error(
      `no debug API on port ${PORT} and STARFALL_AUTOSTART=0. Start one with:\n` +
      `  ./gw debugServer -Pport=${PORT} -Pscene=${BOOT_SCENE}`
    );
  }
  if (!serverProcess) {
    const gw = process.platform === 'win32' ? 'bash' : './gw';
    const args = process.platform === 'win32'
      ? ['./gw', 'debugServer', '-q', `-Pport=${PORT}`, `-Pscene=${BOOT_SCENE}`]
      : ['debugServer', '-q', `-Pport=${PORT}`, `-Pscene=${BOOT_SCENE}`];
    serverProcess = spawn(gw, args, {
      cwd: PROJECT,
      stdio: ['ignore', 'ignore', 'ignore'],
      detached: false,
    });
    serverProcess.on('exit', () => { serverProcess = null; });
  }
  // A cold Gradle start plus GL context creation can take a while on first run.
  const deadline = Date.now() + 120000;
  while (Date.now() < deadline) {
    if (await health()) return;
    await new Promise((r) => setTimeout(r, 500));
  }
  throw new Error(`the debug API did not come up on port ${PORT} within 120 s`);
}

async function api(method, endpoint, body) {
  await ensureServer();
  const res = await fetch(`http://127.0.0.1:${PORT}${endpoint}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error(`non-JSON response from ${endpoint}: ${text.slice(0, 500)}`);
  }
  if (!res.ok) throw new Error(json.error || `${endpoint} returned ${res.status}`);
  return json;
}

// --------------------------------------------------------------------- tools

const TOOLS = [
  {
    name: 'list_scenes',
    description:
      'List every scene the capture harness and debug API can drive, and which one is loaded. '
      + 'Scene names are the same ones ./gw capture -Pscene=<name> accepts.',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    call: () => api('GET', '/scenes'),
  },
  {
    name: 'get_state',
    description:
      'Current scene, simulated time, camera framing, the events this scene understands, and '
      + 'the named regions available for measurement.',
    inputSchema: { type: 'object', properties: {}, additionalProperties: false },
    call: () => api('GET', '/state'),
  },
  {
    name: 'load_scene',
    description: 'Load a scene by name and reset its simulated clock to zero (after its warmup).',
    inputSchema: {
      type: 'object',
      properties: { name: { type: 'string', description: 'a name from list_scenes' } },
      required: ['name'],
      additionalProperties: false,
    },
    call: (a) => api('POST', '/scene', { name: a.name }),
  },
  {
    name: 'set_time',
    description:
      'Seek to an absolute simulated time in seconds. Seeking backwards rebuilds the scene and '
      + 'replays from warmup, so the state is always one a capture starting there would produce.',
    inputSchema: {
      type: 'object',
      properties: { t: { type: 'number', description: 'seconds since warmup' } },
      required: ['t'],
      additionalProperties: false,
    },
    call: (a) => api('POST', '/time', { t: a.t }),
  },
  {
    name: 'step',
    description: 'Advance the simulation by n steps of dt seconds (default one 60 Hz frame).',
    inputSchema: {
      type: 'object',
      properties: {
        frames: { type: 'integer', minimum: 0, default: 1 },
        dt: { type: 'number', default: 0.0166667, description: 'seconds per step' },
      },
      additionalProperties: false,
    },
    call: (a) => api('POST', '/step', { frames: a.frames ?? 1, dt: a.dt ?? 1 / 60 }),
  },
  {
    name: 'set_camera',
    description:
      'Frame captures on a region of interest. This crops and rescales the rendered frame; it '
      + 'never changes what the scene draws, so it cannot alter what is being measured. '
      + 'Pass reset:true to go back to the full frame.',
    inputSchema: {
      type: 'object',
      properties: {
        x: { type: 'integer' }, y: { type: 'integer' },
        w: { type: 'integer' }, h: { type: 'integer' },
        outW: { type: 'integer', description: 'output width; omit to keep the crop size' },
        outH: { type: 'integer' },
        reset: { type: 'boolean' },
      },
      additionalProperties: false,
    },
    call: (a) => api('POST', '/camera', a.reset ? { reset: true } : a),
  },
  {
    name: 'trigger_animation',
    description:
      'Fire a named scene event, e.g. a parry or a knockback. Scenes that are time-scripted '
      + 'rather than event-driven report that clearly; drive those with set_time and step.',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string' },
        args: { type: 'object', additionalProperties: { type: 'string' } },
      },
      required: ['name'],
      additionalProperties: false,
    },
    call: (a) => api('POST', '/event', { name: a.name, args: a.args || {} }),
  },
  {
    name: 'capture_frame',
    description: 'Write a single PNG of the current state to a path.',
    inputSchema: {
      type: 'object',
      properties: { out: { type: 'string', default: 'out/debug/frame.png' } },
      additionalProperties: false,
    },
    call: (a) => api('POST', '/frame', { out: a.out || 'out/debug/frame.png' }),
  },
  {
    name: 'capture_sequence',
    description:
      'Capture a windowed sequence plus a contact sheet. Anything about timing must be captured '
      + 'at a true frame rate (STYLE.md 11.2): use step 0.0167 and aim start at the beat that '
      + 'matters. The result includes the exact ./gw capture command that reproduces it.',
    inputSchema: {
      type: 'object',
      properties: {
        out: { type: 'string', description: 'capture directory, e.g. out/captures/s4-p1-parry' },
        frames: { type: 'integer', default: 24 },
        step: { type: 'number', default: 0.0166667, description: 'seconds between frames' },
        start: { type: 'number', default: 0, description: 'seconds after warmup for frame 0' },
        cols: { type: 'integer', default: 6 },
        label: { type: 'string' },
        live: {
          type: 'boolean',
          default: false,
          description: 'capture from the current live state instead of replaying from warmup; '
            + 'needed after trigger_animation, but the result is not reproducible from the CLI',
        },
      },
      required: ['out'],
      additionalProperties: false,
    },
    call: (a) => api('POST', '/capture', {
      out: a.out,
      frames: a.frames ?? 24,
      step: a.step ?? 1 / 60,
      start: a.start ?? 0,
      cols: a.cols ?? 6,
      label: a.label,
      fresh: !a.live,
    }),
  },
  {
    name: 'measure_frame',
    description:
      'Measure the live frame without writing it: paper level, figure box and height, and per-region '
      + 'coverage, ink share and centroid. Every number comes back with the rectangle it was taken '
      + 'through.',
    inputSchema: {
      type: 'object',
      properties: {
        regions: {
          type: 'array',
          items: { type: 'string' },
          description: 'region names, or inline specs like hair=fig:-0.06,-0.01,0.70,0.30',
        },
        threshold: { type: 'number', default: 0.85, description: 'ink threshold as a fraction of paper' },
      },
      additionalProperties: false,
    },
    call: (a) => api('POST', '/measure', { regions: a.regions || [], threshold: a.threshold ?? 0.85 }),
  },
  {
    name: 'analyse_capture',
    description:
      'Run the analysis CLI over a capture directory or a PNG. Commands: report, figure, regions, '
      + 'coverage, bands, track, autocorr, edge, marks, values, diff, timing. `track` requires '
      + '--anchor: a lag figure without its anchor is unfalsifiable (STYLE.md 7.1). '
      + 'Example args: ["track","out/captures/s4-p1-parry","--anchor","hips","--fps","60"].',
    inputSchema: {
      type: 'object',
      properties: {
        args: {
          type: 'array',
          items: { type: 'string' },
          description: 'argv for the analysis CLI, starting with the command name',
        },
      },
      required: ['args'],
      additionalProperties: false,
    },
    call: async (a) => {
      const r = await api('POST', '/analyse', { args: a.args });
      return { text: r.output, exit: r.exit };
    },
  },
];

// --------------------------------------------------------------------- MCP plumbing

function send(message) {
  process.stdout.write(JSON.stringify(message) + '\n');
}

function result(id, value) {
  send({ jsonrpc: '2.0', id, result: value });
}

function failure(id, code, message) {
  send({ jsonrpc: '2.0', id, error: { code, message } });
}

async function handle(msg) {
  const { id, method, params } = msg;
  if (method === 'initialize') {
    const asked = params?.protocolVersion;
    result(id, {
      protocolVersion: typeof asked === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(asked) ? asked : PROTOCOL_VERSION,
      capabilities: { tools: {} },
      serverInfo: { name: 'starfall', version: '0.1.0' },
      instructions:
        'Drives the Starfall visual feedback loop. Capture windows at a true frame rate '
        + '(step 0.0167) for anything about timing, and measure with analyse_capture rather than '
        + 'by eye. Starts ./gw debugServer on demand; the first call may take a minute.',
    });
    return;
  }
  if (method === 'notifications/initialized' || method?.startsWith('notifications/')) {
    return;
  }
  if (method === 'ping') {
    result(id, {});
    return;
  }
  if (method === 'tools/list') {
    result(id, {
      tools: TOOLS.map(({ name, description, inputSchema }) => ({ name, description, inputSchema })),
    });
    return;
  }
  if (method === 'tools/call') {
    const tool = TOOLS.find((t) => t.name === params?.name);
    if (!tool) {
      failure(id, -32602, `unknown tool '${params?.name}'`);
      return;
    }
    try {
      const out = await tool.call(params.arguments || {});
      const text = typeof out === 'string' ? out
        : out && typeof out.text === 'string' ? out.text
        : JSON.stringify(out, null, 2);
      result(id, { content: [{ type: 'text', text }], isError: false });
    } catch (err) {
      result(id, { content: [{ type: 'text', text: 'error: ' + err.message }], isError: true });
    }
    return;
  }
  if (id !== undefined) {
    failure(id, -32601, `method not found: ${method}`);
  }
}

// Requests still being served. A client normally keeps stdin open for the process lifetime,
// but a piped script closes it immediately — and exiting then would drop the answers, which
// is also exactly how you would test this server from a shell.
let inFlight = 0;
let closing = false;

const rl = createInterface({ input: process.stdin });
rl.on('line', (line) => {
  const trimmed = line.trim();
  if (!trimmed) return;
  let msg;
  try {
    msg = JSON.parse(trimmed);
  } catch {
    return;
  }
  inFlight++;
  handle(msg)
    .catch((err) => {
      if (msg.id !== undefined) failure(msg.id, -32603, err.message);
    })
    .finally(() => {
      inFlight--;
      if (closing && inFlight === 0) stop();
    });
});

const stop = () => {
  if (serverProcess) {
    try { serverProcess.kill(); } catch { /* already gone */ }
  }
  process.exit(0);
};
process.on('SIGINT', stop);
process.on('SIGTERM', stop);
rl.on('close', () => {
  closing = true;
  if (inFlight === 0) stop();
});
