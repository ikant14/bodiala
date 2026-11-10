---
name: dispatch
description: Pick ready KAN tickets from Jira and start one linked agent session per ticket (up to 4), each running /work-ticket. Use when the user says "dispatch", "start agents", "boot up agents on Jira tickets" or similar.
---

# /dispatch [n]

You are the **coordinator**. You don't write code. You choose tickets, start one agent session per ticket, and report. `n` is how many agents to start (default: as many free slots as there are, max 4).

Run this from a session the user opened themselves. A session that an orchestrator dispatched can't start linked sessions or use the sidebar; if `start_session` or the sidebar tools are missing, stop and tell the user that.

Jira: site `iraklikantar.atlassian.net`, cloudId `85c8de9e-53b0-41d0-b30e-beb850223a9f`, project `KAN`. Rules for what is ready and how lanes work: `AGENTS.md` §1 and §7.

## 1. Tidy up first

Run the `/land` checks (step 1–3 of `.claude/skills/land/SKILL.md`) so merged work is closed and its slots are free before you count them. Report what was landed, if anything.

## 2. Count free slots

Slots 1–4 are free when `C:\Users\user\IdeaProjects\.agent-slots\slot-<n>` doesn't exist. Slot 0 is the user's; never assign it. If no slot is free, say so and stop.

## 3. Find ready tickets

1. JQL: `project = KAN AND parent in (KAN-151, KAN-153) AND status = "To Do" ORDER BY Rank` (active sprint first: add `AND sprint in openSprints()` for a first pass).
2. Skip anything whose key or summary is a decision (`DEC-*`). Decisions belong to the user.
3. For each candidate, read it with `getJiraIssue` (`view: "evidence"`) and check its links: every issue it **is blocked by** must be **Done**. Exceptions are in `AGENTS.md` §1 (undecided phase-1 decisions may be built on their written recommendation; ignore PAY-2/PAY-6 links). Also read the ticket's "Depends on:" line in the description and treat those backlog ids the same way (look them up by label `backlog-<ID>`).
4. Also look at `status = "In Progress"` and `status = "In Review"`: avoid tickets in the same lane, or that clearly touch the same files, as work already in flight.
5. Take tickets from the top of the rank, at most one per lane, until you have `n`.

If fewer tickets are ready than slots, start only those. If none are ready, say why (what they're blocked on) and stop.

## 4. Confirm with the user

Before claiming anything, show a short table: ticket, title, lane, slot, branch name (`feature/KAN-xx-…` or `bugfix/KAN-xx-…`, per `AGENTS.md` §3), and anything risky (Opus-level work per `CLAUDE.md` "Model choice", frontend changes, stub changes). Ask once: "Start these?" Wait for a yes.

## 5. Claim and start

For each approved ticket:

1. Re-read it; if it is no longer To Do, drop it.
2. Transition it to **In Progress** and add the comment `Claimed by dispatch · slot <n> · branch <branch> · <date>`.
3. Start a **linked session** (`start_session`) in `C:\Users\user\IdeaProjects\bodiala` with a worktree, named after the ticket (e.g. `KAN-44 CheckRate`), with this prompt:

   ```
   /work-ticket KAN-xx slot=<n> branch=<branch>
   ```

   Use Opus for tickets that `CLAUDE.md` "Model choice" says need Opus for execution; otherwise the project default.
4. Put every session you started, plus this one, in the sidebar group **bodiala agents** (create it if it doesn't exist).

## 6. Report

A table: ticket, session link, slot, branch. Then: "I'll be told when each one finishes. Ask me for status any time, or run `/land` after you merge PRs."

When a linked session reports back, relay its result in one or two lines: PR link and In Review, or blocked and why.
