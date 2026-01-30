# 🛠️ Tech Context — Frontend

## Stack

| Компонент | Технология |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 + Glassmorphism |
| Network | Retrofit + OkHttp |
| DI | Hilt |
| Streaming | SSE (Server-Sent Events) |
| Local DB | Room |
| State | StateFlow + ViewModel |
| Images | Coil (with cache) |

## Backend API

- **Base URL:** `http://46.17.99.76:8000`
- **Auth:** JWT Bearer Token (auto-refresh via TokenAuthenticator)

## API Endpoints

### Auth
| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/auth/login | Логин |
| POST | /api/v1/auth/register | Регистрация |
| POST | /api/v1/auth/refresh | Обновление токена |

### Chat
| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/chat/send/stream | SSE стриминг |
| GET | /api/v1/conversations/ | Список диалогов |
| GET | /api/v1/conversations/{id}/messages | Сообщения |
| DELETE | /api/v1/chat/{id}/messages/{msgId} | Удаление сообщения |

### Documents
| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/documents/upload | Загрузка |
| GET | /api/v1/documents/ | Список |
| DELETE | /api/v1/documents/{id} | Удаление |
| PATCH | /api/v1/documents/{id}/rename | Переименование |

### Wellness
| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/wellness/mood | Записать настроение |
| GET | /api/v1/wellness/mood/history | История |
| POST | /api/v1/wellness/habits | Создать привычку |
| POST | /api/v1/wellness/habits/{id}/complete | Выполнить |

### Life Context
| Метод | Path | Описание |
|-------|------|----------|
| GET | /api/v1/life-context/questionnaire | Анкета |
| POST | /api/v1/life-context/questionnaire | Сохранить |
| GET | /api/v1/life-context/important-dates | Даты |
| GET | /api/v1/life-context/important-people | Люди |

### Medical
| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/medical/symptoms | Проверка симптомов |
| POST | /api/v1/medical/drug-interactions | Взаимодействия |
| POST | /api/v1/medical/lab-results | Анализы |
| GET | /api/v1/medical/recommendations | Рекомендации |
| GET | /api/v1/medical/emergency-info | Экстренная помощь |

## SSE Events Format

```json
{"type": "status", "status": "thinking"}
{"type": "token", "content": "часть текста"}
{"type": "image", "url": "https://..."}
{"type": "done", "message_id": "uuid", "conversation_id": "uuid"}
{"type": "error", "message": "описание ошибки"}
```

## Ключевые классы

| Класс | Описание |
|-------|----------|
| `ChatRepository` | SSE streaming + Room sync |
| `TokenAuthenticator` | Auto JWT refresh |
| `ChatViewModel` | UI state management |
| `GlassTheme` | Цвета и стили Glassmorphism |
| `GlassCard` | Базовый компонент карточки |
| `ImagePreloader` | Предзагрузка изображений |

## Структура проекта

```
app/src/main/kotlin/com/health/companion/
├── data/
│   ├── local/          # Room DB, DAOs
│   ├── remote/         # APIs, DTOs
│   └── repositories/   # Data layer
├── di/                 # Hilt modules
├── presentation/
│   ├── components/     # GlassCard, GlassDesignSystem
│   ├── navigation/     # NavGraph
│   ├── screens/        # UI screens
│   └── theme/          # Colors, Typography
├── services/           # Notifications, WebSocket
└── utils/              # TokenManager, CrashLogger
```
