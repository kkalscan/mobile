---
name: release-apk
description: >-
  Release kkalscan/mobile to RuStore: always bump VERSION in gradle.properties,
  push, wait for CI Run (tests) success, then publish. Use when the user says
  release-apk, release, выпусти релиз, publish to RuStore, or asks to ship a store build.
---

# Release APK

**Always bump version** → commit/push → **wait for CI `Run` success** → publish signed AAB to RuStore.

Profile, CI debug APK, and RuStore all read `VERSION_NAME` / `VERSION_CODE` from `gradle.properties`.

## Constants

| Parameter | Value |
|---|---|
| Repository | `kkalscan/mobile` |
| Working directory | `mobile` |
| Main branch | `main` |
| Version file | `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`) |
| CI workflow | `Run` / `run.yml` (includes `:shared:jvmTest`) |
| Release workflow | `RuStore Release` / `rustore-release.yml` |
| Package name | `ru.kkalscan.app` |
| RuStore publish type | `INSTANTLY` |
| Timeout | 25 min |
| MCP | `github`, `rustore` |

## Hard rules

1. **Version bump is mandatory.** Every release-apk run must write a new `VERSION_NAME` / `VERSION_CODE` into `gradle.properties` and commit it before CI/publish. Never publish the same version already `ACTIVE` (or in moderation) in RuStore. Never skip the bump and only dispatch the workflow.
2. **CI gate.** Do not dispatch `rustore-release.yml` until `run.yml` for the release commit has `conclusion: success`. On CI failure/timeout — stop, report logs, do not publish.

## Checklist

```
- [ ] 1. Confirm working tree on main
- [ ] 2. Resolve next VERSION_NAME / VERSION_CODE (must be higher than RuStore + file)
- [ ] 3. Bump gradle.properties (mandatory); commit; push origin/main
- [ ] 4. Wait for Run on that SHA → success (tests passed)
- [ ] 5. Dispatch RuStore Release (empty version inputs → read gradle.properties)
- [ ] 6. Wait for release workflow → success
- [ ] 7. Verify RuStore list_versions
- [ ] 8. Report URLs + versionName/Code + status
```

## Steps

### 1. Working tree

Work from `mobile`:

```bash
git status --short --branch
git rev-parse HEAD
git log -5 --oneline --decorate
```

Do not mix unrelated dirty files into the version commit.

### 2. Resolve next version (required bump)

Read file:

```bash
grep -E '^VERSION_(NAME|CODE)=' gradle.properties
```

RuStore MCP `list_versions` (`packageName: "ru.kkalscan.app"`, `size: 10`).

**Default:** next **patch** after `max(file VERSION_NAME, latest RuStore active/moderation versionName)`.

| From | To | versionCode |
|---|---|---|
| `X.Y.Z` | `X.Y.(Z+1)` | `X * 10000 + Y * 100 + (Z+1)` |

Example: file/store `1.0.13` → bump to `1.0.14` / `10014`.

If the user requests an explicit higher version, use it. If computed `versionCode` ≤ any existing RuStore code, stop and choose a higher code.

**Refuse to continue** if the chosen version equals the current store or file version without an increment.

### 3. Bump, commit, push (mandatory)

Edit `gradle.properties`:

```properties
VERSION_NAME=<next>
VERSION_CODE=<next_code>
```

```bash
git add gradle.properties
git commit -m "$(cat <<'EOF'
Bump app version to <VERSION_NAME>.

EOF
)"
git push origin main
```

`RELEASE_SHA=$(git rev-parse HEAD)`.

The release commit **must** include the version bump. Feature work can be in earlier commits on `main`; this skill always adds (or rides with) a version bump commit before publish.

### 4. Wait for CI (`Run`) — required

```bash
gh run list --workflow run.yml --branch main --limit 10 \
  --json databaseId,headSha,status,conclusion,url,displayTitle,createdAt
```

Match `headSha == RELEASE_SHA`. Retry ~10s if not queued yet.

```bash
gh run watch <run_id> --exit-status
```

- **success** → continue  
- **failure / cancelled / timeout** → `gh run view <run_id> --log-failed`, **stop**, no RuStore

Green `Run` = tests (`:shared:jvmTest`) passed for this versioned commit.

### 5. Dispatch RuStore release

Prefer workflow reading bumped `gradle.properties`:

```bash
gh workflow run rustore-release.yml --ref main \
  -f whats_new='<release_notes>'
```

Optional explicit (must match the file just bumped):

```bash
gh workflow run rustore-release.yml --ref main \
  -f version_name=<VERSION_NAME> \
  -f version_code=<VERSION_CODE> \
  -f whats_new='<release_notes>'
```

New run must be on `main` with `headSha == RELEASE_SHA`:

```bash
gh run list --workflow rustore-release.yml --limit 5 \
  --json databaseId,status,conclusion,headSha,event,displayTitle,createdAt,url,headBranch
```

### 6. Wait for release

```bash
gh run watch <release_run_id> --exit-status
```

On failure: logs + root cause; do not claim publication; do not blind-retry.

### 7. Verify RuStore

`list_versions` → find new `versionName` / `versionCode`.

Acceptable: `MODERATION`, `TAKEN_FOR_MODERATION`, `READY_FOR_PUBLICATION`, `ACTIVE`; `publishType` `INSTANTLY`.

### 8. Report

- Bumped `VERSION_NAME` / `VERSION_CODE`
- `Run` URL + conclusion
- `RuStore Release` URL + conclusion
- RuStore `versionId`, `versionStatus`, `publishType`

## Errors

| Case | Action |
|---|---|
| Would not bump version | **Stop.** Always increment. |
| CI `Run` failed | **Stop.** No RuStore dispatch. |
| versionCode not increasing | **Stop.** Pick next valid code. |
| Release workflow failed | Logs; no publication claim. |

## Relation to other skills

- Preferred over `release` for new publishes.
- Device install of debug APK: `run-apk` after green `Run`.
