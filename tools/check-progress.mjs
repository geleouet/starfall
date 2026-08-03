import fs from 'fs';
import path from 'path';

import { fileURLToPath } from 'url';
const root = path.resolve(fileURLToPath(new URL('..', import.meta.url)));
const html = fs.readFileSync(path.join(root, 'progress.html'), 'utf8');

// Extract the DATA object literal declaration through the closing "};"
const start = html.indexOf('const DATA = {');
if (start < 0) { console.error('FAIL: no DATA'); process.exit(1); }
const objEnd = html.indexOf('\n};', start);
if (objEnd < 0) { console.error('FAIL: DATA has no closing };'); process.exit(1); }
const body = html.slice(start, objEnd + 3);

let DATA;
try {
  DATA = eval(body + '\nDATA;');
} catch (e) {
  console.error('FAIL: DATA does not evaluate:', e.message);
  process.exit(1);
}

let bad = 0;
const seen = new Set();
const walk = (v, p) => {
  if (v === undefined) { console.error('FAIL undefined at ' + p); bad++; return; }
  if (typeof v === 'string') {
    // A concatenation leak reads as "…<tag>undefined" or "… undefined<tag>"; the word also
    // occurs in prose ("the shading language leaves undefined"), so only flag the leak shape.
    if (/(^|[>\s])undefined([<,.;:]|$)/.test(v) && !/\b(leaves|is|as|are|was|remains|were)\s+undefined\b/.test(v)) {
      console.error('FAIL "undefined" leak at ' + p + ': ' + v.slice(0, 80)); bad++;
    }
    if (/\\u[0-9a-fA-F]{4}/.test(v)) { console.error('FAIL raw escape leaked at ' + p); bad++; }
    return;
  }
  if (Array.isArray(v)) return v.forEach((x, i) => walk(x, p + '[' + i + ']'));
  if (v && typeof v === 'object') return Object.entries(v).forEach(([k, x]) => walk(x, p + '.' + k));
};
walk(DATA, 'DATA');

// Existence on THIS disk proves nothing: out/**/frame_*.png is gitignored, so seven
// embedded frames were broken images for every reader but me, and this check passed
// on all of them. What the page needs is that the file is *published*, so ask git.
import { execFileSync } from 'child_process';
const tracked = new Set(
  execFileSync('git', ['-C', root, 'ls-files'], { encoding: 'utf8', maxBuffer: 64 << 20 })
    .split('\n').filter(Boolean));

const checkImg = (rel, where) => {
  if (seen.has(rel)) return;
  seen.add(rel);
  if (!fs.existsSync(path.join(root, rel))) {
    console.error('FAIL missing image (' + where + '): ' + rel); bad++;
  } else if (!tracked.has(rel)) {
    console.error('FAIL untracked image — broken for every reader but this one ('
      + where + '): ' + rel + '  [git add -f it]'); bad++;
  }
};
(DATA.refs || []).forEach(r => checkImg(r, 'refs'));
(DATA.log || []).forEach((e, i) => { if (e.sheet) checkImg(e.sheet, 'log[' + i + '] ' + e.title); });

// A test that reads a gitignored capture and guards it with assumeTrue does not fail
// on a clean clone -- it SKIPS, silently, and the result it certifies simply does not
// exist for anyone else. Three did. `skipped="0"` on the author's machine proves
// nothing, because the files are on the author's disk; the only honest check is
// whether git publishes them. Same class as the images above, one layer down.
const srcRefs = new Map();
const scan = (dir) => {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) scan(p);
    else if (/\.java$/.test(e.name)) {
      for (const m of fs.readFileSync(p, 'utf8').matchAll(/out\/captures\/[\w./-]*frame_\d+\.png/g)) {
        if (!srcRefs.has(m[0])) srcRefs.set(m[0], path.relative(root, p).replace(/\\/g, '/'));
      }
    }
  }
};
scan(path.join(root, 'src'));
for (const [rel, from] of srcRefs) {
  if (!tracked.has(rel)) {
    console.error('FAIL untracked capture read by code — the assertion fails OPEN on a clean '
      + 'clone (' + from + '): ' + rel + '  [git add -f it]'); bad++;
  }
}

// The path scan above is a HINT, not the guard, and an audit proved why: it regex-matches
// literal paths, so `CorpusTest`, which composes them from two arguments, walked straight
// past it -- six tests skipping, BUILD SUCCESSFUL, and this checker printing OK. That is
// exactly the defect STYLE.md 11.2b(f) describes: the assertion was reachable, but its
// scope never covered its claim.
//
// The scope-correct guard does not look at paths at all. It asks the suite how many tests
// it declined to run, which catches every skip mechanism -- composed paths, assumptions,
// empty parameter sets, a forgotten @Disabled -- regardless of how the artefact is named.
const resultsDir = path.join(root, 'build/test-results/test');
if (!fs.existsSync(resultsDir)) {
  // Refusing here is the point. A missing report is the one input under which a
  // skip-counting check would otherwise pass by having nothing to count.
  console.error('FAIL no test results at build/test-results/test — run `./gw test` before '
    + 'trusting this checker; with no report it cannot see a skipped test.');
  bad++;
} else {
  // And the report must be NEWER than what it claims to have tested. The first adversarial
  // attempt against the skip check defeated it this way: capture frames are not declared
  // gradle inputs, so hiding one leaves `./gw test` UP-TO-DATE and this checker reads a
  // stale XML that still says skipped=0. Results older than their inputs are not results.
  const newest = (dir, acc = 0) => {
    if (!fs.existsSync(dir)) return acc;
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, e.name);
      acc = e.isDirectory() ? newest(p, acc) : Math.max(acc, fs.statSync(p).mtimeMs);
    }
    return acc;
  };
  const reportAt = Math.max(...fs.readdirSync(resultsDir)
    .filter(f => f.endsWith('.xml'))
    .map(f => fs.statSync(path.join(resultsDir, f)).mtimeMs), 0);
  const inputAt = Math.max(newest(path.join(root, 'src')), newest(path.join(root, 'out/captures')));
  if (inputAt > reportAt) {
    console.error('FAIL test report is older than its inputs by '
      + Math.round((inputAt - reportAt) / 1000) + 's — re-run `./gw test --rerun-tasks`. '
      + 'Capture frames are not declared gradle inputs, so a green UP-TO-DATE build can '
      + 'certify a suite that never saw them.');
    bad++;
  }

  let skipped = 0, files = 0;
  for (const f of fs.readdirSync(resultsDir)) {
    if (!f.endsWith('.xml')) continue;
    files++;
    const xml = fs.readFileSync(path.join(resultsDir, f), 'utf8');
    const m = xml.match(/<testsuite[^>]*\bskipped="(\d+)"/);
    if (m && +m[1] > 0) {
      skipped += +m[1];
      console.error('FAIL ' + m[1] + ' skipped test(s) in ' + f + ' — a skipped assertion '
        + 'certifies nothing and the suite reports it as success');
      bad++;
    }
  }
  console.log('test classes: ' + files + ', skipped: ' + skipped);
}

// Sanity counts
console.log('captures read by code (literal paths only — see above): ' + srcRefs.size);
console.log('log entries: ' + (DATA.log || []).length);
console.log('systems: ' + (DATA.systems || []).map(s => s.n + ':' + s.status + '/p' + s.passes).join(' '));
console.log('updated: ' + DATA.updated);
console.log(bad === 0 ? 'OK' : 'FAILURES: ' + bad);
process.exit(bad === 0 ? 0 : 1);
