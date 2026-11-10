---
name: work-ticket
description: Implement one KAN Jira ticket end to end in this worktree — branch from origin/main, build, test, open a PR, and move the ticket to In Review. Also handles review fixes on an existing PR. Use when the prompt is "/work-ticket KAN-xx" (usually sent by /dispatch).
---

# /work-ticket KAN-xx [slot=<n>] [branch=<name>] [review]

You are one agent working one ticket. `AGENTS.md` is the rulebook; this is the order to do it in. Jira cloudId `85c8de9e-53b0-41d0-b30e-beb850223a9f`.

If the argument includes `review`, skip to **Review fixes** below.

## 1. Check the ground

1. `git status` and `git rev-parse --abbrev-ref HEAD`. If there are uncommitted changes or unpushed commits that aren't yours, **stop and report**; never discard them.
2. Read the ticket (`getJiraIssue`, `view: "evidence"`). It must be **In Progress** with a claim comment for this branch (from /dispatch), or **To Do**. If you were started without /dispatch, claim it yourself per `AGENTS.md` §2. Anything else: stop and report.
3. Check its blockers again ("is blocked by" links and the "Depends on:" line). If one isn't Done, or the ticket needs a decision nobody has made, go to **Blocked**.

## 2. Set up

1. `git fetch origin; git switch -C <branch> origin/main` (branch name rules: `AGENTS.md` §3). If the ticket also changes bodiala-web, create a worktree there with the **same branch name** (`AGENTS.md` §3). Never edit the main checkouts.
2. Claim your slot's lock file (`AGENTS.md` §4). If `slot` wasn't given, take the first free one of 1–4.
3. Pick your model per `CLAUDE.md` "Model choice". If the ticket is Opus-level and you're on Sonnet, ask the `opus-advisor` subagent for a plan before writing code.

## 3. Build it

- Keep scope to the ticket's **What** and acceptance checklist. Other agents work in parallel; don't refactor shared files (`AGENTS.md` §5).
- Schema changes need a timestamped Flyway migration. New endpoints need a `RouteMatrix` row.
- If the JSON the frontend reads changes, change bodiala-web in the same branch name, or keep the old shape working.
- If an acceptance item can't be met without unbuilt work (e.g. a voucher that doesn't exist yet), build what you can, and list the gap in the PR and Jira comment. If that gap is the core of the ticket, go to **Blocked** instead.
- Commit as you go (WIP commits are fine; the user squash-merges).

## 4. Verify

Done means `AGENTS.md` §6.1: checklist met, `gradlew test` green **in your slot's schema**, `npm.cmd run build` green if you touched the frontend, and user-visible behaviour checked in your slot's running app (stub, never real Hotelbeds). Then rebase onto `origin/main` and run the tests again.

## 5. Hand back

1. Credential check in each repo you'll push: `bash C:/Users/user/IdeaProjects/bodiala/scripts/check-secrets.sh . origin/main..HEAD`. Exit 1 → don't push; fix the commits.
2. Push, then open a PR against `main` titled `KAN-xx: <ticket title>` with the body from `AGENTS.md` §6.4 (Jira link, what and why, checklist status, test results, open items, ending with the Claude Code line). Use `& "C:\Program Files\GitHub CLI\gh.exe"`. If gh isn't available or logged in, give the compare link instead.
3. Jira: comment with the PR link(s), what changed, test results, unmet checklist items and any decision recommendation you relied on. Transition to **In Review**. Don't edit the description.
4. Release the slot: stop the processes you started, delete your lock file. Keep the worktree.
5. Final message (the coordinator relays it): `KAN-xx → In Review · <PR link>` plus one line on anything left open.

**Never merge, and never push `main`.** Merging is the user's job.

## Review fixes (`/work-ticket KAN-xx review`)

1. Read the PR's review comments: `gh pr view <branch> --comments` and `gh api repos/ikant14/<repo>/pulls/<n>/comments`.
2. Fix them on the same branch, rebase on `origin/main`, re-run the tests, run the credential check, `git push --force-with-lease`.
3. Reply on the PR saying what you changed, and add a short Jira comment. The ticket stays In Review.

## Blocked

Comment on the ticket saying exactly what blocks it (which ticket or decision, and what would unblock it); transition it back to **To Do**; release your slot; delete any branch that has no commits. Final message: `KAN-xx blocked: <reason>`. Don't build around the blocker unless the user says so.
