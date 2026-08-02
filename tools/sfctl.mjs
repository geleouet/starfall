#!/usr/bin/env node
/**
 * sfctl — command-line client for the Starfall debug API.
 *
 * This is the half of the feedback loop that works today. An MCP server cannot be used in the
 * session that writes it, because Claude Code loads MCP servers at startup; this speaks the
 * same HTTP API from a shell, right now, with no restart and no dependencies beyond Node.
 *
 * Start the API first, in another shell or in the background:
 *   ./gw debugServer -Pport=7671 -Pscene=sim-extreme
 *
 * Then:
 *   node tools/sfctl.mjs scenes
 *   node tools/sfctl.mjs state
 *   node tools/sfctl.mjs scene sim-extreme
 *   node tools/sfctl.mjs seek 1.95
 *   node tools/sfctl.mjs step --frames 4 --dt 0.0167
 *   node tools/sfctl.mjs camera --x 420 --y 90 --w 260 --h 260 --out-w 520 --out-h 520
 *   node tools/sfctl.mjs camera --reset
 *   node tools/sfctl.mjs event knockback --arg dir=left
 *   node tools/sfctl.mjs frame out/debug/one.png
 *   node tools/sfctl.mjs capture out/captures/s6-probe --frames 24 --step 0.0167 --start 1.95 --cols 6
 *   node tools/sfctl.mjs measure --region hair --region hips
 *   node tools/sfctl.mjs analyse report out/captures/s6-probe
 *   node tools/sfctl.mjs analyse track out/captures/s6-probe --anchor hips --fps 60
 *   node tools/sfctl.mjs shutdown
 *
 * Options: --port (default 7671, or $STARFALL_DEBUG_PORT), --json for raw output.
 */

const DEFAULT_PORT = Number(process.env.STARFALL_DEBUG_PORT || 7671);

function parseArgs(argv) {
  const positional = [];
  const opts = {};
  const repeated = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      let key = a.slice(2);
      let value = null;
      const eq = key.indexOf('=');
      if (eq >= 0) {
        value = key.slice(eq + 1);
        key = key.slice(0, eq);
      } else if (i + 1 < argv.length && !argv[i + 1].startsWith('--')) {
        value = argv[++i];
      } else {
        value = 'true';
      }
      opts[key] = value;
      (repeated[key] ||= []).push(value);
    } else {
      positional.push(a);
    }
  }
  return { positional, opts, repeated };
}

async function call(port, method, path, body) {
  const url = `http://127.0.0.1:${port}${path}`;
  let res;
  try {
    res = await fetch(url, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    throw new Error(
      `cannot reach the debug API at ${url}\n` +
      `  start it with:  ./gw debugServer -Pport=${port} -Pscene=<name>\n` +
      `  underlying error: ${err.message}`
    );
  }
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error(`non-JSON response (${res.status}) from ${path}: ${text.slice(0, 400)}`);
  }
  if (!res.ok) {
    throw new Error(`${path} -> ${res.status}: ${json.error || text}`);
  }
  return json;
}

function num(opts, key, fallback) {
  return opts[key] === undefined ? fallback : Number(opts[key]);
}

/** Prints the interesting part of a response, or the whole thing with --json. */
function report(json, opts, summarise) {
  if (opts.json !== undefined || !summarise) {
    console.log(JSON.stringify(json, null, 2));
    return;
  }
  summarise(json);
}

const USAGE = `sfctl — drive the Starfall debug API from a shell

  scenes                              list registered scenes
  state                               current scene, time, camera, events
  scene <name>                        load a scene
  seek <seconds>                      set absolute simulated time
  step [--frames N] [--dt S]          advance the simulation
  camera [--x --y --w --h] [--out-w --out-h] | --reset
  event <name> [--arg k=v ...]        trigger a scene event
  frame <path.png>                    write one frame
  capture <dir> [--frames --step --start --cols --label] [--live]
                                      write a windowed sequence and a contact sheet
  measure [--region NAME|SPEC ...]    measure the live frame without writing it
  regions [--file regions.json]       load and resolve a region set
  analyse <analysis args...>          run the analysis CLI in the server's JVM
  shutdown                            stop the server

Global: --port N (default ${DEFAULT_PORT}), --json (raw response)

Everything here also works without the server:
  ./gw capture -Pscene=<name> -Pframes=24 -Pstart=1.95 -Pstep=0.0167 -Pout=<dir>
  ./gw analyse -Pargs="report <dir>"
  ./gw timing  -Pscene=<name> -Pstart=1.95 -Pduration=0.4 -Pout=<file.json>
`;

