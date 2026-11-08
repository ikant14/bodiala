// Builds the Jira re-import plan: tags, phases, rank order and HTML descriptions with real task lists.
// usage: node build.js <backlog.md> <jira_map.csv> <jira_links.csv> <outDir>
const fs = require('fs');
const path = require('path');
const [,, backlogPath, mapPath, linksPath, outDir] = process.argv;

const lines = fs.readFileSync(backlogPath, 'utf8').split(/\r?\n/);
const keyOf = new Map(fs.readFileSync(mapPath, 'utf8').split(/\r?\n/).slice(1).filter(Boolean)
  .map(l => l.split(',').map(s => s.trim())));
const links = fs.readFileSync(linksPath, 'utf8').split(/\r?\n/).slice(1).filter(Boolean)
  .map(l => { const [a, , b] = l.split(',').map(s => s.trim()); return [a, b]; }); // blocker -> blocked

// ---------- parse sections ----------
const items = new Map();
for (let i = 0; i < lines.length; i++) {
  const m = lines[i].match(/^(###|####) ([A-Z0-9]+-\d+) · (.*)$/);
  if (!m) continue;
  const id = m[2];
  if (m[1] === '###' && !id.startsWith('DEC-')) continue;
  let j = i + 1;
  while (j < lines.length && !/^#{2,4} /.test(lines[j]) && lines[j] !== '---') j++;
  const body = lines.slice(i + 1, j);
  while (body.length && body[body.length - 1].trim() === '') body.pop();
  items.set(id, { id, title: m[3], body, isDec: id.startsWith('DEC-') });
  i = j - 1;
}

for (const it of items.values()) {
  if (it.isDec) {
    const t = it.body.find(l => l.startsWith('**Type:**'));
    it.types = t.match(/\*\*Type:\*\* ([^.]*)\./)[1].split(/,\s*/);
    it.prio = 0; it.repos = []; it.effort = null;
  } else {
    const meta = it.body[0].replace(/`/g, '');
    const parts = meta.split(' · ');
    it.prio = Number(parts[0].match(/P(\d)/)[1]);
    it.types = parts[1].split(/,\s*/);
    const r = parts[2];
    it.repos = /all repos/.test(r) ? ['bodiala', 'bodiala-web', 'hotelbeds-stub']
      : r.startsWith('none') ? [] : r.replace(/\(.*?\)/g, '').split(/,\s*/).map(s => s.trim()).filter(Boolean);
    it.effort = parts[3].match(/Effort (\w)/)[1].toLowerCase();
  }
  it.key = keyOf.get(it.id);
  if (!it.key) throw new Error('no key for ' + it.id);
}
if (items.size !== Number(process.env.EXPECT || 133)) throw new Error('unexpected item count ' + items.size);

// ---------- tags ----------
const T = (...t) => t;
const TAG_OVERRIDE = {
  'DEC-1': T('Business', 'Legal'), 'DEC-2': T('Business', 'Legal'), 'DEC-3': T('Business'),
  'DEC-4': T('Business'), 'DEC-5': T('Business'), 'DEC-6': T('Business', 'Legal'),
  'DEC-7': T('Business'), 'DEC-8': T('Business', 'BE', 'FE'), 'DEC-9': T('DevOps', 'FE'),
  'SEC-4': T('BE', 'DevOps'), 'SEC-7': T('FE', 'DevOps'), 'SEC-8': T('BE', 'DevOps'),
  'SEC-9': T('BE', 'DevOps'), 'SEC-10': T('FE', 'DevOps'),
  'PAY-1': T('Business'), 'PAY-6': T('BE'), 'PAY-7': T('BE', 'Business'), 'PAY-9': T('BE', 'Business'),
  'PAY-10': T('DevOps', 'Legal'), 'PAY-12': T('BE', 'FE', 'Legal'),
  'HB-7': T('BE', 'DevOps'), 'HB-15': T('Business'), 'HB-16': T('Business'), 'HB-17': T('DevOps', 'Business'),
  'CHK-7': T('Business', 'FE'),
  'A11Y-7': T('FE'),
  'LEGAL-9': T('Legal'), 'LEGAL-10': T('BE', 'Legal'), 'LEGAL-11': T('Legal'), 'LEGAL-12': T('Legal', 'DevOps'),
  'LEGAL-13': T('Legal'), 'LEGAL-14': T('Legal'),
  'SEO-3': T('FE', 'BE', 'Business'), 'SEO-9': T('BE', 'DevOps'),
  'OPS-1': T('BE'), 'OPS-2': T('DevOps'), 'OPS-3': T('DevOps'), 'OPS-4': T('DevOps'),
  'OPS-5': T('BE', 'DevOps'), 'OPS-6': T('BE', 'DevOps'), 'OPS-7': T('BE'),
  'OPS-8': T('DevOps'), 'OPS-9': T('DevOps'), 'OPS-10': T('FE', 'DevOps'), 'OPS-11': T('DevOps'),
  'QA-6': T('BE', 'DevOps'), 'QA-8': T('BE', 'DevOps'),
  'GROW-4': T('BE', 'FE', 'Business'), 'GROW-5': T('BE', 'Business'), 'GROW-7': T('BE', 'FE', 'Business'),
};
for (const it of items.values()) {
  let tags = TAG_OVERRIDE[it.id];
  if (!tags) {
    tags = [];
    if (it.repos.some(r => r === 'bodiala' || r === 'hotelbeds-stub')) tags.push('BE');
    if (it.repos.includes('bodiala-web')) tags.push('FE');
    if (it.types.includes('legal')) tags.push('Legal');
    if (it.types.includes('business')) tags.push('Business');
    if (it.types.includes('ops')) tags.push('DevOps');
  }
  const order = ['BE', 'FE', 'DevOps', 'Legal', 'Business'];
  it.tags = [...new Set(tags)].sort((a, b) => order.indexOf(a) - order.indexOf(b));
  if (!it.tags.length) throw new Error('no tags for ' + it.id);
}

// ---------- phases ----------
// 1 = build and verify locally now; 2 = legal, business, payments, deployment and go-live; 3 = after launch (P2/P3).
const PHASE2 = new Set([
  'DEC-1', 'DEC-4', 'DEC-9',
  'SEC-4', 'SEC-7', 'SEC-8', 'SEC-10',
  'HB-7', 'HB-15', 'HB-16', 'HB-17',
  'CHK-7', 'A11Y-7',
  'SEO-1', 'SEO-2', 'SEO-3', 'SEO-4', 'SEO-7',
  'OPS-2', 'OPS-3', 'OPS-4', 'OPS-8', 'OPS-9', 'OPS-10',
  'QA-6', 'QA-8', 'GROW-1', 'I18N-1', 'I18N-2', 'I18N-3',
]);
// DEC-2 (revised 2026-11-08): no online payment at launch, so the online card-payment tasks wait until after launch.
// PAY-1 (PSP account, needed for payment links) and PAY-7 (invoices) stay in phase 2.
const PHASE3 = new Set(['PAY-2', 'PAY-3', 'PAY-4', 'PAY-5', 'PAY-6', 'PAY-8', 'PAY-9', 'PAY-10']);
for (const it of items.values()) {
  if (it.prio >= 2 || PHASE3.has(it.id)) it.phase = 3;
  else if (PHASE2.has(it.id) || it.id.startsWith('PAY-') || it.id.startsWith('LEGAL-')) it.phase = 2;
  else it.phase = 1;
}
// Cross-phase links that are deliberately soft: the earlier task is built now and the later task only
// completes it (or only matters once online payment is added). Every other cross-phase link must point forward.
const SOFT = new Set(['PAY-2>CHK-1', 'LEGAL-2>CHK-2', 'LEGAL-11>CHK-5',
  'PAY-2>SEC-7', 'PAY-2>A11Y-7', 'PAY-2>HB-15', 'PAY-6>OPS-8', 'PAY-6>PAY-7']);
const NO_ONLINE_PAY = 'There is no online payment at launch (DEC-2): agents take payment by payment link or bank transfer (BOOK-10).';
const NOTE = {
  'CHK-1': 'Phase 1 (local build): build the booking-request flow, with no payment step (DEC-2, BOOK-10). Online payment (PAY-2) is planned for after launch (phase 3).',
  'SEC-7': `${NO_ONLINE_PAY} The "blocked by PAY-2" link only applies once online payment is added after launch. Until then, keep PSP scripts off the site entirely.`,
  'A11Y-7': `${NO_ONLINE_PAY} The "blocked by PAY-2" link only applies once online payment is added after launch. Audit the booking-request flow instead.`,
  'HB-15': `${NO_ONLINE_PAY} The "blocked by PAY-2" link only applies once online payment is added after launch. Show reviewers the request flow and the agent's booking step, and confirm with Hotelbeds that this flow is acceptable.`,
  'OPS-8': `${NO_ONLINE_PAY} The "blocked by PAY-6" link only applies once online payment is added after launch. At launch, alert on supplier failures and on requests that sit unanswered.`,
  'PAY-7': `${NO_ONLINE_PAY} At launch, issue the invoice or receipt from the payment the agent records. The "blocked by PAY-6" link (daily PSP reconciliation) only applies once online payment is added after launch.`,
  'CHK-2': 'Phase 1 (local build): link a placeholder terms page for now. The real booking terms come from LEGAL-2 in phase 2.',
  'CHK-5': 'Phase 1 (local build): send to a local mail catcher (for example Mailpit) for now. The email provider\'s DPA (LEGAL-11) is needed before real emails go out in phase 2.',
};
for (const id of ['DEC-2', 'DEC-3', 'DEC-5', 'DEC-6', 'DEC-7', 'DEC-8']) {
  NOTE[id] = 'Phase 1 (local build): adopt the recommendation below as the working assumption now, so local work can proceed. Confirm it with Hotelbeds, counsel or the PSP before launch (phase 2).';
}
// Decisions already taken: a green panel replaces the phase note (text must match what Jira holds).
const DECIDED = {
  'DEC-2': '<strong>Decided 2026-11-08: A, merchant of record selling AT_WEB rates in our own name, with an agent-assisted flow at launch.</strong> The customer requests a booking online and pays nothing. An agent contacts them, collects payment by payment link or bank transfer, re-checks the saved price and books at Hotelbeds (KAN-150, BOOK-10). The price is frozen as our quote, not at Hotelbeds. Booking at Hotelbeds as a hold was rejected for now as too risky. This is the working assumption for phase 1. Before launch it still needs confirming by counsel (LEGAL-2), Hotelbeds (DEC-3, HB-15, HB-16) and the tax adviser (DEC-1). The record is in <code>docs/decisions/DEC-2-merchant-model.md</code> (bodiala repo).',
};
const violations = [];
for (const [a, b] of links) {
  const A = items.get(a), B = items.get(b);
  if (!A || !B) throw new Error('unknown link ' + a + '>' + b);
  if (A.phase > B.phase && !SOFT.has(a + '>' + b)) violations.push(`${a}(p${A.phase}) blocks ${b}(p${B.phase})`);
}

// ---------- order ----------
const EPIC_ORDER = { 1: ['DEC', 'OPS', 'SEC', 'BOOK', 'HB', 'CHK', 'SRCH', 'A11Y', 'QA', 'SEO'], 2: ['DEC', 'LEGAL', 'PAY', 'HB', 'OPS', 'SEC', 'A11Y', 'QA', 'CHK', 'SEO', 'I18N', 'GROW'], 3: ['CHK', 'SRCH', 'QA', 'SEO', 'PAY', 'GROW'] };
const hard = links.filter(([a, b]) => items.get(a).phase === items.get(b).phase); // same-phase edges drive the order
const blockersOf = new Map([...items.keys()].map(k => [k, []]));
const blockedBy = new Map([...items.keys()].map(k => [k, []]));
for (const [a, b] of hard) { blockersOf.get(b).push(a); blockedBy.get(a).push(b); }
// effective priority: a blocker inherits the most urgent priority of anything it (transitively) blocks
const eff = new Map();
const effOf = (id, seen = new Set()) => {
  if (eff.has(id)) return eff.get(id);
  if (seen.has(id)) throw new Error('cycle at ' + id);
  seen.add(id);
  let p = items.get(id).prio;
  for (const b of blockedBy.get(id)) p = Math.min(p, effOf(b, seen));
  eff.set(id, p);
  return p;
};
for (const id of items.keys()) effOf(id);
const sortKey = id => {
  const it = items.get(id);
  const [ep, n] = [id.replace(/-\d+$/, ''), Number(id.match(/-(\d+)$/)[1])];
  return [it.phase, eff.get(id), EPIC_ORDER[it.phase].indexOf(ep), n];
};
const cmp = (x, y) => { const a = sortKey(x), b = sortKey(y); for (let i = 0; i < a.length; i++) if (a[i] !== b[i]) return a[i] - b[i]; return 0; };
const indeg = new Map([...items.keys()].map(k => [k, blockersOf.get(k).length]));
const ready = [...items.keys()].filter(k => indeg.get(k) === 0);
const order = [];
while (ready.length) {
  ready.sort(cmp);
  const id = ready.shift();
  order.push(id);
  for (const b of blockedBy.get(id)) { indeg.set(b, indeg.get(b) - 1); if (indeg.get(b) === 0) ready.push(b); }
}
if (order.length !== items.size) throw new Error('topological sort incomplete');
// phases must come out contiguous
for (let i = 1; i < order.length; i++) if (items.get(order[i]).phase < items.get(order[i - 1]).phase) throw new Error('phase order broken at ' + order[i]);
// every hard link points forward
const pos = new Map(order.map((id, i) => [id, i]));
const backwards = links.filter(([a, b]) => pos.get(a) > pos.get(b) && !SOFT.has(a + '>' + b));

// ---------- stream epics (what the task is for, and when) ----------
const INFRA = new Set(['DEC-9', 'OPS-2', 'OPS-3', 'OPS-4', 'OPS-8', 'OPS-9', 'OPS-10', 'OPS-11', 'SEC-4', 'SEC-7', 'SEC-8', 'SEC-9',
  'SEC-10', 'HB-7', 'HB-17', 'QA-6', 'QA-8', 'SEO-9']);
const LEGAL_BIZ = new Set(['DEC-1', 'DEC-4', 'PAY-1', 'PAY-10', 'HB-15', 'HB-16', 'CHK-7']);
const STREAM = {
  'app-now': 'App · now', 'app-later': 'App · later',
  'infra-now': 'Infra · now (local Docker)', 'infra-later': 'Infra · later (deployment)',
  'legal-now': 'Legal & business · now', 'legal-later': 'Legal & business · later',
};
for (const it of items.values()) {
  const s = INFRA.has(it.id) ? 'infra' : (it.id.startsWith('LEGAL-') || LEGAL_BIZ.has(it.id)) ? 'legal' : 'app';
  it.stream = `${s}-${it.phase === 1 ? 'now' : 'later'}`;
}

// ---------- labels ----------
const PHASE_NAME = { 1: '1, local build', 2: '2, legal, payments and deployment', 3: '3, after launch' };
const PHASE_LABEL = { 1: 'phase-1-local', 2: 'phase-2-launch', 3: 'phase-3-later' };
for (const it of items.values()) {
  const l = ['backlog', 'backlog-' + it.id, 'p' + it.prio];
  if (it.effort) l.push('effort-' + it.effort);
  if (it.isDec) l.push('decision');
  l.push(...it.tags, PHASE_LABEL[it.phase]);
  it.labels = l;
}

// ---------- markdown -> Jira HTML ----------
const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
function inline(src) {
  const codes = [];
  let s = src.replace(/`([^`]+)`/g, (_, c) => `\u0000${codes.push(c) - 1}\u0000`);
  s = esc(s);
  s = s.replace(/\[([^\]]+)\]\((https?:[^)\s]+(?:\([^)\s]*\)[^)\s]*)*)\)/g, (_, t, u) => `<a href="${u.replace(/"/g, '%22')}">${t}</a>`);
  s = s.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');
  s = s.replace(/(^|[^*\w])\*([^*\s][^*]*?)\*(?=[^*\w]|$)/g, '$1<em>$2</em>');
  s = s.replace(/\u0000(\d+)\u0000/g, (_, i) => `<code>${esc(codes[Number(i)])}</code>`);
  return s;
}
function toHtml(md) {
  const out = [];
  let i = 0;
  const isBlank = l => l.trim() === '';
  while (i < md.length) {
    const l = md[i];
    if (isBlank(l)) { i++; continue; }
    if (/^- \[[ xX]\] /.test(l)) {
      const lis = [];
      while (i < md.length && /^- \[[ xX]\] /.test(md[i])) {
        let text = md[i].replace(/^- \[[ xX]\] /, ''); i++;
        while (i < md.length && /^ {2,}\S/.test(md[i])) { text += ' ' + md[i].trim(); i++; }
        lis.push(`<li data-type="task-item"><input type="checkbox"> ${inline(text)}</li>`);
      }
      out.push(`<ul data-type="task-list">${lis.join('')}</ul>`);
      continue;
    }
    if (/^(- |\d+\. )/.test(l)) {
      const ordered = /^\d+\. /.test(l);
      const re = ordered ? /^\d+\. / : /^- (?!\[[ xX]\] )/;
      const lis = [];
      while (i < md.length && re.test(md[i])) {
        let text = md[i].replace(re, ''); i++;
        const sub = [];
        while (i < md.length && /^ {2,}\S/.test(md[i])) {
          if (/^ {2,}(- |\d+\. )/.test(md[i])) sub.push(md[i].replace(/^ {2,}(- |\d+\. )/, ''));
          else if (sub.length) sub[sub.length - 1] += ' ' + md[i].trim();
          else text += ' ' + md[i].trim();
          i++;
        }
        lis.push(`<li><p>${inline(text)}</p>${sub.length ? '<ul>' + sub.map(s => `<li><p>${inline(s)}</p></li>`).join('') + '</ul>' : ''}</li>`);
      }
      out.push(ordered ? `<ol start="1">${lis.join('')}</ol>` : `<ul>${lis.join('')}</ul>`);
      continue;
    }
    if (l.startsWith('|')) {
      const rows = [];
      while (i < md.length && md[i].startsWith('|')) { rows.push(md[i]); i++; }
      const cells = r => r.replace(/^\|/, '').replace(/\|\s*$/, '').split('|').map(c => c.trim());
      const head = cells(rows[0]);
      const body = rows.slice(2).map(cells);
      out.push('<table><thead><tr>' + head.map(c => `<th><p>${inline(c)}</p></th>`).join('') + '</tr></thead><tbody>'
        + body.map(r => '<tr>' + r.map(c => `<td><p>${inline(c)}</p></td>`).join('') + '</tr>').join('') + '</tbody></table>');
      continue;
    }
    if (l.startsWith('```')) throw new Error('code fence inside a task');
    const para = [];
    while (i < md.length && !isBlank(md[i]) && !/^(- |\d+\. |\|)/.test(md[i])) { para.push(md[i].trim()); i++; }
    out.push(`<p>${inline(para.join(' '))}</p>`);
  }
  return out.join('\n');
}

fs.mkdirSync(path.join(outDir, 'html'), { recursive: true });
const manifest = [];
order.forEach((id, idx) => {
  const it = items.get(id);
  let html = '';
  if (DECIDED[id]) html += `<div data-type="panel-success"><p>${DECIDED[id]}</p></div>\n`;
  else if (NOTE[id]) html += `<div data-type="panel-info"><p>${inline(NOTE[id])}</p></div>\n`;
  html += toHtml(it.body);
  html += `\n<p>Source: <code>docs/backlog.md</code> (bodiala repo), task ${id}. Phase: ${PHASE_NAME[it.phase]}.</p>`;
  const file = path.join(outDir, 'html', id + '.html');
  fs.writeFileSync(file, html);
  manifest.push({ rank: idx + 1, id, key: it.key, phase: it.phase, prio: it.prio, eff: eff.get(id), tags: it.tags, labels: it.labels, title: it.title, stream: it.stream, streamName: STREAM[it.stream], checkboxes: (html.match(/data-type="task-item"/g) || []).length });
});
fs.writeFileSync(path.join(outDir, 'manifest.json'), JSON.stringify(manifest, null, 1));

const srcBoxes = [...items.values()].reduce((n, it) => n + it.body.filter(l => /^- \[[ xX]\] /.test(l)).length, 0);
console.log('items', manifest.length, 'checkboxes src', srcBoxes, 'html', manifest.reduce((n, m) => n + m.checkboxes, 0));
console.log('phase counts', [1, 2, 3].map(p => manifest.filter(m => m.phase === p).length).join(' / '));
console.log('tag counts', ['BE', 'FE', 'DevOps', 'Legal', 'Business'].map(t => t + '=' + manifest.filter(m => m.tags.includes(t)).length).join(' '));
console.log('phase violations (non-soft, backward across phases):', violations.length ? violations.join('; ') : 'none');
console.log('backward links in final order:', backwards.length ? backwards.map(x => x.join('>')).join('; ') : 'none');
for (const m of manifest) console.log(`${String(m.rank).padStart(3)} ${m.id.padEnd(8)} ${m.key.padEnd(7)} ph${m.phase} P${m.prio}${m.eff !== m.prio ? '→' + m.eff : '  '} ${m.tags.join(',').padEnd(22)} ${m.title.slice(0, 70)}`);
