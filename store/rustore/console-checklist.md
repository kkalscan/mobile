# RuStore Console — финальный чеклист

Тексты: [`copy.md`](copy.md)  
Ассеты: `icon-512.png`, `upload/screenshot-01..05.png` (полный UI 9:16, **загружать в RuStore**)

**Не загружайте** маркетинговые `screenshot-*.png` с обрезанным меню и файлы из `raw/` (1080×2400).

## Автоматически (API)

> **Важно:** если API возвращает `403 This user does not have rights`, проверьте в RuStore Console → Компания → API RuStore, что у ключа включены права **загрузка и публикация приложений**, и что в карточке приложения указан package `ru.kkalscan.app` (сейчас приложение `kkalscan` может быть без привязанного package до первой ручной загрузки AAB).

```bash
cd mobile
# credentials из /opt/rustore-mcp/.env на VPS или GitHub Secrets
export RUSTORE_KEY_ID=...
export RUSTORE_PRIVATE_KEY=...
python3 scripts/rustore_upload_store.py \
  --aab composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Скрипт: черновик → иконка → скриншоты → AAB → модерация.

## Вручную в Console (если API не принял тексты)

1. [console.rustore.ru](https://console.rustore.ru) → **kkalscan** → версия / черновик
2. Вставить поля из `copy.md` (название, краткое и полное описание, FAQ)
3. **Категория:** Здоровье · **12+** · **Бесплатное**
4. **Privacy:** http://91.207.75.72:8080/privacy
5. **Upload-сертификат AAB:** `mobile/.secrets/kkalscan-upload-cert.pem` (если ещё не загружен)
6. **Контакты:** email ИП + VK (минимум одно поле)
7. **Разрешения:** INTERNET — обязательно; камера не используется (выбор фото из галереи)
8. Проверить crop скриншотов в превью консоли
9. Комментарий модератору — из `copy.md`

## После модерации

- CI: `gh workflow run rustore-release.yml --repo kkalscan/mobile`
- Проверить карточку в каталоге RuStore
