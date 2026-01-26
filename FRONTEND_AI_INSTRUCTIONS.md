# 🤖 Инструкции для Frontend AI (Cursor на Windows)

## Как получить контекст от Backend AI

### 1. Быстрый старт — получить всё сразу

```bash
# В терминале Windows (PowerShell):
Invoke-RestMethod -Uri "http://<VPS_IP>:8000/api/v1/shared/context" | ConvertTo-Json -Depth 10
```

### 2. Получить только API endpoints

```bash
curl "http://<VPS_IP>:8000/api/v1/shared/context?category=api"
```

### 3. Получить модели данных (Kotlin)

```bash
curl "http://<VPS_IP>:8000/api/v1/shared/context?category=models"
```

### 4. Получить краткую сводку

```bash
curl "http://<VPS_IP>:8000/api/v1/shared/context/summary"
```

---

## Как добавить контекст для Backend AI

Когда ты (Frontend AI) делаешь что-то важное — сообщи об этом:

```bash
curl -X POST "http://<VPS_IP>:8000/api/v1/shared/context" \
  -H "Content-Type: application/json" \
  -d '{
    "category": "features",
    "title": "SSE Client Implementation",
    "content": "Реализовал SSE клиент на Kotlin с OkHttp. Поддерживает reconnect и timeout.",
    "source": "frontend",
    "priority": 8
  }'
```

### Категории:
- `api` — информация об endpoints
- `models` — модели данных
- `tasks` — текущие задачи
- `decisions` — принятые решения
- `bugs` — найденные баги
- `features` — реализованные фичи

---

## Как запросить информацию от Backend AI

```bash
curl -X POST "http://<VPS_IP>:8000/api/v1/shared/context/sync-request" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Как правильно обрабатывать ошибки 401 от API?",
    "from_source": "frontend"
  }'
```

Backend AI увидит этот запрос и добавит ответ.

---

## Cursor Rules для Frontend

Создай файл `.cursorrules` в корне frontend проекта:

```
# Pand-AI-Helper Frontend

## Проект
Android приложение на Kotlin + Jetpack Compose

## Backend API
- Base URL: http://<VPS_IP>:8000/api/v1
- Auth: JWT Bearer token
- Streaming: SSE (Server-Sent Events)

## Синхронизация контекста
Перед началом работы выполни:
curl http://<VPS_IP>:8000/api/v1/shared/context/summary

После важных изменений добавь контекст:
curl -X POST http://<VPS_IP>:8000/api/v1/shared/context -d '...'

## Ключевые решения
- Чат использует SSE streaming (НЕ REST)
- Сохранять conversation_id между сообщениями
- Backend понимает контекст (последние 8 сообщений)
```

---

## Пример Kotlin кода для SSE

```kotlin
// ChatRepository.kt
class ChatRepository(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    fun sendMessage(
        token: String,
        message: String,
        conversationId: String?
    ): Flow<ChatEvent> = callbackFlow {
        val body = JSONObject().apply {
            put("message", message)
            conversationId?.let { put("conversation_id", it) }
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/chat/send/stream")
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val call = okHttpClient.newCall(request)
        
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val json = JSONObject(line.removePrefix("data: "))
                            when (json.getString("type")) {
                                "status" -> trySend(ChatEvent.Status(json.getString("status")))
                                "token" -> trySend(ChatEvent.Token(json.getString("content")))
                                "done" -> {
                                    trySend(ChatEvent.Done(
                                        messageId = json.getString("message_id"),
                                        agent = json.getString("agent")
                                    ))
                                    close()
                                }
                                "error" -> {
                                    trySend(ChatEvent.Error(json.getString("message")))
                                    close()
                                }
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                trySend(ChatEvent.Error(e.message ?: "Unknown error"))
                close()
            }
        })

        awaitClose { call.cancel() }
    }
}

sealed class ChatEvent {
    data class Status(val status: String) : ChatEvent()
    data class Token(val content: String) : ChatEvent()
    data class Done(val messageId: String, val agent: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
}
```

---

## Важно!

1. **conversation_id** — сохраняй его после первого сообщения и передавай в следующих
2. **Streaming** — используй SSE, не REST
3. **Контекст** — Backend помнит последние 8 сообщений в рамках conversation
4. **Синхронизация** — регулярно проверяй `/shared/context` для актуальной информации

---

*Этот файл создан Backend AI для Frontend AI*
*Обновлено: 2026-01-25*
