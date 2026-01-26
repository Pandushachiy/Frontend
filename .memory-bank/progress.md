# 📊 Progress Log — Pand-AI-Helper Frontend

---

## [2026-01-26] Memory Bank Setup

### Что сделано:
- Создан Memory Bank
- Настроена синхронизация с Backend AI

### Файлы:
- `.cursor/rules/memory.mdc`
- `.cursor/rules/frontend.mdc`
- `.cursor/rules/sync.mdc`
- `.memory-bank/*`

---

## [2026-01-26] Documents Multi-Select

### Что сделано:
- Изменён `pickImageLauncher` с `GetContent()` на `GetMultipleContents()`
- Добавлен метод `uploadDocuments(uris: List<Uri>)` в DocumentsViewModel
- Теперь можно выбирать несколько фото из галереи

### Файлы:
- `DocumentsScreen.kt`
- `DocumentsViewModel.kt`

---

## [2026-01-26] Feyberry Design

### Что сделано:
- Полный редизайн LoginScreen и RegisterScreen
- Анимированная ягода (metaball lava lamp style)
- Glassmorphism стиль
- Русская локализация
- Portrait-only ориентация

### Файлы:
- `LoginScreen.kt`
- `RegisterScreen.kt`
- `AndroidManifest.xml`

---

## [2026-01-26] Chat Session Fixes

### Что сделано:
- Исправлены дубликаты сессий
- Убраны "дёргания" при переключении сессий
- Исправлено исчезновение сообщений при стриминге
- TokenAuthenticator для SSE запросов

### Файлы:
- `ChatViewModel.kt`
- `ChatScreen.kt`
- `ChatRepository.kt`
