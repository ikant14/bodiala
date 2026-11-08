# Backlog ↔ Jira tools

Node scripts that keep `docs/backlog.md` and Jira project KAN in step. Run them from this folder with Node 24.

- `build.js <docs/backlog.md> <../jira_map.csv> <../jira_links.csv> out`: parses the backlog and assigns tags, phases and Jira epics. It computes the rank order (phase, then dependencies, then priority) and writes `out/manifest.json` plus one Jira HTML description per task in `out/html/`. Set `EXPECT=<n>` to the task count.
- `doc.js <docs/backlog.md>`: regenerates the "Working order" section of the backlog from `out/manifest.json`.
- `diffjob.js [ids to skip]`: compares `out_old` (what Jira holds) with `out` (the new plan). It writes `job-pay.json`, the minimal sequential list of label, description and rank edits to apply with the Atlassian MCP's `editJiraIssue`. Rank moves must run one at a time, in order.

Phase rules, tags, soft links, notes and the five stream epics are hard-coded near the top of `build.js`.
