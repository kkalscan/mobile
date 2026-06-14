# Архитектура клиента KkalScan

KMP-приложение: **shared** (логика) + **composeApp** (UI). Целевые платформы: **Android (RuStore)** и **wasmJs** (ручное тестирование в браузере).

## Модули

| Модуль | Назначение |
|--------|------------|
| `shared` | API-клиент, репозитории, view models, модели |
| `composeApp` | Compose Multiplatform UI, тема Citrus Scan, навигация |

## Интерфейсы (каждый класс — через интерфейс)

| Интерфейс | Реализация |
|-----------|------------|
| `IKkalScanApi` | `KkalScanApi` |
| `IApiConfig` | `DefaultApiConfig` |
| `IDeviceIdStorage` | `InMemoryDeviceIdStorage` |
| `IDiaryRepository` | `DiaryRepository` |
| `IScanRepository` | `ScanRepository` |
| `ISubscriptionRepository` | `SubscriptionRepository` |
| `IDiaryViewModel` | `DiaryViewModel` |
| `IScanViewModel` | `ScanViewModel` |

## Слои

```
composeApp (экраны E1–E4)
    ↓ StateFlow
shared/presentation (ViewModels)
    ↓ suspend
shared/data/repository
    ↓
shared/data/api (Ktor → http://91.207.75.72:8080/api/v1)
```

## Навигация (MVP)

Старт: **E3 Дневник** → FAB → **E1 Скан** → **E2 Результат** → дневник.  
При `limit_hit` → **E4 Paywall** (реклама stub / Pro stub).

## TDD

- Kotest + `kotlin.test` в `shared/src/commonTest`
- Порядок: тест репозитория/VM → реализация → экран

## Запуск web preview

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack
# затем dev-сервер (если нужен hot reload):
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

## Maestro (Android E2E, позже)

Сценарии описаны в [manual-test-log.md](./manual-test-log.md). Flows: diary → scan stub → result → add entry.

## Backend

Prod API: `http://91.207.75.72:8080/api/v1/`  
Tochka / VK — только после Pro; на клиенте пока stub.
