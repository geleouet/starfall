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

// Sanity counts
console.log('log entries: ' + (DATA.log || []).length);
console.log('systems: ' + (DATA.systems || []).map(s => s.n + ':' + s.status + '/p' + s.passes).join(' '));
console.log('updated: ' + DATA.updated);
console.log(bad === 0 ? 'OK' : 'FAILURES: ' + bad);
process.exit(bad === 0 ? 0 : 1);