async function main() {
  const argv = process.argv.slice(2);
  if (argv.length === 0 || argv[0] === 'help' || argv[0] === '--help') {
    console.log(USAGE);
    process.exit(argv.length === 0 ? 2 : 0);
  }
  const cmd = argv[0];
  const { positional, opts, repeated } = parseArgs(argv.slice(1));
  const port = num(opts, 'port', DEFAULT_PORT);

  switch (cmd) {
    case 'scenes': {
      const r = await call(port, 'GET', '/scenes');
      report(r, opts, (j) => {
        console.log(`current: ${j.current}`);
        j.scenes.forEach((s) => console.log('  ' + s));
      });
      break;
    }
    case 'state': {
      const r = await call(port, 'GET', '/state');
      report(r, opts, (j) => {
        console.log(`scene    ${j.scene}  (${j.description})`);
        console.log(`time     ${j.time} s   duration ${j.duration} s   warmup ${j.warmup} s`);
        console.log(`render   ${j.width}x${j.height}   camera ${j.camera ? j.camera.join(',') : 'full frame'}`);
        console.log(`events   ${j.events.length ? j.events.join(', ') : '(scene is time-scripted; use seek/step)'}`);
        console.log(`regions  ${(j.regions || []).join(', ')}`);
      });
      break;
    }
    case 'health':
      report(await call(port, 'GET', '/health'), opts, null);
      break;
    case 'scene': {
      const name = positional[0];
      if (!name) throw new Error('usage: sfctl scene <name>');
      report(await call(port, 'POST', '/scene', { name }), opts, (j) =>
        console.log(`loaded ${j.scene} at t=${j.time}s`));
      break;
    }
    case 'seek': {
      const t = Number(positional[0]);
      if (Number.isNaN(t)) throw new Error('usage: sfctl seek <seconds>');
      report(await call(port, 'POST', '/time', { t }), opts, (j) =>
        console.log(`${j.scene} at t=${j.time}s`));
      break;
    }
    case 'step': {
      const body = { frames: num(opts, 'frames', 1), dt: num(opts, 'dt', 1 / 60) };
      report(await call(port, 'POST', '/step', body), opts, (j) =>
        console.log(`${j.scene} at t=${j.time}s`));
      break;
    }
    case 'camera': {
      const body = opts.reset !== undefined
        ? { reset: true }
        : {
            x: num(opts, 'x', 0), y: num(opts, 'y', 0),
            w: num(opts, 'w', 0), h: num(opts, 'h', 0),
            outW: num(opts, 'out-w', 0), outH: num(opts, 'out-h', 0),
          };
      report(await call(port, 'POST', '/camera', body), opts, (j) =>
        console.log(`camera ${j.camera ? j.camera.join(',') : 'full frame'}`));
      break;
    }
    case 'event': {
      const name = positional[0];
      if (!name) throw new Error('usage: sfctl event <name> [--arg k=v]');
      const args = {};
      (repeated.arg || []).forEach((kv) => {
        const i = kv.indexOf('=');
        if (i > 0) args[kv.slice(0, i)] = kv.slice(i + 1);
      });
      report(await call(port, 'POST', '/event', { name, args }), opts, (j) => {
        if (!j.supported) console.log(j.error);
        else if (!j.fired) console.log(`unknown event '${j.event}'. known: ${(j.known || []).join(', ')}`);
        else console.log(`fired ${j.event} at t=${j.time}s`);
      });
      break;
    }
    case 'frame': {
      const out = positional[0] || 'out/debug/frame.png';
      report(await call(port, 'POST', '/frame', { out }), opts, (j) =>
        console.log(`${j.path}  (${j.width}x${j.height}, t=${j.time}s)`));
      break;
    }
    case 'capture': {
      const out = positional[0];
      if (!out) throw new Error('usage: sfctl capture <dir> [--frames N --step S --start S --cols N]');
      const body = {
        out,
        frames: num(opts, 'frames', 24),
        step: num(opts, 'step', 1 / 60),
        start: num(opts, 'start', 0),
        cols: num(opts, 'cols', 6),
        fresh: opts.live === undefined,
      };
      if (opts.label) body.label = opts.label;
      report(await call(port, 'POST', '/capture', body), opts, (j) => {
        console.log(`${j.frames} frames -> ${j.dir}`);
        console.log(`contact sheet: ${j.contactSheet}`);
        console.log(`window: t=${j.startTime}s .. ${j.endTime}s at ${(1 / body.step).toFixed(1)} Hz`);
        console.log(`reproduce: ${j.reproduceWith}`);
      });
      break;
    }
    case 'measure': {
      const body = { regions: repeated.region || [], threshold: num(opts, 'threshold', 0.85) };
      report(await call(port, 'POST', '/measure', body), opts, (j) => {
        console.log(`${j.scene} t=${j.time}s  paper ${j.paper}  figure ${j.figure.join(',')} (h=${j.figureHeight})`);
        j.regions.forEach((r) => console.log(
          `  ${r.name.padEnd(12)} ${String(r.rect.join(',')).padEnd(20)} coverage ${(r.coverage * 100).toFixed(1)}%` +
          `  share ${(r.shareByCount * 100).toFixed(2)}%  centroid ${r.centroidX.toFixed(1)},${r.centroidY.toFixed(1)}`));
      });
      break;
    }
    case 'regions': {
      const body = opts.file ? { file: opts.file } : {};
      report(await call(port, 'POST', '/regions', body), opts, (j) => {
        console.log(`figure ${j.figure.join(',')}`);
        Object.entries(j.regions).forEach(([k, v]) => console.log(`  ${k.padEnd(12)} ${v.join(',')}`));
      });
      break;
    }
    case 'analyse':
    case 'analyze': {
      // Forward everything to the analysis CLI except sfctl's own options — including the
      // value that follows --port, which would otherwise arrive as a stray positional.
      const mine = new Set(['port', 'json']);
      const args = [];
      const rest = argv.slice(1);
      for (let i = 0; i < rest.length; i++) {
        const a = rest[i];
        if (a.startsWith('--') && mine.has(a.slice(2).split('=')[0])) {
          if (!a.includes('=') && i + 1 < rest.length && !rest[i + 1].startsWith('--')) i++;
          continue;
        }
        args.push(a);
      }
      const r = await call(port, 'POST', '/analyse', { args });
      if (opts.json !== undefined) console.log(JSON.stringify(r, null, 2));
      else process.stdout.write(r.output);
      if (r.exit !== 0) process.exitCode = r.exit;
      break;
    }
    case 'shutdown':
      report(await call(port, 'POST', '/shutdown', {}), opts, () => console.log('stopping'));
      break;
    default:
      console.error(`unknown command '${cmd}'\n`);
      console.log(USAGE);
      process.exit(2);
  }
}

main().catch((err) => {
  console.error('error: ' + err.message);
  process.exit(1);
});
