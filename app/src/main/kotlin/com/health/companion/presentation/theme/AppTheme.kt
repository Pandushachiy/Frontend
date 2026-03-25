package com.health.companion.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// ─────────────────────────────────────────────────────────────────
// ACCENT THEME
// surfaceTint — мешается в surfaceColor карточек на ~6%
// ─────────────────────────────────────────────────────────────────

enum class AppThemeOption(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val userBubble: Color,
    val userBubbleDark: Color,
    val surfaceTint: Color
) {
    TEAL(
        id = "teal", label = "Мята",
        primary        = Color(0xFF00D9A5),
        secondary      = Color(0xFF6366F1),
        userBubble     = Color(0xFF0D9E7A),
        userBubbleDark = Color(0xFF0A7A5E),
        surfaceTint    = Color(0xFF00D9A5)
    ),
    INDIGO(
        id = "indigo", label = "Индиго",
        primary        = Color(0xFF6366F1),
        secondary      = Color(0xFF8B5CF6),
        userBubble     = Color(0xFF4F46E5),
        userBubbleDark = Color(0xFF3730A3),
        surfaceTint    = Color(0xFF6366F1)
    ),
    PURPLE(
        id = "purple", label = "Пурпур",
        primary        = Color(0xFFA855F7),
        secondary      = Color(0xFFEC4899),
        userBubble     = Color(0xFF9333EA),
        userBubbleDark = Color(0xFF7E22CE),
        surfaceTint    = Color(0xFFA855F7)
    ),
    OCEAN(
        id = "ocean", label = "Океан",
        primary        = Color(0xFF06B6D4),
        secondary      = Color(0xFF3B82F6),
        userBubble     = Color(0xFF0891B2),
        userBubbleDark = Color(0xFF0E7490),
        surfaceTint    = Color(0xFF06B6D4)
    ),
    SUNSET(
        id = "sunset", label = "Закат",
        primary        = Color(0xFFF97316),
        secondary      = Color(0xFFEF4444),
        userBubble     = Color(0xFFEA580C),
        userBubbleDark = Color(0xFFC2410C),
        surfaceTint    = Color(0xFFF97316)
    ),
    ROSE(
        id = "rose", label = "Роза",
        primary        = Color(0xFFFF5C87),
        secondary      = Color(0xFFFF3370),
        userBubble     = Color(0xFFE04A72),
        userBubbleDark = Color(0xFFBF3A5E),
        surfaceTint    = Color(0xFFFF5C87)
    ),
    SAKURA(
        id = "sakura", label = "Сакура",
        primary        = Color(0xFFEC407A),
        secondary      = Color(0xFFAB47BC),
        userBubble     = Color(0xFFCE3667),
        userBubbleDark = Color(0xFFAB2B52),
        surfaceTint    = Color(0xFFEC407A)
    ),
    CORAL(
        id = "coral", label = "Коралл",
        primary        = Color(0xFFFF6B4A),
        secondary      = Color(0xFFFB8C00),
        userBubble     = Color(0xFFE5573A),
        userBubbleDark = Color(0xFFC44428),
        surfaceTint    = Color(0xFFFF6B4A)
    ),
    LIME(
        id = "lime", label = "Лайм",
        primary        = Color(0xFF6BCB77),
        secondary      = Color(0xFF26A69A),
        userBubble     = Color(0xFF4DA85A),
        userBubbleDark = Color(0xFF3A8045),
        surfaceTint    = Color(0xFF6BCB77)
    ),
    GOLD(
        id = "gold", label = "Золото",
        primary        = Color(0xFFFFBE0B),
        secondary      = Color(0xFFFF9500),
        userBubble     = Color(0xFFCC9400),
        userBubbleDark = Color(0xFFA07200),
        surfaceTint    = Color(0xFFFFBE0B)
    ),
    NEON(
        id = "neon", label = "Неон",
        primary        = Color(0xFF00FF94),
        secondary      = Color(0xFF0AFFF0),
        userBubble     = Color(0xFF00CC72),
        userBubbleDark = Color(0xFF009952),
        surfaceTint    = Color(0xFF00FF94)
    ),
    CRIMSON(
        id = "crimson", label = "Алый",
        primary        = Color(0xFFDC143C),
        secondary      = Color(0xFFFF4081),
        userBubble     = Color(0xFFB01030),
        userBubbleDark = Color(0xFF8B0D26),
        surfaceTint    = Color(0xFFDC143C)
    ),
    VIOLET(
        id = "violet", label = "Ультра",
        primary        = Color(0xFF7C4DFF),
        secondary      = Color(0xFF3D5AFE),
        userBubble     = Color(0xFF651FFF),
        userBubbleDark = Color(0xFF4527A0),
        surfaceTint    = Color(0xFF7C4DFF)
    ),
    AMBER(
        id = "amber", label = "Янтарь",
        primary        = Color(0xFFFFD600),
        secondary      = Color(0xFFFF6D00),
        userBubble     = Color(0xFFF9A825),
        userBubbleDark = Color(0xFFF57F17),
        surfaceTint    = Color(0xFFFFD600)
    ),
    MALACHITE(
        id = "malachite", label = "Малахит",
        primary        = Color(0xFF00E676),
        secondary      = Color(0xFF69FF47),
        userBubble     = Color(0xFF00C853),
        userBubbleDark = Color(0xFF1B5E20),
        surfaceTint    = Color(0xFF00E676)
    ),
    ICE(
        id = "ice", label = "Лёд",
        primary        = Color(0xFF40C4FF),
        secondary      = Color(0xFF80DEEA),
        userBubble     = Color(0xFF0288D1),
        userBubbleDark = Color(0xFF01579B),
        surfaceTint    = Color(0xFF40C4FF)
    ),
    GARNET(
        id = "garnet", label = "Гранат",
        primary        = Color(0xFFFF1744),
        secondary      = Color(0xFFAA00FF),
        userBubble     = Color(0xFFCC0033),
        userBubbleDark = Color(0xFF880022),
        surfaceTint    = Color(0xFFFF1744)
    ),
    FUCHSIA(
        id = "fuchsia", label = "Фуксия",
        primary        = Color(0xFFE040FB),
        secondary      = Color(0xFF40C4FF),
        userBubble     = Color(0xFFAB00D6),
        userBubbleDark = Color(0xFF6A0080),
        surfaceTint    = Color(0xFFE040FB)
    );

    val accentGradient: Brush
        get() = Brush.linearGradient(listOf(primary, secondary))

    companion object {
        fun fromId(id: String): AppThemeOption =
            entries.firstOrNull { it.id == id } ?: TEAL
    }
}

