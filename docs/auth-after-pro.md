# Авторизация после покупки Pro

Документ фиксирует модель: **free-пользователь без входа**, **привязка аккаунта — после оплаты Pro**.

**Связанные документы:** [mvp-functional-requirements.md](./mvp-functional-requirements.md), [test-plan-rustore-calories-photo.md](./test-plan-rustore-calories-photo.md)

| Поле | Значение |
|------|----------|
| Версия | 1.0 |
| Дата | 2026-06-14 |
| Статус | Принято для v0.1 |

---

## 1. Решение

| До Pro | После Pro |
|--------|-----------|
| Только `device_id`, дневник и лимиты на сервере | Pro активен сразу на текущем `device_id` |
| Вход не предлагается | Экран **E5 «Привяжите аккаунт»** (один раз + из настроек) |
| — | VK ID / Yandex ID → `user_id`, merge данных |

**Зачем:** не режем воронку до скана; платящий пользователь получает Pro мгновенно; аккаунт нужен для **восстановления Pro и дневника** на новом телефоне.

**Не делаем:** обязательный вход до скана, вход для free-пользователей.

---

## 2. Роли

| Роль | Идентификация | Pro | Восстановление на новом телефоне |
|------|---------------|-----|----------------------------------|
| **Гость** | `device_id` | нет | только если сохранился UUID на устройстве |
| **Pro (гость)** | `device_id` + `is_pro` | да | **нет** — пока не привязан аккаунт |
| **Pro (привязан)** | JWT + `user_id`, также `device_id` | да | **да** — вход VK/Яндекс |

---

## 3. User flow

### 3.1. Покупка Pro

```
E4 → Custom Tabs /pay?device_id=…
        ↓
Webhook Точки → devices.pro_until
        ↓
Приложение: GET /subscription/status → is_pro: true
        ↓
Безлимит сканов на этом телефоне (как раньше)
        ↓
Показ E5 «Привяжите аккаунт» (если user_id ещё нет)
```

Pro **не блокируется** ожиданием входа — оплатил → сканируешь.

### 3.2. Экран E5

| Элемент | Текст / действие |
|---------|------------------|
| Заголовок | «Pro активирован» |
| Подзаголовок | «Привяжите аккаунт, чтобы не потерять подписку и дневник при смене телефона» |
| CTA 1 | **Войти через VK** |
| CTA 2 | **Войти через Яндекс** |
| Вторично | «Позже» → закрыть, баннер в E3 до привязки |

**v0.1 для теста:** достаточно **VK ID**; Yandex — v0.1.1. Google / Telegram — v0.2.

### 3.3. Привязка (merge)

```
POST /api/v1/auth/{provider}
Body: { device_id, access_token | id_token }
        ↓
Backend: verify token у провайдера
        ↓
Создать/найти user_id
        ↓
Привязать device_id → user_id
Перенести pro_until на user_id (если ещё не на user)
Перенести diary_entries с device на user
        ↓
Response: { access_token (JWT), user_id, is_pro, linked: true }
```

### 3.4. Новый телефон (восстановление)

```
Установка → новый device_id
        ↓
E3 или настройки → «Уже есть Pro? Войти»
        ↓
VK / Yandex → POST /auth/{provider} (без merge device, только token)
        ↓
Backend: user_id найден, is_pro активен
        ↓
JWT + дневник + безлимит на новом device_id
```

### 3.5. «Позже»

- Pro продолжает работать на **этом** `device_id`.
- В E3 — компактный баннер: «Привяжите VK, чтобы сохранить Pro».
- При `subscription/status`: `is_pro: true, account_linked: false`.

---

## 4. API (дополнение к FR)

| Метод | Назначение |
|-------|------------|
| `POST /api/v1/auth/vk` | VK token + `device_id` → JWT, merge |
| `POST /api/v1/auth/yandex` | Yandex token + `device_id` → JWT, merge |
| `GET /api/v1/auth/me` | `Authorization: Bearer` → профиль, `is_pro`, `linked_providers` |

**Обновление существующих эндпоинтов:**

- `GET /subscription/status` → добавить `account_linked: boolean`
- После login клиент шлёт `Authorization: Bearer` **или** `device_id` (guest); при наличии JWT сервер резолвит `user_id` и все его devices.

### Пример `GET /subscription/status`

```json
{
  "is_pro": true,
  "pro_until": "2026-07-14T12:00:00+03:00",
  "account_linked": false,
  "linked_providers": []
}
```

### Пример `POST /api/v1/auth/vk`

Request:
```json
{
  "device_id": "uuid-device",
  "access_token": "vk_oauth_token"
}
```

Response:
```json
{
  "access_token": "jwt…",
  "user_id": "uuid-user",
  "is_pro": true,
  "account_linked": true,
  "linked_providers": ["vk"]
}
```

---

## 5. БД (дополнение)

| Таблица | Поля |
|---------|------|
| `users` | `id`, `created_at` |
| `oauth_identities` | `user_id`, `provider` (vk/yandex/…), `provider_user_id`, unique(provider, provider_user_id) |
| `devices` | + `user_id` nullable FK; `pro_until` дублировать на user при merge |
| `diary_entries` | + `user_id` nullable; при merge: UPDATE SET user_id WHERE device_id = … |

**Правило Pro:** источник правды — `users.pro_until` если `user_id` есть, иначе `devices.pro_until`.

---

## 6. Клиент (KMP + Decompose)

```
RootComponent
├── … E1–E4
├── LinkAccount (E5)     ← push после is_pro && !account_linked
└── Settings (опционально v0.1) → «Привязать аккаунт», «Войти» для restore
```

- `AuthRepository`: хранит JWT в EncryptedSharedPreferences / Keychain.
- Guest-режим: только `device_id` в заголовке `X-Device-Id`.
- После login: `Authorization: Bearer` + `X-Device-Id` для merge и текущего устройства.

**AppMetrica:** `pro_purchased`, `link_account_shown`, `link_account_success`, `link_account_skipped`, `pro_restored`.

---

## 7. Критерии приёмки (auth)

- [ ] Free-пользователь не видит экран входа
- [ ] После тестовой оплаты Pro — безлимит без входа
- [ ] E5 показывается один раз после первой активации Pro
- [ ] «Позже» закрывает E5, Pro работает
- [ ] VK link merge: дневник и Pro на `user_id`
- [ ] Новый телефон: VK login → Pro и дневник восстановлены
- [ ] Два `device_id` с одним VK не дают двойной Pro

---

## 8. Out of scope (этот документ)

- Google, Telegram — v0.2
- Обязательный вход для free
- Email / пароль
- Apple Sign In — при выходе на iOS
