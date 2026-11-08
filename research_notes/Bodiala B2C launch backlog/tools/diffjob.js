// Compares the previous plan (out_old, which matches Jira) with the new one (out) and writes the minimal
// sequential job: label/description changes, plus a rank move wherever an item's predecessor changed.
const fs = require('fs');
const path = require('path');
const dir = __dirname;
const load = d => JSON.parse(fs.readFileSync(path.join(dir, d, 'manifest.json'), 'utf8'));
const html = (d, id) => fs.readFileSync(path.join(dir, d, 'html', id + '.html'), 'utf8');
const oldM = load('out_old'), newM = load('out');
const SKIP_DESC = new Set(process.argv.slice(2)); // ids whose description Jira already holds
const oldById = new Map(oldM.map(e => [e.id, e]));
// Keep the longest run that is already in the new relative order (LIS) where it is; move everything else,
// top-down in the new order, right after its new predecessor. The simulation below checks the result.
const newIdx = new Map(newM.map((e, i) => [e.key, i]));
const seq = oldM.map(e => newIdx.get(e.key));
const tails = [], prevOf = new Array(seq.length), tailIdx = [];
seq.forEach((v, i) => {
  let lo = 0, hi = tails.length;
  while (lo < hi) { const mid = (lo + hi) >> 1; if (tails[mid] < v) lo = mid + 1; else hi = mid; }
  tails[lo] = v; tailIdx[lo] = i; prevOf[i] = lo ? tailIdx[lo - 1] : -1;
});
const keep = new Set();
for (let i = tailIdx[tails.length - 1]; i !== -1; i = prevOf[i]) keep.add(oldM[i].key);
const cur = oldM.map(e => e.key);
const job = [];
newM.forEach((e, i) => {
  const o = oldById.get(e.id);
  const pred = i ? newM[i - 1].key : null;
  const step = { key: e.key, id: e.id };
  if (JSON.stringify(o.labels) !== JSON.stringify(e.labels)) step.labels = e.labels;
  if (!SKIP_DESC.has(e.id) && html('out_old', e.id) !== html('out', e.id)) step.htmlFile = path.join(dir, 'out', 'html', e.id + '.html');
  if (pred && !keep.has(e.key) && cur.indexOf(e.key) !== cur.indexOf(pred) + 1) {
    step.rankAfterIssue = pred;
    cur.splice(cur.indexOf(e.key), 1);
    cur.splice(cur.indexOf(pred) + 1, 0, e.key);
  }
  if (step.labels || step.htmlFile || step.rankAfterIssue) job.push(step);
});
if (JSON.stringify(cur) !== JSON.stringify(newM.map(e => e.key))) throw new Error('simulation does not reach the new order');
job.forEach((s, i) => (s.step = i + 1));
fs.writeFileSync(path.join(dir, 'job-pay.json'), JSON.stringify(job, null, 1));
for (const s of job) console.log(String(s.step).padStart(2), s.id.padEnd(8), s.key.padEnd(8), s.labels ? 'labels' : '      ', s.htmlFile ? 'desc' : '    ', s.rankAfterIssue ? 'rank after ' + s.rankAfterIssue : '');
console.log('steps', job.length);