val LocalAppTheme = compositionLocalOf { AppThemeOption.TEAL }

// ─────────────────────────────────────────────────────────────────
// CHAT BACKGROUND
// ─────────────────────────────────────────────────────────────────

enum class ChatBackgroundOption(
    val id: String,
    val label: String,
    val topColor: Color,
    val midColor: Color,
    val bottomColor: Color,
    val surfaceColor: Color,
    val inputColor: Color
) {
    NIGHT(
        id = "night", label = "Ночь",
        topColor     = Color(0xFF0A0E27),
        midColor     = Color(0xFF121830),
        bottomColor  = Color(0xFF1A1F3A),
        surfaceColor = Color(0xFF1E2444),
        inputColor   = Color(0xFF161C38)
    ),
    OBSIDIAN(
        id = "obsidian", label = "Обсидиан",
        topColor     = Color(0xFF080B14),
        midColor     = Color(0xFF0B1020),
        bottomColor  = Color(0xFF10172E),
        surfaceColor = Color(0xFF161D30),
        inputColor   = Color(0xFF111828)
    ),
    FOREST(
        id = "forest", label = "Лес",
        topColor     = Color(0xFF081208),
        midColor     = Color(0xFF0C1A0D),
        bottomColor  = Color(0xFF102214),
        surfaceColor = Color(0xFF152B18),
        inputColor   = Color(0xFF102214)
    ),
    AMETHYST(
        id = "amethyst", label = "Аметист",
        topColor     = Color(0xFF11091E),
        midColor     = Color(0xFF160D28),
        bottomColor  = Color(0xFF1C1232),
        surfaceColor = Color(0xFF231840),
        inputColor   = Color(0xFF1A1232)
    ),
    MIDNIGHT(
        id = "midnight", label = "Полночь",
        topColor     = Color(0xFF050916),
        midColor     = Color(0xFF091220),
        bottomColor  = Color(0xFF0D1A2E),
        surfaceColor = Color(0xFF111E34),
        inputColor   = Color(0xFF0C1628)
    ),
    DUSK(
        id = "dusk", label = "Сумерки",
        topColor     = Color(0xFF180D06),
        midColor     = Color(0xFF20120A),
        bottomColor  = Color(0xFF2A180C),
        surfaceColor = Color(0xFF2E1C0E),
        inputColor   = Color(0xFF24160A)
    ),
    ABYSS(
        id = "abyss", label = "Бездна",
        topColor     = Color(0xFF020A0E),
        midColor     = Color(0xFF051018),
        bottomColor  = Color(0xFF091822),
        surfaceColor = Color(0xFF0E2030),
        inputColor   = Color(0xFF071420)
    ),
    SPACE(
        id = "space", label = "Космос",
        topColor     = Color(0xFF07081C),
        midColor     = Color(0xFF0B0D28),
        bottomColor  = Color(0xFF101234),
        surfaceColor = Color(0xFF161840),
        inputColor   = Color(0xFF0D102C)
    ),
    JADE(
        id = "jade", label = "Нефрит",
        topColor     = Color(0xFF041210),
        midColor     = Color(0xFF071C18),
        bottomColor  = Color(0xFF0B2420),
        surfaceColor = Color(0xFF10302C),
        inputColor   = Color(0xFF091E1A)
    ),
    WINE(
        id = "wine", label = "Вино",
        topColor     = Color(0xFF130510),
        midColor     = Color(0xFF1C0818),
        bottomColor  = Color(0xFF250C22),
        surfaceColor = Color(0xFF2E102C),
        inputColor   = Color(0xFF1C0A1A)
    ),
    VOLCANIC(
        id = "volcanic", label = "Вулкан",
        topColor     = Color(0xFF150806),
        midColor     = Color(0xFF200E08),
        bottomColor  = Color(0xFF2C140A),
        surfaceColor = Color(0xFF381A0E),
        inputColor   = Color(0xFF24100A)
    ),
    STEEL(
        id = "steel", label = "Сталь",
        topColor     = Color(0xFF09090E),
        midColor     = Color(0xFF0D0F18),
        bottomColor  = Color(0xFF121522),
        surfaceColor = Color(0xFF181C2E),
        inputColor   = Color(0xFF0F1220)
    ),
    BLOOD(
        id = "ruby_bg", label = "Кровь",
        topColor     = Color(0xFF120005),
        midColor     = Color(0xFF1D000A),
        bottomColor  = Color(0xFF2A0010),
        surfaceColor = Color(0xFF3A0018),
        inputColor   = Color(0xFF200008)
    ),
    EMERALD(
        id = "emerald_bg", label = "Изумруд",
        topColor     = Color(0xFF001209),
        midColor     = Color(0xFF001E10),
        bottomColor  = Color(0xFF002A1A),
        surfaceColor = Color(0xFF003824),
        inputColor   = Color(0xFF001C12)
    ),
    NOVEMBER(
        id = "twilight_bg", label = "Ноябрь",
        topColor     = Color(0xFF0C0320),
        midColor     = Color(0xFF140530),
        bottomColor  = Color(0xFF1E0842),
        surfaceColor = Color(0xFF2A0E58),
        inputColor   = Color(0xFF170638)
    ),
    CHARCOAL(
        id = "charcoal", label = "Уголь",
        topColor     = Color(0xFF060606),
        midColor     = Color(0xFF0A0A0A),
        bottomColor  = Color(0xFF101010),
        surfaceColor = Color(0xFF181818),
        inputColor   = Color(0xFF0D0D0D)
    ),
    VAPOR(
        id = "vapor", label = "Вейпор",
        topColor     = Color(0xFF080015),
        midColor     = Color(0xFF110025),
        bottomColor  = Color(0xFF1A0038),
        surfaceColor = Color(0xFF260050),
        inputColor   = Color(0xFF13002C)
    ),
    ARCTIC(
        id = "arctic_bg", label = "Арктика",
        topColor     = Color(0xFF060E18),
        midColor     = Color(0xFF0A1826),
        bottomColor  = Color(0xFF0F2336),
        surfaceColor = Color(0xFF152E46),
        inputColor   = Color(0xFF0C1A2C)
    ),
    NEBULA(
        id = "nebula", label = "Туманность",
        topColor     = Color(0xFF060A1E),
        midColor     = Color(0xFF0A0E2E),
        bottomColor  = Color(0xFF10123E),
        surfaceColor = Color(0xFF181A52),
        inputColor   = Color(0xFF0C1034)
    ),
    ONYX(
        id = "onyx", label = "Оникс",
        topColor     = Color(0xFF040408),
        midColor     = Color(0xFF07070F),
        bottomColor  = Color(0xFF0C0B18),
        surfaceColor = Color(0xFF121022),
        inputColor   = Color(0xFF090912)
    );

    val gradient: Brush
        get() = Brush.verticalGradient(listOf(topColor, midColor, bottomColor))

    companion object {
        fun fromId(id: String): ChatBackgroundOption =
            entries.firstOrNull { it.id == id } ?: NIGHT
    }
}

val LocalChatBackground = compositionLocalOf { ChatBackgroundOption.NIGHT }

// ─────────────────────────────────────────────────────────────────
// FONT FAMILY
// ─────────────────────────────────────────────────────────────────

enum class AppFontFamily(
    val id: String,
    val label: String,
    val subtitle: String,
    val fontFamily: FontFamily,
    val sample: String
) {
    DEFAULT(
        id = "default", label = "Системный", subtitle = "Roboto",
        fontFamily = FontFamily.Default,
        sample = "Привет, Дима!"
    ),
    SERIF(
        id = "serif", label = "Классика", subtitle = "Serif",
        fontFamily = FontFamily.Serif,
        sample = "Привет, Дима!"
    ),
    MONOSPACE(
        id = "monospace", label = "Моно", subtitle = "Monospace",
        fontFamily = FontFamily.Monospace,
        sample = "Привет, Дима!"
    ),
    CURSIVE(
        id = "cursive", label = "Скрипт", subtitle = "Cursive",
        fontFamily = FontFamily.Cursive,
        sample = "Привет, Дима!"
    );

    companion object {
        fun fromId(id: String): AppFontFamily =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

val LocalAppFontFamily = compositionLocalOf { AppFontFamily.DEFAULT }
