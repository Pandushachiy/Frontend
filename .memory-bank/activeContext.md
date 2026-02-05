# 🎯 Active Context — Pand-AI-Helper Frontend

> **Последнее обновление:** 2026-02-04

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
- [x] Редизайн сессий чата (анимации, попап)

## 🔄 В работе
- [ ] Session Attachments (контекст сессии)
- [ ] Dashboard виджеты интеграция

## ⚠️ ИЗВЕСТНЫЕ БАГИ (ждём бэк)
1. **image_url теряется** — бэк не возвращает `image_url` в GET /messages
2. **Время сообщений** — `created_at` не всегда приходит с бэка

---

## 📋 ПОЛНАЯ ДОКУМЕНТАЦИЯ API

### 1️⃣ DASHBOARD API (`/api/v1/dashboard/`)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/dashboard` | Главный дашборд |
| GET | `/dashboard/mood-chart?days=7` | График настроения |
| GET | `/dashboard/streak` | Серия активности |
| GET | `/dashboard/emotional-state` | Эмоциональное состояние |
| GET | `/dashboard/memory-summary` | Сводка памяти |

**DashboardResponse:**
```json
{
  "greeting": "Привет!",
  "insight": "Текст инсайта",
  "messagesThisWeek": 15,
  "streak": {
    "days": 5,
    "emoji": "🔥",
    "message": "5 дней подряд!"
  },
  "factAboutMe": {
    "emoji": "💡",
    "text": "Ты любишь кофе"
  },
  "quickActions": [
    {"id": "1", "emoji": "💬", "title": "Поболтать", "action": "chat"}
  ],
  "lastUpdated": "2026-02-04T12:00:00Z"
}
```

**MoodChartResponse:**
```json
{
  "type": "mood_chart",
  "period_days": 7,
  "data": {
    "chart_data": [
      {"date": "2026-02-01", "mood_level": 7.5, "stress_level": 3.0, "energy_level": 6.0}
    ],
    "entries_count": 7,
    "average_mood": 7.2,
    "best_day": {"date": "2026-02-03", "mood_level": 9.0},
    "worst_day": {"date": "2026-02-02", "mood_level": 5.0}
  }
}
```

**EmotionalStateResponse:**
```json
{
  "type": "emotional_state",
  "valence": 0.7,
  "arousal": 0.5,
  "primary_emotion": "joy",
  "secondary_emotions": ["calm", "optimism"],
  "mood_label": "Позитивное",
  "needs_support": false,
  "confidence": 0.85,
  "detected_triggers": ["хорошая погода"]
}
```

---

### 2️⃣ WELLNESS API (`/api/v1/wellness/`)

#### Mood (Настроение)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/wellness/mood` | Записать настроение |
| GET | `/wellness/mood?days=30` | История настроения |
| GET | `/wellness/mood/today` | Настроение сегодня |
| GET | `/wellness/mood/stats?days=30` | Статистика |

**MoodRequest (POST):**
```json
{
  "mood_level": 7,
  "energy_level": 6,
  "stress_level": 3,
  "anxiety_level": 2,
  "activities": ["работа", "прогулка"],
  "triggers": ["хорошая погода"],
  "journal_text": "Отличный день!"
}
```

#### Habits (Привычки)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/wellness/habits` | Создать привычку |
| GET | `/wellness/habits` | Список привычек |
| GET | `/wellness/habits/{id}` | Одна привычка |
| PUT | `/wellness/habits/{id}` | Обновить привычку |
| DELETE | `/wellness/habits/{id}` | Удалить привычку |
| POST | `/wellness/habits/{id}/complete` | Отметить выполнение |
| POST | `/wellness/habits/{id}/uncomplete` | Отменить выполнение |
| GET | `/wellness/habits/stats` | Статистика привычек |

