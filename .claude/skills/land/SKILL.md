---
name: land
description: After the user has merged agent PRs, move their KAN tickets to Done with the main commit hash, remove the worktrees and branches, and archive the agent sessions. Use when the user says "land", "I merged", "clean up merged PRs" or similar; /dispatch also runs it first.
---

# /land [KAN-xx …]

Closes out tickets whose PRs the user has **already merged**. You never merge anything yourself. Without arguments, check every KAN ticket that is **In Review**. Jira cloudId `85c8de9e-53b0-41d0-b30e-beb850223a9f`. gh: `& "C:\Program Files\GitHub CLI\gh.exe"`.

## 1. Find merged work

For each ticket in **In Review** (JQL `project = KAN AND status = "In Review"`), or each key given:

1. Find its PRs in both repos: `gh pr list --repo ikant14/bodiala --state all --search "KAN-xx in:title"` (same for `ikant14/bodiala-web`).
2. It is **landed** only when every PR for it is `MERGED`. Note the squash commit on `main` (`gh pr view <n> --json mergeCommit`).
3. A PR that is open stays as it is. A PR that was **closed without merging**: don't touch the ticket; list it for the user to decide.

## 2. Close in Jira

For each landed ticket: comment `Landed on main: <short hash> (<PR link>)` (one line per repo), then transition it to **Done**.

## 3. Clean up

For each landed ticket's branch, in each repo it touched:

1. Find its worktree (`git worktree list`). Skip any worktree with uncommitted changes and tell the user; never discard work.
2. `git worktree remove <path>`, then `git branch -D <branch>` locally. Delete the remote branch only if it still exists (`git push origin --delete <branch>`; GitHub may have done it already).
3. Make sure its slot lock file in `C:\Users\user\IdeaProjects\.agent-slots` is gone.
4. Archive the agent session for that ticket (find it with `list_sessions` by title or worktree path) if session tools are available.

## 4. Report

One line per ticket: `KAN-xx → Done · <hash>`, plus anything skipped and why (open PR, closed unmerged, dirty worktree).
