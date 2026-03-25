package com.health.companion.services

/**
 * Parses the structured reminder message sent by the backend via WebSocket/FCM.
 *
 * Backend format:
 * ```
 * 🟡 **Купить молоко**
 * ⏰ Сегодня в 15:00
 * 📅 Следующее: завтра в 09:00   ← только для повторяющихся
 * 📝 Не забудь чек               ← только если есть description
 * 📁 Личное · Одноразовое
 * 💬 Напишите «готово»...        ← игнорируем в уведомлениях, только для чата
 * ```
 */
data class ParsedReminder(
    val reminderId: String,
    val title: String,
    val timeLabel: String?,
    val nextLabel: String?,
    val description: String?,
    val categoryLabel: String?,
    val priority: String,
    val category: String,
)

object ReminderMessageParser {

    private val STRIP_MARKDOWN = Regex("\\*{1,2}(.*?)\\*{1,2}")
    private val EMOJI_PREFIX = Regex("^[🔴🟠🟡🟢🔔]\\s*")

    /**
     * @param rawText  Multi-line text from backend (WebSocket content or FCM body)
     * @param metadata Map with "reminder_id", "priority", "category" keys
     */
    fun parse(rawText: String, metadata: Map<String, String>): ParsedReminder {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Line 1: "🟡 **Купить молоко**"
        val titleRaw = lines.getOrNull(0) ?: ""
        val title = STRIP_MARKDOWN.replace(titleRaw, "$1")
            .replace(EMOJI_PREFIX, "")
            .trim()
            .ifBlank { "Напоминание" }

        var timeLabel: String? = null
        var nextLabel: String? = null
        var description: String? = null
        var categoryLabel: String? = null

        for (line in lines.drop(1)) {
            when {
                line.startsWith("⏰") -> timeLabel = line.removePrefix("⏰").trim()
                line.startsWith("📅") -> nextLabel = line.removePrefix("📅").trim()
                line.startsWith("📝") -> {
                    val text = line.removePrefix("📝").trim()
                    // Skip backend chat-instruction lines that leak into description
                    val isInstruction = text.contains("Напишите", ignoreCase = true)
                        || text.contains("готово", ignoreCase = true)
                        || text.contains("отложить", ignoreCase = true)
                    if (!isInstruction) description = text
                }
                line.startsWith("📁") -> categoryLabel = line.removePrefix("📁").trim()
                line.startsWith("💬") -> { /* chat-only hint — skip in system notifications */ }
            }
        }

        return ParsedReminder(
            reminderId = metadata["reminder_id"] ?: "",
            title = title,
            timeLabel = timeLabel,
            nextLabel = nextLabel,
            description = description,
            categoryLabel = categoryLabel,
            priority = metadata["priority"] ?: "medium",
            category = metadata["category"] ?: "general",
        )
    }
}
