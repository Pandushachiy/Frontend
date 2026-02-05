# Design System Guide - Полная система дизайна

> **ВЕРСИЯ**: 1.0  
> **ДАТА**: 2026-02-05  
> **НАЗНАЧЕНИЕ**: Единая система дизайна для приложения на Android Kotlin + Jetpack Compose

---

## 📋 Содержание

1. [Color Palette - Цветовая палитра](#color-palette)
2. [Typography System - Типографика](#typography-system)
3. [Spacing System - Система отступов](#spacing-system)
4. [Corner Radius - Скругления](#corner-radius)
5. [Elevation - Тени и высота](#elevation)
6. [Icons - Иконки](#icons)
7. [Правила использования](#правила-использования)
8. [Чеклист перед коммитом](#чеклист-перед-коммитом)

---

## 🎨 Color Palette

### Основные правила
- **НИКОГДА** не используй `Color(0xFF...)` напрямую в UI компонентах
- **ВСЕГДА** используй `MaterialTheme.colorScheme.*`
- Все цвета адаптируются к Light/Dark теме автоматически

### Текущая палитра приложения

Судя по скриншотам, у тебя темная тема с такими цветами:

```kotlin
// ui/theme/Color.kt
object AppColors {
    // Основная палитра для темной темы
    val DarkBackground = Color(0xFF0D1B2A)        // Темно-синий фон
    val DarkSurface = Color(0xFF1B263B)           // Поверхности (карточки)
    val DarkSurfaceVariant = Color(0xFF2D3E52)    // Вариант поверхности
    
    val PrimaryTeal = Color(0xFF4ECDC4)           // Бирюзовый акцент (кнопки)
    val SecondaryPurple = Color(0xFF9D4EDD)       // Фиолетовый акцент
    
    val TextPrimary = Color(0xFFE0E1DD)           // Основной текст
    val TextSecondary = Color(0xFF778DA9)         // Вторичный текст
    val TextMuted = Color(0xFF546E7A)             // Приглушенный текст
    
    // Семантические цвета
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
}
```

### MaterialTheme ColorScheme настройка

```kotlin
// ui/theme/Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.PrimaryTeal,              // Основной акцентный цвет
    onPrimary = Color.White,                      // Текст на primary
    primaryContainer = AppColors.PrimaryTeal.copy(alpha = 0.2f),
    onPrimaryContainer = AppColors.PrimaryTeal,
    
    secondary = AppColors.SecondaryPurple,
    onSecondary = Color.White,
    
    background = AppColors.DarkBackground,        // Фон приложения
    onBackground = AppColors.TextPrimary,         // Текст на фоне
    
    surface = AppColors.DarkSurface,              // Карточки, поверхности
    onSurface = AppColors.TextPrimary,            // Текст на surface
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,   // Вторичный текст
    
    outline = AppColors.TextMuted,                // Границы
    outlineVariant = AppColors.TextMuted.copy(alpha = 0.3f),
    
    error = AppColors.Error,
    onError = Color.White,
    errorContainer = AppColors.Error.copy(alpha = 0.2f),
)

// Светлая тема (если нужна)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color.White,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1C),
    // ... остальные цвета
)
```

### Правила использования цветов

| Элемент UI | Цвет фона | Цвет текста | Пример использования |
|------------|-----------|-------------|----------------------|
| **Экран** | `background` | `onBackground` | Основной фон экрана |
| **Карточка** | `surface` | `onSurface` | Карточки, панели |
| **Карточка (выделенная)** | `surfaceVariant` | `onSurface` | Активная карточка |
| **Кнопка основная** | `primary` | `onPrimary` | CTA кнопки |
| **Кнопка вторичная** | `primaryContainer` | `onPrimaryContainer` | Второстепенные кнопки |
| **Иконка активная** | - | `primary` | Активные иконки в bottomBar |
| **Иконка неактивная** | - | `onSurfaceVariant` | Неактивные иконки |
| **Текст заголовок** | - | `onSurface` | H1, H2 заголовки |
| **Текст описание** | - | `onSurfaceVariant` | Подписи, описания |
| **Разделитель** | `outlineVariant` | - | Линии, границы |
| **Чип/Тег** | `secondaryContainer` | `onSecondaryContainer` | Статусы, теги |

### Примеры кода

```kotlin
// ❌ НЕПРАВИЛЬНО
Box(
    modifier = Modifier.background(Color(0xFF1B263B))
) {
    Text(
        text = "Hello",
        color = Color(0xFFE0E1DD)
    )
}

// ✅ ПРАВИЛЬНО
Box(
    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
) {
    Text(
        text = "Hello",
        color = MaterialTheme.colorScheme.onSurface
    )
}
```

```kotlin
// ❌ НЕПРАВИЛЬНО
Card(
    colors = CardDefaults.cardColors(
        containerColor = Color(0xFF1B263B)
    )
)

// ✅ ПРАВИЛЬНО
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
)
```

---

## 📝 Typography System

### Основные правила
- **НИКОГДА** не используй `fontSize`, `fontWeight` напрямую
- **ВСЕГДА** используй `MaterialTheme.typography.*`
- Если нужен кастомный стиль - создай его в `Typography` теме

### Типографическая шкала

```kotlin
// ui/theme/Type.kt
val Typography = Typography(
    // Крупные заголовки
    displayLarge = TextStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Bold
    ),
    
    // Заголовки
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    ),
    
    // Подзаголовки
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    
    // Основной текст
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),
    
    // Лейблы
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
)
```

### Когда что использовать

| Стиль | Применение | Пример |
|-------|------------|--------|
| `displayLarge` | Splash screen, заставки | Название приложения |
| `displayMedium` | Крупные акценты | Пустые состояния |
| `headlineLarge` | H1 заголовки экранов | "Настройки" |
| `headlineMedium` | H2 секции | "Здоровье" |
| `headlineSmall` | H3 подсекции | "Автоотправка" |
| `titleLarge` | Заголовки карточек | "Медицинский помощник" |
| `titleMedium` | Имена, названия | "Дмитрий" |
| `titleSmall` | Мелкие заголовки | Заголовки в списках |
| `bodyLarge` | Основной текст | Описания, параграфы |
| `bodyMedium` | Второстепенный текст | Подписи под полями |
| `bodySmall` | Вспомогательный текст | Hints, timestamps |
| `labelLarge` | Текст кнопок | "Войти", "Сохранить" |
| `labelMedium` | Метки полей | Email, Password labels |
| `labelSmall` | Маленькие лейблы | Версия, счетчики |

### Примеры использования

```kotlin
// ❌ НЕПРАВИЛЬНО
Text(
    text = "Настройки",
    fontSize = 32.sp,
    fontWeight = FontWeight.Bold
)

// ✅ ПРАВИЛЬНО
Text(
    text = "Настройки",
    style = MaterialTheme.typography.headlineLarge
)
```

```kotlin
// ❌ НЕПРАВИЛЬНО
Text(
    text = "pandushachiy@gmail.com",
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = Color(0xFF778DA9)
)

// ✅ ПРАВИЛЬНО
Text(
    text = "pandushachiy@gmail.com",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

---

## 📏 Spacing System

### Основные правила
- **НИКОГДА** не используй произвольные `.dp` значения
- **ВСЕГДА** используй `Spacing.*` константы
- Консистентность важнее идеального значения

### Система отступов

```kotlin
// ui/theme/Spacing.kt
object Spacing {
    // Базовые отступы (4dp grid system)
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
    
    // Специализированные отступы
    val screenPadding = 16.dp           // Отступ от края экрана
    val cardPadding = 16.dp             // Внутренний отступ карточки
    val buttonPadding = PaddingValues(  // Отступ внутри кнопки
        horizontal = 24.dp,
        vertical = 12.dp
    )
    val iconPadding = 12.dp             // Отступ вокруг иконки
    
    // Отступы между элементами
    val itemSpacing = 12.dp             // Между элементами в списке
    val sectionSpacing = 24.dp          // Между секциями
    val componentSpacing = 8.dp         // Между компонентами в группе
}
```

### Когда что использовать

| Размер | Значение | Применение |
|--------|----------|------------|
| `extraSmall` | 4dp | Минимальные отступы, иконки рядом с текстом |
| `small` | 8dp | Отступы между близкими элементами (текст + подпись) |
| `medium` | 16dp | Стандартные отступы (между карточками, внутри карточки) |
| `large` | 24dp | Между секциями, крупные группы |
| `extraLarge` | 32dp | Большие отступы (верх экрана, между важными блоками) |
| `huge` | 48dp | Очень большие отступы (специальные случаи) |

### Примеры использования

```kotlin
// ❌ НЕПРАВИЛЬНО
Column(
    modifier = Modifier
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    // content
}

// ✅ ПРАВИЛЬНО
Column(
    modifier = Modifier
        .padding(Spacing.screenPadding)
        .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(Spacing.itemSpacing)
) {
    // content
}
```

```kotlin
// ❌ НЕПРАВИЛЬНО
Card(
    modifier = Modifier.padding(10.dp)
) {
    Column(modifier = Modifier.padding(14.dp)) {
        // content
    }
}

// ✅ ПРАВИЛЬНО
Card(
    modifier = Modifier.padding(Spacing.medium)
) {
    Column(modifier = Modifier.padding(Spacing.cardPadding)) {
        // content
    }
}
```

### Типичные паттерны отступов

```kotlin
// Экран с карточками
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(Spacing.screenPadding),
    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
) {
    items(list) { item ->
        ItemCard(item)
    }
}

// Внутри карточки
Card {
    Column(
        modifier = Modifier.padding(Spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text("Title")
        Text("Description")
    }
}

// Форма ввода
Column(
    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
) {
    OutlinedTextField(...)
    OutlinedTextField(...)
    Button(...)
}
```

---

## 🔲 Corner Radius

### Основные правила
- Используй готовые значения скруглений
- Консистентность скруглений создаёт единый стиль

### Система скруглений

```kotlin
// ui/theme/Shape.kt
object CornerRadius {
    val none = 0.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val full = 50.dp  // или RoundedCornerShape(50%)
}

// Готовые Shape для Material3
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(CornerRadius.small),
    small = RoundedCornerShape(CornerRadius.small),
    medium = RoundedCornerShape(CornerRadius.medium),
    large = RoundedCornerShape(CornerRadius.large),
    extraLarge = RoundedCornerShape(CornerRadius.extraLarge)
)
```

### Применение

| Элемент | Скругление | Пример |
|---------|------------|--------|
| Кнопки | `medium` (12dp) | Button, IconButton |
| Карточки | `medium` (12dp) | Card в списках |
| Модальные окна | `large` (16dp) | Dialogs, BottomSheet |
| Текстовые поля | `small` (8dp) | TextField, OutlinedTextField |
| Chips | `full` (50%) | Статус-чипы |
| Аватары | `full` (50%) | Круглые аватарки |

### Примеры использования

```kotlin
// ❌ НЕПРАВИЛЬНО
Card(
    shape = RoundedCornerShape(10.dp)
)

// ✅ ПРАВИЛЬНО
Card(
    shape = RoundedCornerShape(CornerRadius.medium)
)
// или
Card(
    shape = MaterialTheme.shapes.medium
)
```

```kotlin
// Кнопка с правильным скруглением
Button(
    onClick = { },
    shape = RoundedCornerShape(CornerRadius.medium)
) {
    Text("Click me")
}

// Карточка сообщения с разными углами (облачко чата)
Surface(
    shape = RoundedCornerShape(
        topStart = CornerRadius.large,
        topEnd = CornerRadius.large,
        bottomStart = CornerRadius.small,
        bottomEnd = CornerRadius.large
    )
)
```

---

## 🎭 Elevation

### Система теней

```kotlin
// ui/theme/Elevation.kt
object AppElevation {
    val level0 = 0.dp   // Плоские элементы
    val level1 = 2.dp   // Карточки
    val level2 = 4.dp   // FAB, выделенные карточки
    val level3 = 8.dp   // Navigation drawer
    val level4 = 12.dp  // Модальные окна
    val level5 = 16.dp  // Dialogs
}
```

### Применение

```kotlin
Card(
    elevation = CardDefaults.cardElevation(
        defaultElevation = AppElevation.level1
    )
)

FloatingActionButton(
    elevation = FloatingActionButtonDefaults.elevation(
        defaultElevation = AppElevation.level2
    )
)
```

---

## 🎨 Icons

### Правила использования иконок

```kotlin
// ui/theme/Icons.kt
object AppIcons {
    val Home = Icons.Default.Home
    val Chat = Icons.Default.Chat
    val Wellness = Icons.Default.FavoriteBorder
    val Files = Icons.Default.Folder
    val Settings = Icons.Default.Settings
    
    val Add = Icons.Default.Add
    val Edit = Icons.Default.Edit
    val Delete = Icons.Default.Delete
    val Back = Icons.Default.ArrowBack
    val More = Icons.Default.MoreVert
    
    // Размеры иконок
    object Size {
        val small = 16.dp
        val medium = 24.dp  // Стандартный
        val large = 32.dp
        val extraLarge = 48.dp
    }
}
```

### Использование

```kotlin
// ✅ ПРАВИЛЬНО
Icon(
    imageVector = AppIcons.Settings,
    contentDescription = "Настройки",
    tint = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.size(AppIcons.Size.medium)
)

// С анимацией цвета
val iconColor by animateColorAsState(
    targetValue = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    },
    label = "icon_color"
)

Icon(
    imageVector = icon,
    contentDescription = label,
    tint = iconColor
)
```

---

## ⚙️ Правила использования

### Общие принципы

1. **Консистентность превыше всего**
   - Используй ТОЛЬКО значения из Design System
   - Не придумывай новые значения без крайней необходимости

2. **Никаких магических чисел**
   - Все размеры через константы
   - Все цвета через MaterialTheme
   - Все стили через Typography

3. **Адаптивность к темам**
   - Все цвета должны корректно работать в Light/Dark режимах
   - Проверяй оба варианта

4. **Переиспользование компонентов**
   - Не создавай дубликаты UI
   - Используй готовые компоненты
   - Параметризуй компоненты для гибкости

### Структура файлов темы

```
app/src/main/java/com/yourapp/ui/theme/
├── Color.kt          # AppColors + ColorScheme
├── Type.kt           # Typography
├── Theme.kt          # AppTheme композ
├── Shape.kt          # CornerRadius + Shapes
├── Spacing.kt        # Spacing константы
├── Elevation.kt      # AppElevation
└── Icons.kt          # AppIcons
```

---

## ✅ Чеклист перед коммитом

Перед тем как закоммитить UI код, проверь:

### Цвета
- [ ] Нет `Color(0xFF...)` в UI коде
- [ ] Все цвета через `MaterialTheme.colorScheme.*`
- [ ] Цвета выглядят корректно в Dark теме

### Типографика
- [ ] Нет `fontSize`, `fontWeight` напрямую
- [ ] Используется `MaterialTheme.typography.*`
- [ ] Правильные стили для контекста (headline для заголовков, body для текста и т.д.)

### Отступы
- [ ] Нет произвольных `.dp` значений
- [ ] Все отступы через `Spacing.*`
- [ ] Отступы логичны и консистентны с остальным UI

### Скругления
- [ ] Используются `CornerRadius.*` или `MaterialTheme.shapes.*`
- [ ] Нет произвольных значений в `RoundedCornerShape`

### Анимации
- [ ] Все анимации согласно Animation Guidelines
- [ ] Используются `AnimationDuration.*`
- [ ] Есть `label` в `animate*AsState`

### Компоненты
- [ ] Переиспользуются готовые компоненты из Component Catalog
- [ ] Нет дублирования кода
- [ ] Компонент параметризован где нужно

### Код
- [ ] Код форматирован
- [ ] Нет TODO/FIXME без issue
- [ ] Компонент документирован (KDoc если сложный)

---

## 🚀 Быстрый старт для новых экранов

### Шаблон экрана

```kotlin
@Composable
fun NewScreen(
    navController: NavController,
    viewModel: NewViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Заголовок",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            items(viewModel.items) { item ->
                ItemCard(
                    item = item,
                    onClick = { /* action */ }
                )
            }
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AppElevation.level1
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(AppIcons.Size.large)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## 📞 FAQ

**Q: Что делать если нужен уникальный цвет для специального элемента?**

A: Добавь его в `AppColors` и в `ColorScheme`. Не используй цвет напрямую в компоненте.

**Q: Нужен отступ 20dp, но такого нет в Spacing. Что делать?**

A: Используй ближайший (`medium` = 16dp или `large` = 24dp). Если действительно критично - добавь новое значение в `Spacing` и используй везде.

**Q: Как быть с градиентами?**

A: Создай Brush в `AppColors`:
```kotlin
val primaryGradient = Brush.linearGradient(
    colors = listOf(PrimaryTeal, SecondaryPurple)
)
```

**Q: Нужна анимация, её нет в гайде. Что делать?**

A: Смотри `Animation-Guidelines.md`. Если там тоже нет - создай по аналогии с существующими.

---

**ВАЖНО**: Этот Design System - источник истины. Все решения по UI должны приниматься на его основе. Если что-то не подходит - обсуди с командой и обнови документ, но не нарушай правила локально.
