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

## Backend API

- **Base URL:** `http://46.17.99.76:8000`
- **Auth:** JWT Bearer Token (auto-refresh via TokenAuthenticator)

## Endpoints

| Метод | Path | Описание |
|-------|------|----------|
| POST | /api/v1/auth/login | Логин |
| POST | /api/v1/auth/register | Регистрация |
| POST | /api/v1/auth/refresh | Обновление токена |
| POST | /api/v1/chat/send/stream | Чат SSE стриминг |
| GET | /api/v1/conversations/ | Список диалогов |
| GET | /api/v1/conversations/{id}/messages | Сообщения диалога |
| POST | /api/v1/documents/upload | Загрузка документов |
| GET | /api/v1/documents/ | Список документов |
| DELETE | /api/v1/documents/{id} | Удаление документа |

## SSE Events Format

```json
{"type": "status", "status": "thinking"}
{"type": "token", "content": "часть текста"}
{"type": "done", "message_id": "uuid", "conversation_id": "uuid", "new_conversation_id": "uuid"}
{"type": "error", "message": "описание ошибки"}
```

## Ключевые классы

- `ChatRepository` — SSE streaming + Room sync
- `TokenAuthenticator` — auto JWT refresh
- `ChatViewModel` — UI state management
- `GlassTheme` — цвета и стили Feyberry
