// Inserts the "Working order" section into docs/backlog.md, before the launch sequence. Idempotent.
const fs = require('fs');
const path = require('path');
const [,, backlogPath] = process.argv;
const m = JSON.parse(fs.readFileSync(path.join(__dirname, 'out', 'manifest.json'), 'utf8'));
const keys = new Map(m.map(e => [e.id, e.key]));
const count = p => m.filter(e => e.phase === p).length;
const cell = s => s.replace(/\|/g, '\\|');

const phases = [
  [1, 'Phase 1: local build'],
  [2, 'Phase 2: legal, payments and deployment'],
  [3, 'Phase 3: after launch'],
];
const tables = phases.map(([p, title]) => {
  const rows = m.filter(e => e.phase === p).map(e =>
    `| ${e.rank} | ${e.id} | ${e.key} | ${cell(e.title)} | ${e.id.startsWith('DEC-') ? 'Decision' : 'P' + e.prio} | ${e.streamName} | ${e.tags.join(', ')} |`);
  return `### ${title} (${count(p)} items)\n\n| # | ID | Jira | Task | Priority | Jira epic | Tags |\n|---|---|---|---|---|---|---|\n${rows.join('\n')}\n`;
}).join('\n');

const epicLines = [
  ['app-now', 'KAN-151', 'backend and frontend work for the local build, plus the six product decisions that unblock it'],
  ['infra-now', 'KAN-153', 'running the apps in Docker locally, and credential hygiene'],
  ['app-later', 'KAN-152', 'launch polish (SEO, translation, invoices, the accessibility audit), then online payment and growth after launch'],
  ['infra-later', 'KAN-154', 'hosting, CI/CD, staging and production hardening'],
  ['legal-later', 'KAN-155', 'legal pages and opinions, contracts, the payment-provider account and Hotelbeds certification'],
].map(([s, key, what]) => `- **${m.find(e => e.stream === s).streamName}** (${key}, ${m.filter(e => e.stream === s).length} tasks): ${what}.`).join('\n');

const section = `## Working order: build locally first, then legal and deployment

Nothing gets deployed until the product has been proven locally. The ${m.length} items (${m.filter(e => !e.id.startsWith('DEC-')).length} tasks and ${m.filter(e => e.id.startsWith('DEC-')).length} decisions) are therefore ranked in three phases. The Jira backlog (project KAN) holds them in the same order.

| Phase | Jira label | Items | What it covers |
|---|---|---|---|
| **1. Local build** (now) | \`phase-1-local\` | ${count(1)} | Six decisions taken as working assumptions. Then security, booking correctness, the Hotelbeds certification code, checkout without payment, search, accessibility and tests, all run against the stub or the Hotelbeds test API |
| **2. Legal, payments and deployment** (after the local build) | \`phase-2-launch\` | ${count(2)} | The entity, PSP and hosting decisions; legal pages and opinions; the PSP account for payment links (PAY-1) and invoices (PAY-7); the Hotelbeds contract, certification and mTLS; CI/CD, staging and production infrastructure; SEO and localisation |
| **3. After launch** | \`phase-3-later\` | ${count(3)} | Online card payment (PAY-2 to PAY-6 and PAY-8 to PAY-10, which DEC-2 removed from launch), plus the P2 and P3 items |

**How the order is computed.** Within a phase, an item always comes after everything that blocks it. Among the items that are ready, the higher priority goes first, and a blocker inherits the priority of the most urgent item waiting on it.

**Where phase 1 leans on phase 2.** Six decisions open phase 1, because so much of the engineering depends on them: DEC-2, DEC-3, DEC-5, DEC-6, DEC-7 and DEC-8. Each is taken now by adopting its recommendation as a working assumption, then confirmed with Hotelbeds, counsel or the PSP in phase 2. Three phase-1 tasks have a phase-2 blocker that only completes them before launch:

- CHK-1 builds the booking-request flow with no payment step (DEC-2). Online payment (PAY-2) comes after launch.
- CHK-2 links a placeholder terms page. LEGAL-2 supplies the real booking terms.
- CHK-5 sends email to a local mail catcher. The email provider's DPA (LEGAL-11) must be in place before real email goes out.

The Jira descriptions of these nine items open with a note that says the same.

**Payment links that no longer block.** DEC-2 removed online card payment from launch, so its tasks moved to phase 3. Five phase-2 tasks keep a Jira "is blocked by" link to them, which only applies once online payment is added: SEC-7, A11Y-7 and HB-15 (blocked by PAY-2), and OPS-8 and PAY-7 (blocked by PAY-6). The order ignores these links, and each of the five tasks opens with a note saying so.

**Jira epics: what a task is for, and when.** In Jira, every task sits under one of five epics. The epic shows on each backlog row, and the backlog's Epic filter shows one group at a time:

${epicLines}

The topic epics this document is organised by (SEC, BOOK, HB and so on) survive as the prefix of every task title.

**Discipline tags.** Every Jira issue carries one or more of these labels. They replace the earlier \`type-*\` and \`repo-*\` labels; each task's header line below still lists its type and repos.

- \`BE\`: code in \`bodiala\` or \`hotelbeds-stub\`.
- \`FE\`: code in \`bodiala-web\`.
- \`DevOps\`: infrastructure, CI/CD, monitoring and secrets.
- \`Legal\`: legal, privacy and compliance work.
- \`Business\`: decisions, contracts, accounts and support.

${tables}
---
`;

let doc = fs.readFileSync(backlogPath, 'utf8');
const start = doc.indexOf('## Working order: build locally first');
if (start !== -1) {
  const end = doc.indexOf('## Launch sequence');
  doc = doc.slice(0, start) + doc.slice(end);
}
const at = doc.indexOf('## Launch sequence');
if (at === -1) throw new Error('launch sequence heading not found');
doc = doc.slice(0, at) + section + doc.slice(at);
fs.writeFileSync(backlogPath, doc);
console.log('inserted', section.split('\n').length, 'lines; keys mapped', keys.size);
