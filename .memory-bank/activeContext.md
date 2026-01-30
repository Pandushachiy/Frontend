# 🎯 Active Context — Pand-AI-Helper Frontend

> **Последнее обновление:** 2026-01-30

## Backend API
- **URL:** http://46.17.99.76:8000
- **Синхрон:** GET /api/v1/shared/context?source=backend
- **Статус:** ✅ Работает

## ✅ Завершённые задачи
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
- [x] Life Context модуль (Профиль, Анкета, Даты, Люди)
- [x] Medical Assistant модуль (Симптомы, Лекарства, Анализы)
- [x] Аудит и очистка неиспользуемых файлов

## 🔄 В работе
- [ ] Session Attachments (контекст сессии)
- [ ] Dashboard виджеты интеграция

## 📁 Структура экранов

### Навигация (Bottom Bar)
| Tab | Экран | Route |
|-----|-------|-------|
| 🏠 | DashboardScreen | `dashboard` |
| 💬 | ChatScreen | `chat` |
| 📄 | DocumentsScreen | `documents` |
| 🧘 | WellnessScreen | `wellness` |
| ⚙️ | SettingsScreen | `settings` |

### Profile Module (из Settings)
- `ProfileScreen` — Главный экран профиля с аватаром
- `QuestionnaireScreen` — Пошаговая анкета (6 секций)
- `ImportantDatesScreen` — Управление важными датами
- `ImportantPeopleScreen` — Близкие люди

### Medical Module (из Settings)
- `MedicalAssistantScreen` — Главное меню
- `SymptomCheckerScreen` — Проверка симптомов
- `DrugInteractionsScreen` — Взаимодействия лекарств
- `LabResultsScreen` — Анализ результатов
- `RecommendationsScreen` — Рекомендации
- `EmergencyScreen` — Экстренная помощь

## 🗑️ Удалённые файлы (30.01.2026)
- `MoodScreen.kt` — заменён на WellnessScreen
- `MoodViewModel.kt` — заменён на WellnessViewModel
- `HealthScreen.kt` — не использовался
- `HealthViewModel.kt` — не использовался
- `GlassMorphismBox.kt` — не использовался
- `GlassMorphismCard.kt` — заменён на GlassCard

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

## Последние изменения Backend
- ProfileResponse: `userId` (не user_id) ✅
- **NEW:** Wellness API — `/api/v1/wellness/`
- **NEW:** Life Context API — `/api/v1/life-context/`
- **NEW:** Medical API — `/api/v1/medical/`
- SSE Events: `status`, `token`, `image`, `done`, `error`

## Важные файлы
- `app/src/main/kotlin/com/health/companion/data/remote/api/` — API клиенты
- `app/src/main/kotlin/com/health/companion/presentation/screens/` — UI
- `app/src/main/kotlin/com/health/companion/data/repositories/` — Репозитории
- `app/src/main/kotlin/com/health/companion/presentation/components/` — GlassDesignSystem
