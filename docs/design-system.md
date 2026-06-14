# Дизайн-система KkalScan

Яркая мобильная система для Android (Jetpack Compose / Material 3).  
Фокус: **быстрый скан → крупные цифры ккал → лёгкий дневник**.

**Связанные документы:** [brand-dns.md](./brand-dns.md), [mvp-functional-requirements.md](./mvp-functional-requirements.md)

| Поле | Значение |
|------|----------|
| Версия | 1.1 |
| Дата | 2026-06-14 |
| Название темы | **Citrus Scan** |
| Платформа | Android, RuStore |

---

## 1. Референсы RuStore (что берём и чего избегаем)

| Приложение | RuStore | Что работает | Что не копируем |
|------------|---------|--------------|-----------------|
| **Wayout** | 4.9★, 60k+ DL | Чистые карточки приёмов пищи, понятный дневник, крупная CTA «сфоткать», без визуального шума | **Синяя** палитра, плотный Pro-paywall |
| **CalZen AI** | 4.8★, 40k+ DL | **Огромные цифры ккал**, чипы Б/Ж/У, превью фото блюда, ощущение «премиум-счётчика» | **Красно-оранжевый** бренд, агрессивные таймеры скидки |
| **AIplate** | 4.8★, 50k+ DL | Акцент на «нейросеть», мягкие градиенты | Фиолетово-VK тон, обязательный вход |
| **Calz** | слабый RuStore | Простота первого экрана | Бледная generic-палитра |

**Визуальный тезис KkalScan:**  
*«Энергия свежего фрукта + уверенность сканера»* — теплее CalZen, ярче Wayout, без клинического зелёного и без синего/красного лидеров.

---

## 2. Цвета

### 2.1. Основная палитра

| Токен | HEX | Роль |
|-------|-----|------|
| `primary` | `#FF7A2F` | Mango — главные кнопки, FAB, активные элементы |
| `primaryContainer` | `#FFE4D1` | Фон выделенных блоков, бейдж «3 скана» |
| `onPrimary` | `#FFFFFF` | Текст на primary |
| `secondary` | `#1EC995` | Mint — успех скана, прогресс дня, иконки БЖУ |
| `secondaryContainer` | `#D4FAEE` | Фон карточек «съедено сегодня» |
| `tertiary` | `#FFE566` | Lemon — бесплатные сканы, акценты free |
| `tertiaryContainer` | `#FFF8D6` | Подсветка лимита / бонуса за рекламу |

### 2.2. Нейтрали и поверхности

