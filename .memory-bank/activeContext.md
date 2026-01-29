# 🎯 Active Context — Pand-AI-Helper Frontend

> **Последнее обновление:** 2026-01-29

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
- [ ] Image-to-Image редактирование (прикрепление фото)
- [ ] Session Attachments (контекст сессии)

## Последние изменения Backend (проверять синхрон!)
- ProfileResponse: `userId` (не user_id) ✅
- MemoryResponse: key, value, type, createdAt
- **NEW:** Image-to-Image API — `images: [base64]` в chat request
- **NEW:** Session Attachments API — `/api/v1/attachments/{conversationId}/`
- SSE Events: `status`, `token`, `image`, `done`, `error`

## Актуальные DTO

### ProfileResponse
```kotlin
data class ProfileResponse(
    val userId: String,           // ✅ camelCase!
    val name: String,
    val email: String,
    val memories: List<MemoryResponse>,
    val documentsCount: Int,
    val entitiesCount: Int,
    val relationsCount: Int
)
```

### MemoryResponse
```kotlin
data class MemoryResponse(
    val key: String,        // "🎯 Цель: ..." или "pet_cat"
    val value: String,      // Описание (до 500 символов)
    val type: String,       // IMPORTANT, CUSTOM, PREFERENCE
    val createdAt: String   // ISO datetime
)
```

## Важные файлы
- `app/src/main/kotlin/com/health/companion/data/remote/api/` — API клиенты
- `app/src/main/kotlin/com/health/companion/presentation/screens/` — Compose UI
- `app/src/main/kotlin/com/health/companion/data/repositories/` — Репозитории
