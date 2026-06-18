---
name: run-apk
description: >-
  After push to main in kkalscan/mobile, waits for GitHub Actions workflow Run,
  downloads artifact via GitHub MCP, installs and launches on Android via mobile-mcp.
  Use when user says run-apk, after pushing mobile to main, or asks to install debug APK on device.
---

# Run APK

Push to `main` → wait for workflow **Run** → download APK → install and launch on Android.

## Constants

| Parameter | Value |
|---|---|
| GitHub owner/repo | `kkalscan/mobile` |
| Workflow name / file | **Run** / `run.yml` |
| Artifact name | `run` |
| APK path | `dist/composeApp-debug.apk` |
| Package name | `ru.kkalscan.app` |
| Poll interval | ~30s |
| Timeout | 25 min |

## Prerequisites

- Workflow `run.yml` is on `main`
- Android device connected (`mobile_list_available_devices`)
- MCP servers: `github`, `mobile-mcp`

## Checklist

```
- [ ] 1. Push to main done (or user confirmed commit SHA)
- [ ] 2. Find workflow run by head_sha
- [ ] 3. Wait for conclusion: success
- [ ] 4. Download artifact via GitHub MCP
- [ ] 5. Unzip → dist/composeApp-debug.apk
- [ ] 6. mobile_list_available_devices → pick Android
- [ ] 7. mobile_install_app (uninstall release first if INSTALL_FAILED_UPDATE_INCOMPATIBLE)
- [ ] 8. mobile_launch_app (ru.kkalscan.app)
- [ ] 9. Report result (+ optional screenshot)
```

## Steps

### 1. Commit SHA

After push to `main`, note `head_sha` (`git rev-parse HEAD` or from push context).

Skill does **not** trigger the workflow — push to `main` starts it automatically.

### 2. Find run

GitHub MCP `actions_list`:
- `method`: `list_workflow_runs`
- `owner`: `kkalscan`, `repo`: `mobile`
- `resource_id`: `run.yml`
- `workflow_runs_filter`: `{ "branch": "main", "event": "push" }`

Pick the run where `head_sha` matches the pushed commit.

If run is not found yet, wait ~10s and retry (workflow may still be queued).

### 3. Wait for build

GitHub MCP `actions_get` → `get_workflow_run` every ~30s:

- `status: completed` + `conclusion: success` → continue
- `conclusion: failure` → `get_job_logs`, report error, **stop**
- Timeout 25 min → report and **stop**

If GitHub MCP is unavailable, use `gh run watch` and `gh run download -n run` as fallback.

### 4. Download artifact

GitHub MCP `actions_list` → `list_workflow_run_artifacts`:
- `resource_id`: successful run ID
- Find artifact with `name: run`

GitHub MCP `actions_get` → `download_workflow_run_artifact`:
- `resource_id`: artifact ID

Unzip to `dist/composeApp-debug.apk` (create `dist/` if missing).

Use absolute path for install step.

### 5. Install and launch

mobile-mcp:
1. `mobile_list_available_devices` — prefer physical Android; ask user if multiple
2. `mobile_install_app` — `device`, `path`: absolute path to APK
3. If `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: `mobile_uninstall_app` (`bundle_id`: `ru.kkalscan.app`), then retry install
4. `mobile_launch_app` — `packageName`: `ru.kkalscan.app`
5. Optional: `mobile_take_screenshot` to verify

## Errors

- **No Android device**: stop, ask user to enable USB debugging
- **Build failed**: fetch job logs via GitHub MCP; do not install stale APK
- **Artifact missing**: verify run succeeded and artifact name is `run`
- **Timeout 25 min**: stop polling, suggest checking Actions tab
