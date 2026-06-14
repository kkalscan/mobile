# KkalScan (КкалСкан)

MVP: калории по фото + дневник на сервере. Market test в RuStore.

## Решения

| Параметр | Значение |
|----------|----------|
| Бренд | **KkalScan** / **КкалСкан** |
| API | `http://91.207.75.72:8080/api/v1/` (DNS отложен) |
| Платформа | Android, RuStore |
| Оплата Pro | Точка API — **после HTTPS**; пока тест по IP `/pay` |
| Монетизация free | Rewarded video (Яндекс) + Pro 199 ₽/мес |
| Backend | Docker `kkalscan-api` на `91.207.75.72:8080` |

## Документация

| Файл | Описание |
|------|----------|
| [docs/design-system.md](docs/design-system.md) | Цвета, шрифты, компоненты (Citrus Scan) |
| [docs/brand-dns.md](docs/brand-dns.md) | Бренд, API `91.207.75.72:8080`, ASO |
| [docs/mvp-functional-requirements.md](docs/mvp-functional-requirements.md) | Функциональные требования v0.1 |
| [docs/auth-after-pro.md](docs/auth-after-pro.md) | Авторизация после покупки Pro (VK/Yandex) |
| [docs/test-plan-rustore-calories-photo.md](docs/test-plan-rustore-calories-photo.md) | План market test |
| [docs/niche-calories-by-photo-ru.md](docs/niche-calories-by-photo-ru.md) | Ниша и конкуренты |
| [docs/competitor-reviews-traffic-analysis.md](docs/competitor-reviews-traffic-analysis.md) | Отзывы и боли рынка |
| [docs/niche-calorie-photo-ru.md](docs/niche-calorie-photo-ru.md) | Ранняя версия анализа ниши |