**CreateHabitRequest:**
```json
{
  "name": "Медитация",
  "description": "10 минут утром",
  "emoji": "🧘",
  "frequency": "daily",
  "frequency_times": 1,
  "reminder_enabled": true,
  "reminder_time": "08:00",
  "reminder_days": [1,2,3,4,5],
  "color": "#6366F1"
}
```

**Habit Response:**
```json
{
  "id": "uuid",
  "name": "Медитация",
  "emoji": "🧘",
  "frequency": "daily",
  "current_streak": 5,
  "best_streak": 10,
  "total_completions": 25,
  "completed_today": false,
  "is_active": true
}
```

#### Digest (Ежедневная сводка)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/wellness/digest/preferences` | Настройки дайджеста |
| PUT | `/wellness/digest/preferences` | Обновить настройки |
| GET | `/wellness/digest/preview` | Превью дайджеста |

---

### 3️⃣ PROFILE API (`/api/v1/profile/`)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/profile` | Профиль пользователя |
| GET | `/profile/knowledge-graph?entity_type=&limit=` | Граф знаний |
| GET | `/profile/routing-stats` | Статистика роутинга |
| DELETE | `/profile/facts/{id}` | Удалить факт |
| DELETE | `/profile/clear-all-facts` | Очистить все факты |

**ProfileResponse:**
```json
{
  "user": {
    "id": "uuid",
    "name": "Имя",
    "email": "email@mail.com",
    "avatarUrl": "https://..."
  },
  "facts": [
    {"id": "1", "emoji": "☕", "text": "Любит кофе", "category": "preferences", "canDelete": true}
  ],
  "documents": [
    {"id": "1", "name": "Анализы.pdf", "type": "medical", "summary": "...", "entitiesCount": 5, "uploadedAt": "2026-02-01"}
  ],
  "stats": {"facts": 15, "documents": 3, "conversations": 25}
}
```

**KnowledgeGraphResponse:**
```json
{
  "entities": [
    {"id": "1", "type": "person", "name": "Мама", "description": "Близкий человек", "confidence": 0.95}
  ],
  "relations": [
    {"id": "1", "sourceName": "User", "targetName": "Мама", "type": "family", "weight": 1.0}
  ],
  "totalEntities": 50,
  "totalRelations": 30
}
```

---

### 4️⃣ MEDICAL API (`/api/v1/medical/`)

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/medical/symptoms` | Проверка симптомов |
| POST | `/medical/drug-interactions` | Взаимодействие лекарств |
| POST | `/medical/lab-results` | Анализ результатов |
| POST | `/medical/search` | Медицинский поиск |
| GET | `/medical/recommendations?focus_area=` | Рекомендации |
| GET | `/medical/emergency-info` | Экстренная информация |

**SymptomCheckRequest:**
```json
{
  "symptoms": "Головная боль, тошнота",
  "duration": "2 дня"
}
```

**SymptomCheckResponse:**
```json
{
  "symptoms": ["головная боль", "тошнота"],
  "severity": "medium",
  "recommendations": ["Отдых", "Обильное питьё"],
  "when_to_see_doctor": "Если симптомы не проходят 3+ дней",
  "specialist_type": "терапевт",
  "possible_causes": ["мигрень", "ОРВИ"],
  "disclaimer": "Это не медицинский совет..."
}
```

**DrugInteractionRequest:**
```json
{
  "drugs": ["Ибупрофен", "Аспирин"],
  "include_current_medications": true
}
```

**LabResultsRequest:**
```json
{
  "results": {
    "Гемоглобин": 140.0,
    "Глюкоза": 5.5,
    "Холестерин": 6.2
  }
}
```

---

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

## Важные файлы
- `app/src/main/kotlin/com/health/companion/data/remote/api/` — API клиенты
- `app/src/main/kotlin/com/health/companion/presentation/screens/` — UI
- `app/src/main/kotlin/com/health/companion/data/repositories/` — Репозитории
- `app/src/main/kotlin/com/health/companion/presentation/components/` — GlassDesignSystem
