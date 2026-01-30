# 🎯 Active Context — Pand-AI-Helper Frontend

> **Последнее обновление:** 2026-01-30

## Backend API
- **URL:** http://46.17.99.76:8000
- **Синхрон:** GET /api/v1/shared/context?source=backend
- **Статус:** ✅ Работает

## Текущие задачи
- [x] Memory Bank Setup
- [x] Синхронизация с Backend AI
- [x] Мультивыбор фото в галерее (Documents)
- [x] Feyberry дизайн Login/Register
- [x] Исправление SSE streaming
- [x] Синхронизация сессий чата
- [x] Генерация изображений в чате
- [x] Анимация генерации изображений
- [x] Скачивание изображений в галерею
- [x] Image-to-Image анимация трансформации
- [x] Удаление сообщений из чата
- [x] Wellness модуль (Mood, Habits, Digest)
- [x] **NEW:** Life Context модуль (Профиль, Анкета, Даты, Люди)
- [x] **NEW:** Medical Assistant модуль (Симптомы, Лекарства, Анализы)
- [ ] Session Attachments (контекст сессии)

## 🆕 Новые модули (30.01.2026)

### Life Context (`/api/v1/life-context/`)
- `GET /questionnaire` — Получить анкету профиля
- `POST /questionnaire` — Сохранить ответы анкеты
- `GET /profile` — Получить профиль пользователя
- `GET /important-dates` — Важные даты
- `POST /important-dates` — Добавить дату
- `GET /important-people` — Близкие люди
- `POST /important-people` — Добавить человека
- `GET /patterns` — Паттерны жизни (аналитика)

### Medical Assistant (`/api/v1/medical/`)
- `POST /symptoms` — Проверка симптомов (severity: low/medium/high/urgent)
- `POST /drug-interactions` — Проверка взаимодействий лекарств
- `POST /lab-results` — Анализ результатов анализов
- `POST /search` — Поиск медицинской информации
- `GET /recommendations` — Рекомендации по здоровью
- `GET /emergency-info` — Экстренная информация (FAST тест, номера)

## ⚠️ ЗАДАЧИ ДЛЯ BACKEND

### DELETE сообщений из контекста
Frontend реализовал удаление сообщений из чата. Нужен endpoint:

```
DELETE /api/v1/chat/conversations/{conversationId}/messages/{messageId}
```

**Требования:**
1. НЕ удалять физически из БД
2. Пометить как `deleted=true`
3. НЕ включать deleted сообщения в контекст при отправке в LLM
4. Response: `{ "status": "ok" }`

Frontend вызывает этот endpoint при удалении сообщения юзером.

## Последние изменения Backend (проверять синхрон!)
- ProfileResponse: `userId` (не user_id) ✅
- MemoryResponse: key, value, type, createdAt
- **NEW:** Image-to-Image API — `images: [base64]` в chat request
- **NEW:** Session Attachments API — `/api/v1/attachments/{conversationId}/`
- **NEW:** Wellness API — `/api/v1/wellness/`
- **NEW:** Life Context API — `/api/v1/life-context/`
- **NEW:** Medical API — `/api/v1/medical/`
- SSE Events: `status`, `token`, `image`, `done`, `error`

## Новые экраны

### Profile Module
- `ProfileScreen` — Главный экран профиля с аватаром, статистикой
- `QuestionnaireScreen` — Пошаговая анкета (6 секций)
- `ImportantDatesScreen` — Управление важными датами
- `ImportantPeopleScreen` — Управление близкими людьми

### Medical Module
- `MedicalAssistantScreen` — Главное меню медпомощника
- `SymptomCheckerScreen` — Проверка симптомов с severity
- `DrugInteractionsScreen` — Проверка лекарств
- `LabResultsScreen` — Анализ результатов анализов
- `RecommendationsScreen` — Рекомендации по здоровью
- `EmergencyScreen` — Экстренная помощь (номера, FAST тест)

## Навигация
- Медицинский помощник доступен: Ещё → Медицинский помощник
- Профиль доступен: Ещё → Карточка профиля

## Важные файлы
- `app/src/main/kotlin/com/health/companion/data/remote/api/` — API клиенты
- `app/src/main/kotlin/com/health/companion/presentation/screens/` — Compose UI
- `app/src/main/kotlin/com/health/companion/data/repositories/` — Репозитории
- **NEW:** `app/src/main/kotlin/com/health/companion/presentation/screens/profile/` — Profile модуль
- **NEW:** `app/src/main/kotlin/com/health/companion/presentation/screens/medical/` — Medical модуль