| Токен | HEX | Роль |
|-------|-----|------|
| `background` | `#FFFCF8` | Тёплый кремовый фон (не холодный #FFF) |
| `surface` | `#FFFFFF` | Карточки блюд |
| `surfaceVariant` | `#F5F0EA` | Вторичные панели, чипы |
| `outline` | `#E8DFD4` | Разделители |
| `onBackground` | `#1C1428` | Основной текст (Ink, не чистый чёрный) |
| `onSurfaceVariant` | `#6B5E72` | Подписи, вторичный текст |

### 2.3. Семантика

| Токен | HEX | Использование |
|-------|-----|---------------|
| `success` | `#1EC995` | Скан OK, добавлено в дневник |
| `warning` | `#FFB020` | «Остался 1 скан» |
| `error` | `#FF4757` | Ошибка сети, лимит |
| `info` | `#4DA3FF` | Подсказки (единственный синий — мелкие hint) |
| `pro` | `#9B5CFF` | Pro 199 ₽ — отдельно от primary, «премиум» |
| `proContainer` | `#F0E6FF` | Фон paywall Pro |

### 2.4. Макросы БЖУ (как у CalZen — цветные чипы)

| Макро | HEX | Фон чипа |
|-------|-----|----------|
| Белки | `#5B8DEF` | `#E8F0FF` |
| Жиры | `#FF9F43` | `#FFF0E0` |
| Углеводы | `#1EC995` | `#D4FAEE` |
| Ккал (главная) | `#FF7A2F` | — (цифра, не чип) |

### 2.5. Градиенты (только 2 места)

```text
scanHero:   #FF7A2F → #FF4D8D   (угол 135°) — экран камеры (E1)
proBanner:  #9B5CFF → #FF7A2F   (угол 90°)  — карточка Pro на E4
```

Не заливать градиентом весь UI — только hero и paywall.

### 2.6. Тёмная тема (v0.2, заложить токены)

| Токен | HEX |
|-------|-----|
| `background` | `#12101A` |
| `surface` | `#1E1A28` |
| `primary` | `#FF9A5C` |
| `secondary` | `#3DDBA8` |
| `onBackground` | `#F5F0FF` |

MVP v0.1: **только светлая тема**.

---

## 3. Типографика

### 3.1. Шрифты (Google Fonts, кириллица)

| Роль | Шрифт | Начертания | Зачем |
|------|-------|------------|-------|
| **Display** | [Unbounded](https://fonts.google.com/specimen/Unbounded) | 600, 700 | Заголовки, **цифры ккал**, логотип — ярко, современно, не как у CalZen (SF/гротеск) |
| **Body** | [Manrope](https://fonts.google.com/specimen/Manrope) | 400, 500, 600, 700 | UI, кнопки, подписи — отличная кириллица, читаемо в RuStore-аудитории |
| **Mono / tabular** | Manrope `fontFeatureSettings: "tnum"` | 600 | Счётчики сканов, ккал в дневнике — цифры не «прыгают» |

**Не использовать:** Inter-only (слишком generic), Roboto default (как у половины Android), Playfair (не про ЗОЖ).

### 3.2. Шкала (sp, Material 3)

| Стиль | Шрифт | Size | Line | Weight | Пример |
|-------|-------|------|------|--------|--------|
| `displayLarge` | Unbounded | 48 | 52 | 700 | **1840** ккал за день |
| `displayMedium` | Unbounded | 36 | 40 | 700 | **420** ккал на блюдо |
| `headlineLarge` | Unbounded | 28 | 32 | 600 | «Сегодня» |
| `headlineMedium` | Manrope | 22 | 28 | 600 | «Завтрак» |
| `titleLarge` | Manrope | 18 | 24 | 600 | Название блюда |
| `titleMedium` | Manrope | 16 | 22 | 600 | Кнопки |
| `bodyLarge` | Manrope | 16 | 24 | 400 | Описания |
| `bodyMedium` | Manrope | 14 | 20 | 400 | Вторичный текст |
| `labelLarge` | Manrope | 14 | 20 | 600 | Чипы БЖУ |
| `labelSmall` | Manrope | 11 | 16 | 500 | «Осталось 2 скана» |

### 3.3. Правила цифр (заимствуем у CalZen)

- **Ккал — всегда крупнее** названия блюда минимум на 1 уровень типографики.
- Единица «ккал» — `labelMedium`, цвет `onSurfaceVariant`, рядом с числом.
- БЖУ — только в **чипах**, не строкой текста.

---

## 4. Отступы, скругления, тени

### 4.1. Spacing (dp)

| Токен | dp |
|-------|-----|
| `xs` | 4 |
| `sm` | 8 |
| `md` | 16 |
| `lg` | 24 |
| `xl` | 32 |
| `xxl` | 48 |

Экранные поля: горизонтально **20 dp** (чуть шире стандартных 16 — «воздух» как Wayout).

### 4.2. Radius

| Токен | dp | Где |
|-------|-----|-----|
| `radiusSm` | 12 | Чипы БЖУ |
| `radiusMd` | 16 | Карточки блюд |
| `radiusLg` | 24 | Bottom sheet, paywall |
| `radiusXl` | 32 | FAB-зона, превью фото |
| `radiusFull` | 999 | Pill-кнопки, бейджи |

### 4.3. Elevation / тени

Светлая тема — **мягкие цветные тени**, не серый Material default:

```text
cardShadow:  0 8 24 rgba(255, 122, 47, 0.12)
fabShadow:   0 12 32 rgba(255, 122, 47, 0.28)
```

---

## 5. Иконография

| Принцип | Решение |
|---------|---------|
| Стиль | Rounded outline, stroke **2 dp**, скруглённые концы |
| Набор | Material Symbols Rounded |
| FAB | `photo_camera` в круге `primary`, размер 28 dp |
| Скан-рамка | Viewfinder с углами `secondary` (как «сканер», не просто камера) |
| Tab / nav | Максимум 2 иконки в MVP: «Дневник», «Скан» |

**Иконка приложения 512×512:**

- Фон: градиент `scanHero` или solid `#FF7A2F`
- Символ: белая рамка viewfinder + цифра «ккал» шрифтом Unbounded
- Без фотореалистичной еды (отличает от CalZen с тарелками)

---

## 6. Компоненты (5 экранов, без онбординга)

После установки — сразу **E3 «Сегодня»** (см. FR-40). Отдельного welcome-экрана нет.

### 6.1. Кнопки

| Тип | Стиль |
|-----|-------|
| **Primary CTA** | Fill `primary`, текст белый, `radiusFull`, высота **56 dp**, Unbounded не нужен — Manrope 600 |
| **Secondary** | Outline `primary`, фон прозрачный |
| **Scan FAB** | 64×64, `primary`, иконка камеры, `fabShadow`, фиксированно над bottom bar |
| **Pro** | Fill `pro` или outline `pro` на E4 |
| **Rewarded ad** | Fill `tertiary`, текст `onBackground` — визуально «бесплатно» |

### 6.2. Карточка блюда (дневник)

```
┌─────────────────────────────────────┐
│ [thumb 56]  Борщ с говядиной        │  titleLarge
│             320 ккал                │  displayMedium, primary
│             [Б 12] [Ж 8] [У 52]     │  macro chips
└─────────────────────────────────────┘
 surface, radiusMd, cardShadow, padding md
```

Заимствование: структура Wayout/CalZen — фото слева (если есть), цифра доминирует.

### 6.3. Бейдж «Сканов осталось»

- Pill `tertiaryContainer`, текст `onBackground`
- Иконка `bolt` или `camera` в `tertiary`
- Примеры: «3 скана бесплатно», «+2 за рекламу»

### 6.4. Экран лимита (E4)

- Верх: иллюстрация viewfinder (не грустный lock — **позитивный** «лимит на сегодня»)
- Заголовок Unbounded: «На сегодня хватит»
- Подзаголовок: «Завтра снова 3 бесплатных скана»
- Две равные карточки-кнопки: реклама (жёлтая) / Pro (фиолетовая)
- **Не** красный таймер скидки (антипаттерн CalZen)

### 6.5. Состояния загрузки скана

- Skeleton в цвете `primaryContainer` (пульс)
- Или Lottie: «сканируем тарелку» — 3 точки `secondary`
- Успех: короткий haptic + flash `secondaryContainer` на карточке результата

---

## 7. Motion

| Действие | Длительность | Кривая |
|----------|--------------|--------|
| Переход экранов | 280 ms | `FastOutSlowIn` |
| Появление карточки блюда | 200 ms | stagger 50 ms |
| FAB press | scale 0.94 → 1 | spring |
| Счётчик ккал | count-up 400 ms | при добавлении в дневник |
| Ошибка сети | shake 8 dp | 300 ms |

Без бесконечных анимаций на главном экране.

---

## 8. Compose / Material 3 — сниппет токенов

```kotlin
// theme/Color.kt
val Mango = Color(0xFFFF7A2F)
val MangoLight = Color(0xFFFFE4D1)
val Mint = Color(0xFF1EC995)
val MintLight = Color(0xFFD4FAEE)
val Lemon = Color(0xFFFFE566)
val Ink = Color(0xFF1C1428)
val Cream = Color(0xFFFFFCF8)
val ProPurple = Color(0xFF9B5CFF)

val KkalScanLightColors = lightColorScheme(
    primary = Mango,
    onPrimary = Color.White,
    primaryContainer = MangoLight,
    secondary = Mint,
    secondaryContainer = MintLight,
    tertiary = Lemon,
    background = Cream,
    surface = Color.White,
    onBackground = Ink,
    outline = Color(0xFFE8DFD4),
)
```

```kotlin
// theme/Type.kt — подключить Unbounded + Manrope через Google Fonts (Compose)
val KkalScanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = UnboundedFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    // ...
)
```

---

## 9. Скриншоты для RuStore (визуальный сторителлинг)

| # | Экран | Акцент |
|---|-------|--------|
| 1 | Дневник «Сегодня» | Крупная сумма ккал, 2–3 карточки блюд, FAB mango |
| 2 | Результат скана | Фото тарелки + **420 ккал** + цветные чипы БЖУ |
| 3 | Экран лимита | «3 скана бесплатно» + жёлтая кнопка рекламы |

Фон скриншотов: `background` (#FFFCF8), без тёмных mockup-рамок.

---

## 10. Чеклист для разработки

- [ ] Подключить Unbounded + Manrope (Google Fonts Downloadable Fonts API)
- [ ] `KkalScanTheme` с `lightColorScheme` из §8
- [ ] Компоненты: `MealCard`, `MacroChip`, `ScanFab`, `ScansLeftBadge`, `LimitPaywall`
- [ ] Иконка 512×512 по §5
- [ ] Проверка контраста: текст Ink на Cream ≥ 4.5:1 (WCAG AA)
- [ ] Скриншоты RuStore по §9

---

## 11. История

| Версия | Дата | Изменения |
|--------|------|-----------|
| 1.0 | 2026-06-14 | Первая версия Citrus Scan, референсы Wayout / CalZen |
