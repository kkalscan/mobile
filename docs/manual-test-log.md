# Журнал ручного тестирования KkalScan

| Дата | Сборка | Платформа | Результат |
|------|--------|-----------|-----------|
| 2026-06-14 | 0.1.0-dev | wasmJs web | Maestro 2/2 ✅ |

## Чеклист MVP (из [mvp-functional-requirements.md](./mvp-functional-requirements.md))

### E3 — Дневник «Сегодня»
- [ ] Открывается стартовым экраном
- [ ] Показывает сумму ккал за день
- [ ] Показывает «осталось сканов»
- [ ] Список приёмов пищи с блюдами
- [ ] Pull/кнопка обновления при ошибке сети

### E1 — Скан
- [ ] Градиент hero (#FF7A2F → #FF4D8D)
- [ ] Выбор фото (web: file input; Android: галерея — TODO)
- [ ] Индикатор загрузки во время POST /scan

### E2 — Результат
- [ ] Крупные ккал, чипы Б/Ж/У
- [ ] Список блюд
- [ ] Выбор типа приёма пищи
- [ ] «Добавить в дневник» → возврат в E3

### E4 — Paywall
- [ ] Появляется при исчерпании лимита
- [ ] «Смотреть рекламу» → POST /scan/bonus
- [ ] Карточка Pro 199 ₽ (stub)

## Сравнение с design-system

Скриншоты складывать в `docs/screenshots/YYYY-MM-DD/`:

| Экран | Ожидание (Citrus Scan) | Факт | OK |
|-------|------------------------|------|-----|
| E3 | primary #FF7A2F, фон #FFFCF8 | | |
| E1 | scanHero gradient | | |
| E4 | proBanner gradient | | |

## Maestro (web wasmJs)

```bash
# 1. Поднять web
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# 2. Прогнать E2E (Maestro 2.6+, platform web)
maestro test -p web --headless --screen-size 390x844 maestro/
```

Flows: `maestro/web-diary-smoke.yaml`, `maestro/web-scan-navigation.yaml`  
**2026-06-14:** 2/2 passed (diary load + scan navigation).

Compose WASM рисует в canvas — для Maestro добавлены DOM-хуки `#maestro-hook` и кнопки `#maestro-open-scan` / `#maestro-scan-back`.

## Maestro (Android, когда будет APK)

```yaml
# maestro/diary-smoke.yaml (черновик)
appId: ru.kkalscan.app
---
- launchApp
- assertVisible: "Сегодня"
- tapOn: "📷"
```

## API smoke (curl)

```bash
curl -s http://91.207.75.72:8080/health
curl -s -H "X-Device-Id: $(uuidgen)" http://91.207.75.72:8080/api/v1/subscription/status
```

## Заметки

- **2026-06-14:** Клиент wasmJs поднят для ручных прогонов против prod API. Tochka/VK — stub на backend.
