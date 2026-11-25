# Contributing to Personal Stock Tracker

## Branches

| Branch | Purpose | Who merges into it |
| --- | --- | --- |
| `main` | Released code only; every commit is tagged (`v1.0.0`) | Lead, from `release/*` or `hotfix/*` |
| `develop` | Integration branch; must always build | Anyone, via an approved PR |
| `feature/<area>-<topic>` | One feature, e.g. `feature/watchlist-remove-undo` | → `develop` |
| `fix/<topic>` | One bug fix | → `develop` (or `release/*` during a release) |
| `test/*`, `build/*`, `ci/*`, `chore/*` | Tests, Gradle, workflows, housekeeping | → `develop` |
| `release/<version>` | Final testing before a release | → `main` (merge commit) |
| `hotfix/<version>` | Urgent fix after a release | → `main` (merge commit) |

Areas: `core`, `data`, `domain`, `watchlist`, `portfolio`, `details`, `alerts`, `worker`, `ui`.

## Commit messages — Conventional Commits

```text
<type>(<scope>): <summary in the imperative, max 72 characters>

<optional body: why and what>

Refs: FR-W2, #12
```

Types: `feat`, `fix`, `test`, `docs`, `style`, `refactor`, `perf`, `build`, `ci`, `chore`.
Run `git config commit.template .gitmessage` once to get the template in your editor.

## Pull requests

1. Branch from an up-to-date `develop`.
2. Keep PRs small (ideally < 400 changed lines) and about **one** thing.
3. **PR title = the final commit message** — we use *Squash and merge*, so the title becomes the single commit on `develop`.
4. Fill in the PR template; link the issue with `Closes #<n>`.
5. CI (`build`) must be green and one teammate must approve.
6. The author merges after approval and deletes the branch.

Reviewers: check that it builds, works on the emulator, has no hard-coded strings or secrets, and follows MVVM (no API/DB calls in Activities).

## Daily routine

```bash
git checkout develop && git pull
git checkout feature/<my-branch>
git merge develop            # resolve conflicts now, while they are small
git push
```

Do **not** rebase or force-push branches that someone else has pulled.

## Never commit

`local.properties`, `keystore.properties`, `*.jks`, API keys, `build/` folders, APK/AAB files. Check `git status` before every commit.

## Shared files

`AndroidManifest.xml`, `build.gradle.kts`, `libs.versions.toml`, `strings.xml`, `AppDatabase.java`: change them in small, separate PRs and merge them the same day. Bottom navigation and toolbar menus are owned by Person B.

