# Working on bodiala in parallel (agents: read this first)

Several agents can work on this project at the same time, each on its own Jira task. This file covers:

- how to pick and claim a task;
- where to work, and how to run the apps and tests without colliding with the other agents;
- how to hand the work back.

For the project itself (architecture, gotchas, build commands), read `CLAUDE.md` in this repo. For the other two repos, read `../bodiala-web/CLAUDE.md` and `../hotelbeds-stub/CLAUDE.md`.

| Repo | Path | Git | Default ports |
|---|---|---|---|
| **bodiala** (backend) | `C:\Users\user\IdeaProjects\bodiala` | yes, `github.com/ikant14/bodiala` | 8080 |
| **bodiala-web** (frontend) | `C:\Users\user\IdeaProjects\bodiala-web` | yes, `github.com/ikant14/bodiala-web` | 5173 |
| **hotelbeds-stub** (fake Hotelbeds) | `C:\Users\user\IdeaProjects\hotelbeds-stub` | **no** (see §3) | 9091 |

Postgres is a single Docker container, `bodiala-postgres` on :5432, shared by everyone.

## 1. Pick a task

Jira project **KAN** (<https://iraklikantar.atlassian.net>) is the source of truth. Each task's description has its Why, What and an acceptance checklist. `docs/backlog.md` has the same text, plus the full ranked order.

- **Only take work from these two epics:**
  - **App · now** (KAN-151)
  - **Infra · now (local Docker)** (KAN-153)

  Take the active sprint first, then the backlog from the top down (it is ranked). The "· later" epics and **Legal & business · later** wait until the user starts phase 2. Don't touch them unless the user asks.
- **A task is ready when:**
  - its status is **To Do**;
  - every issue that it "is blocked by" is **Done**.

  There are two exceptions:
  - **Undecided phase-1 decision** (DEC-3, DEC-5, DEC-6, DEC-7 or DEC-8): you may build on that decision's written recommendation. Say so when you hand back.
  - **Links to PAY-2 and PAY-6**: these only matter once online payment is added. Ignore them (see "Working order" in `docs/backlog.md`).
- **Decisions (DEC-*) belong to the user.** Never move or close one.
- **Prefer a different lane** from the tasks already **In Progress** (§7), so that agents don't edit the same files.
- **Useful JQL:**
  - `project = KAN AND sprint in openSprints() AND status = "To Do" ORDER BY Rank`
  - `project = KAN AND parent in (KAN-151, KAN-153) AND status = "To Do" ORDER BY Rank`
  - `project = KAN AND status = "In Progress"` shows what the other agents are doing.

## 2. Claim it

1. Re-read the issue just before claiming. If it is no longer **To Do**, pick another task.
2. Move it to **In Progress**, and add a comment: `Claimed by <your session name> · slot <n> · branch <branch> · <date>`.
3. Claim a slot (§4).

All agents use the user's Jira account, so the claim comment, not the assignee field, says who owns a task.

## 3. Work in your own worktree, on your own branch

- **Never edit the main checkouts** (`C:\Users\user\IdeaProjects\bodiala` and `...\bodiala-web`). They belong to the user, and other agents branch from them.
- **Branch name:** `kan-<number>-<short-slug>`, for example `kan-84-flyway`. When a task touches both repos, use the same branch name in each.
- **Base branch:** `main`.

  > **Until `hotel-card-photos` is merged into `main`, branch from `hotel-card-photos`.** In both repos, `main` is still behind it: bodiala-web's `main` is only the initial commit. Ask the user if unsure.
- **Create the worktrees.** If your harness creates a worktree for you, use that instead.
  ```powershell
  $key = "kan-84"; $branch = "kan-84-flyway"; $base = "hotel-card-photos"   # or main, once merged
  git -C C:\Users\user\IdeaProjects\bodiala worktree add "C:\Users\user\IdeaProjects\wt\$key\bodiala" -b $branch $base
  git -C C:\Users\user\IdeaProjects\bodiala-web worktree add "C:\Users\user\IdeaProjects\wt\$key\bodiala-web" -b $branch $base
  Set-Location "C:\Users\user\IdeaProjects\wt\$key\bodiala-web"; npm.cmd ci      # worktrees have no node_modules
  ```
  Only create a worktree for a repo your task actually changes.
- **hotelbeds-stub is not under git.** It has no branches and no worktrees:
  - Only an agent whose task needs a stub change may edit it, and only one agent at a time. Say "editing hotelbeds-stub" in your claim comment.
  - Before you start, check that no In Progress claim says the same.
  - Everyone else only runs it (§4).
- **Clean up** once the user has merged your branch: `git worktree remove <path>`, then delete the branch.

## 4. Run the apps and tests in a slot

Ports, the dev database and the test schema are per **slot**. Postgres is shared: one container serves everyone.

| Slot | bodiala | stub | web (Vite) | Dev database | Test schema |
|---|---|---|---|---|---|
| 0 (the user) | 8080 | 9091 | 5173 | `bodiala` | `bodiala_test` |
| 1 | 8180 | 9191 | 5273 | `bodiala_s1` | `bodiala_test_s1` |
| 2 | 8280 | 9291 | 5373 | `bodiala_s2` | `bodiala_test_s2` |
| 3 | 8380 | 9391 | 5473 | `bodiala_s3` | `bodiala_test_s3` |
| 4 | 8480 | 9491 | 5573 | `bodiala_s4` | `bodiala_test_s4` |

At most four agents run at once. Slot 0 and `dev.ps1` are the user's.

**Claim a slot** by creating its lock file. The command fails if the slot is taken; if so, try the next one.

```powershell
New-Item -ItemType Directory -Force C:\Users\user\IdeaProjects\.agent-slots | Out-Null
New-Item -ItemType File C:\Users\user\IdeaProjects\.agent-slots\slot-2 -Value "KAN-84 kan-84-flyway" -ErrorAction Stop
```

**Run things in your slot.** Set `$slot` first:

```powershell
$slot = 2
$api = 8080 + 100*$slot; $stub = 9091 + 100*$slot; $web = 5173 + 100*$slot
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"

# Once per slot: your own dev database ("already exists" is fine).
docker exec bodiala-postgres createdb -U bodiala "bodiala_s$slot"

# Backend tests, in your own schema (run in your bodiala worktree). Clear the variable afterwards.
$env:SPRING_APPLICATION_JSON = "{`"spring.jpa.properties.hibernate.default_schema`":`"bodiala_test_s$slot`"}"
.\gradlew.bat test
Remove-Item Env:SPRING_APPLICATION_JSON

# hotelbeds-stub: run your own copy of its jar. Two `gradlew bootRun`s in one folder would block each other.
# Build the jar first if build\libs is empty, and only if no other agent is building or editing the stub:
#   Set-Location C:\Users\user\IdeaProjects\hotelbeds-stub; .\gradlew.bat bootJar
$jar = Get-ChildItem C:\Users\user\IdeaProjects\hotelbeds-stub\build\libs\*.jar | Where-Object Name -notlike '*-plain.jar' | Select-Object -First 1
Copy-Item $jar.FullName "$env:TEMP\hotelbeds-stub-s$slot.jar" -Force
& "$env:JAVA_HOME\bin\java.exe" -jar "$env:TEMP\hotelbeds-stub-s$slot.jar" --server.port=$stub

# Backend (in your bodiala worktree), against your stub and your database.
.\gradlew.bat bootRun --args="--spring.profiles.active=hotelbeds-stub --server.port=$api --hotelbeds.base-url=http://localhost:$stub --spring.datasource.url=jdbc:postgresql://localhost:5432/bodiala_s$slot"

# Frontend (in your bodiala-web worktree), proxying /api to your backend.
$env:VITE_API_TARGET = "http://localhost:$api"; npm.cmd run dev -- --port $web --strictPort
```

Run the servers in the background, and stop them by the process IDs you started. When you are done, delete your lock file.

### Never

- **Never run `dev.ps1`, `docker compose up`, `docker compose down` or `docker compose down -v` from a worktree.** Never stop, recreate or rename `bodiala-postgres`: every agent and the user depend on it, and `down -v` wipes everyone's data. (OPS-11, which changes `docker-compose.yml`, must test with its own compose project name and host ports.)
- **Never kill a process you didn't start.** The "kill the port" tip in `CLAUDE.md` is for slot 0 only.
- **Never call the real Hotelbeds API.** Its quota is 50 requests a day, shared by everyone. Use the stub. If the user explicitly asks for a live check, count your calls.
- **Never copy, print or commit `.env` or any credential.** The stub profile needs none.
- **Never commit to `main`, `hotel-card-photos` or another task's branch.** Never force-push a shared branch.

## 5. Files that many tasks touch

| File or area | Rule |
|---|---|
| `build.gradle`, `package.json`, `package-lock.json` | Add only your own lines, and don't reorder. If the lockfile conflicts after a rebase, regenerate it with `npm.cmd install`. |
| `application*.properties` | Add your keys as their own block, and don't reformat. |
| Database schema | Until OPS-1 (Flyway) is merged, change it through the JPA entities (`ddl-auto=update`). After that, name migrations with a timestamp version, so parallel branches can't collide: `V2026_11_08_1530__add_booking_request.sql`. Never edit a migration that is already on the base branch. |
| `ApiExceptionHandler`, `api.ts`, `types.ts`, `styles.css`, `SearchView.tsx` | These change in many tasks. Keep diffs small and focused, and rebase before handing back. |
| JSON the frontend reads | If your backend change alters it, change both repos in the same task (same branch name), or keep the old shape working. |
| `CLAUDE.md` | Update only the lines your task makes wrong or adds (status, gotchas). |
| `docs/backlog.md` | The "Working order" section is generated by `research_notes/Bodiala B2C launch backlog/tools/`, so don't hand-edit it. Progress lives in Jira, not here. |

## 6. Hand back

1. **Done means:**
   - the acceptance checklist is met;
   - `gradlew test` is green in your own schema;
   - `npm.cmd run build` is green if you touched the frontend;
   - user-visible behaviour has been checked in your slot's running app.
2. **Commit** on your task branch, with the Jira key in the subject line: `KAN-84: Adopt Flyway and validate the schema`. Rebase onto the latest base branch, then run the tests again.
3. **Push and open a pull request only if the user asked for it in your session.** The user merges.
4. **In Jira:**
   - Comment with the branch name(s), what changed, the test results, any acceptance item not met, and any decision recommendation you relied on.
   - Move the task to **In Review**. The user moves it to **Done** after merging.
   - Don't edit the description; report checklist status in your comment.
5. **Clean up:** stop your processes and delete your slot's lock file. Keep the worktree until your branch is merged.
6. **If you are blocked** (a missing decision, or a clash with another agent's work):
   - say so in a comment on the issue;
   - move the task back to **To Do** if you can't continue;
   - tell the user.

## 7. Lanes you can run in parallel (snapshot, 2026-11-08)

These are ready now, and each lane mostly touches different code. Run one agent per lane, working down the lane in order.

| Lane | Tasks, in order | Mostly touches |
|---|---|---|
| **A · data foundation** (critical path) | OPS-1 (KAN-84) → OPS-7 (KAN-104) → BOOK-1 (KAN-77) → BOOK-6 (KAN-91), BOOK-7 (KAN-94), BOOK-2 (KAN-80) | `build.gradle`, migrations, `booking` package |
| **B · observability** | OPS-5 (KAN-98) → OPS-6 (KAN-101) | Actuator, metrics, logging config |
| **C · security and validation** | SEC-1 (KAN-47) → SEC-5 (KAN-59) → SEC-3 (KAN-53) | Security config, request records, `staticdata` |
| **D · Hotelbeds client** | HB-6 (KAN-36) → HB-13 (KAN-60) → HB-14 (KAN-61) | `provider.hotelbeds` client and DTOs |
| **E · frontend accessibility** | A11Y-1 (KAN-120) → A11Y-2 (KAN-123) → A11Y-3 (KAN-128) → A11Y-4 (KAN-131) | bodiala-web components and styles |
| **F · local Docker** | OPS-11 (KAN-156), SEC-9 (KAN-72) | Dockerfiles, `docker-compose.yml` |

**Waiting on a decision:**
- BOOK-3 (DEC-3, DEC-5);
- HB-5 and CHK-6 (DEC-3);
- HB-4 (DEC-6).

A11Y-5 touches both repos; take it after lane E. Once these lanes are done, rebuild the list from Jira: take the ready tasks from §1 and group them by the code they touch.
