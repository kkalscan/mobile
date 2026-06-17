# RuStore store assets — KkalScan

## Файлы

| Файл | Назначение |
|------|------------|
| `icon-512.png` | Иконка 512×512 для витрины |
| `screenshot-01-diary.png` … `05-limit.png` | Скриншоты 1080×1920 (9:16) |
| `copy.md` | Тексты для RuStore Console |
| `console-checklist.md` | Чеклист публикации |

## Перегенерация графики

**Из приложения на устройстве (рекомендуется):**

```bash
# 1. Сохранить raw-скрины в store/rustore/raw/ (adb или mobile-mcp)
#    01-diary.png, 02-result.png, 03-camera.png, 04-journal.png, 05-profile.png

cd mobile/store/rustore
python3 -m venv .venv && .venv/bin/pip install Pillow
.venv/bin/python frame_device_screenshots.py
# → screenshot-01-diary.png … 05-limit.png (1080×1920)
# → tablet/screenshot-*-landscape.png (1920×1200)
```

**Мокапы (без устройства):**

```bash
cd mobile/store/rustore
.venv/bin/python generate_assets.py
```

## Загрузка в RuStore

```bash
cd mobile
python3 -m venv .venv-rustore && .venv-rustore/bin/pip install pycryptodome
export RUSTORE_KEY_ID=... RUSTORE_PRIVATE_KEY=...
.venv-rustore/bin/python scripts/rustore_upload_store.py \
  --aab composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Если API возвращает 403 — загрузите AAB и медиа вручную через [console.rustore.ru](https://console.rustore.ru) по `console-checklist.md`.
