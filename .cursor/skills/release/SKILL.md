---
name: release
description: Release kkalscan/mobile to RuStore. Use when the user says "release", "выпусти релиз", "publish to RuStore", or asks to verify automatic RuStore publication.
---

# Release

Release `kkalscan/mobile` through GitHub Actions and verify the new version in RuStore.

## Constants

| Parameter | Value |
|---|---|
| Repository | `kkalscan/mobile` |
| Working directory | `mobile` |
| Main branch | `main` |
| Build workflow | `Run` / `run.yml` |
| Release workflow | `RuStore Release` / `rustore-release.yml` |
| Package name | `ru.kkalscan.app` |
| RuStore publish type | `INSTANTLY` |
| Timeout | 25 min |

## Checklist

```
- [ ] 1. Confirm the release commit is on origin/main
- [ ] 2. Wait for the Run workflow for that commit to succeed
- [ ] 3. Resolve next versionName/versionCode
- [ ] 4. Dispatch RuStore Release workflow
- [ ] 5. Wait for conclusion: success
- [ ] 6. Verify the new RuStore version exists
- [ ] 7. Report workflow URL, versionId, version status, and publishType
```

## Steps

### 1. Confirm release commit

Work from `mobile`.

Run:

```bash
git status --short --branch
git rev-parse HEAD
git log -5 --oneline --decorate
```

Only release committed work that is already on `origin/main`. Do not commit unrelated dirty files as part of release. If `main` is ahead, push only if the user asked for push/release and the staged commit is intended for release.

### 2. Wait for `Run`

Find the `Run` workflow for the release commit:

```bash
gh run list --workflow run.yml --branch main --limit 10
```

Pick the run whose head SHA matches `git rev-parse HEAD`.

Wait:

```bash
gh run watch <run_id> --exit-status
```

If it fails, stop and inspect logs. Do not start RuStore release from a commit whose main build failed.

### 3. Resolve version

Check RuStore before choosing the next version:

- Use RuStore MCP `status` with `testAuth: true`.
- Use RuStore MCP `list_versions` with `packageName: "ru.kkalscan.app"` and `size: 10`.

Default to the next patch version after the latest active/moderation version. For `X.Y.Z`, derive `versionCode` as `X * 10000 + Y * 100 + Z`.

Example:

```text
1.0.0 -> 1.0.1, versionCode 10001
```

If the user explicitly requested a version, use it. If the computed `versionCode` would not increase over existing versions, stop and choose the next valid code.

### 4. Dispatch RuStore release

Use clear release notes. For screenshot/card-only releases, use:

```text
Обновлены скриншоты RuStore и оформление карточки приложения.
```

Run:

```bash
gh workflow run rustore-release.yml --ref main \
  -f version_name=<version_name> \
  -f version_code=<version_code> \
  -f whats_new='<release_notes>'
```

Then find the new run:

```bash
gh run list --workflow rustore-release.yml --limit 5 \
  --json databaseId,status,conclusion,headSha,event,displayTitle,createdAt,url,headBranch
```

The run must be on `main` and match the release commit SHA.

### 5. Wait for release

Wait:

```bash
gh run watch <release_run_id> --exit-status
```

Success means the workflow completed `Build signed AAB` and `Publish to RuStore`. If it fails, fetch failed logs and report the failing step. Do not retry blindly; version conflicts may leave a draft in RuStore.

### 6. Verify RuStore

After workflow success, call RuStore MCP `list_versions` again:

```json
{
  "packageName": "ru.kkalscan.app",
  "size": 10
}
```

Find the version with the released `versionName` and `versionCode`.

Acceptable result:

- `versionStatus` is `MODERATION`, `TAKEN_FOR_MODERATION`, `READY_FOR_PUBLICATION`, or `ACTIVE`.
- `publishType` is `INSTANTLY`.
- `sendDateForModer` is present for moderation states.

`MODERATION` with `publishType: INSTANTLY` means the automatic RuStore publication path is set: after moderation, RuStore should publish automatically.

### 7. Report

Report concisely:

- GitHub Actions run URL and conclusion.
- RuStore `versionName`, `versionCode`, `versionId`.
- `versionStatus`, `publishType`, and whether automatic publication is configured.
- Any remaining dirty working tree files that were intentionally not touched.

## Errors

- **Run workflow failed**: stop before RuStore release.
- **RuStore auth failed**: report that credentials/API access must be fixed.
- **Release workflow failed**: inspect failed logs, report root cause, and do not claim publication.
- **Version not found in RuStore after success**: retry `list_versions` once after a short wait, then report the mismatch with workflow URL.
- **Status rejected**: report `REJECTED_BY_MODERATOR` and the versionId; the release reached RuStore but did not pass moderation.
