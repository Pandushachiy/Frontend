# Детальная Спецификация Дизайна ChatGPT-Стиль Android App с Glassmorphism
## Kotlin Compose | Material Design 3 | Apple Glass Design Philosophy

---

## 🎨 АРХИТЕКТУРА ДИЗАЙНА

### Цветовая Палитра (Dynamic Color + Custom)

```kotlin
// Основная палитра для темного режима (primary)
val GlassBackground = Color(0xFF0A0E27)        // Глубокий тёмный градиент
val GlassSurface = Color(0xFF1A1F3A)           // Матовое стекло база
val GlassAltSurface = Color(0xFF252D45)        // Чуть светлее для вложений
val GlassAccent = Color(0xFF6366F1)            // Индиго (modern, не кричащий)
val GlassAccentLight = Color(0xFF818CF8)       // Индиго lighter для hover/interaction
val TextPrimary = Color(0xFFFFFFFF)            // Чистый белый
val TextSecondary = Color(0xFFB0B0C0)          // Серебристо-серый (не простой серый)
val TextTertiary = Color(0xFF8B8B9A)           // Более приглушённый
val UserBubble = Color(0xFF2563EB)             // Синий (telegram style)
val AssistantBubble = Color(0xFF1A1F3A)        // Тот же стеклянный
val BubbleGradient1 = Color(0xFF6366F1)        // Индиго
val BubbleGradient2 = Color(0xFF8B5CF6)        // Фиолетовый

// Семикрасивые полупрозрачные слои
val GlassOverlay20 = Color(0x33FFFFFF)         // 20% белый оверлей
val GlassOverlay10 = Color(0x1AFFFFFF)         // 10% белый оверлей
val GlassOverlay05 = Color(0x0DFFFFFF)         // 5% белый оверлей
val DarkOverlay30 = Color(0x4D000000)          // 30% чёрный для depth
```

---

## 💬 СПЕЦИФИКАЦИЯ ЧАТА (ГЛАВНОЕ!)

### Макет Экрана

```
┌──────────────────────────────────────┐
│  ← BACK    Chat Name    ⋮ MENU       │  ← TopAppBar (40dp высота)
├──────────────────────────────────────┤
│                                      │
│  [Message Area - LazyColumn]         │  ← Scrollable message list
│  Max Width: 90% of screen            │
│  Padding: 12dp horizontal            │
│  Message spacing: 8dp (vertical)     │
│                                      │
│  12.01 | "Привет, это ассистент"   │
│         пришёл медленнее, правый     │
│         край с типографией bubble    │  ← Assistant message (left)
│                                      │
│  12.02 |                    "Здравствуй!" │  ← User message (right)
│                                      │
├──────────────────────────────────────┤
│  [Input Area]                        │
│  ┌─────────────────────────────────┐ │
│  │ Тип сообщение...        📎 🎤 ▶ │ │  ← TextField + actions
│  └─────────────────────────────────┘ │
│                                      │
└──────────────────────────────────────┘
```

### Параметры Сообщения (Message Bubble)

#### **Assistant Message (Левая сторона)**

```kotlin
// Контейнер
Shape: RoundedCornerShape(
    topStart = 4.dp,      // Углы как в Telegram
    topEnd = 12.dp,       // Разные углы для направления
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)
MaxWidth: 85% screen width
MinWidth: 60dp
Padding: 12.dp (horizontal 12, vertical 10)
Background: 
  - Base: GlassSurface (Color(0xFF1A1F3A))
  - Border: GlassOverlay10 (10% white overlay)
  - Shadow: elevation 2.dp, alpha 0.25

Text Properties:
  - Font: system sans-serif (Roboto по умолчанию)
  - Size: 15.sp base
  - LineHeight: 1.4 (23sp for 15sp text)
  - Color: TextPrimary
  - LetterSpacing: 0.25.sp (natural, не compressed)
  - Padding Internal: 12.dp horizontal, 10.dp vertical

Spacing:
  - Before Message: 8.dp от предыдущего
  - Left margin: 12.dp от края экрана
  - Between timestamp и bubble: 6.dp vertical
```

#### **User Message (Правая сторона)**

```kotlin
Shape: RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 4.dp,       // Mirror layout
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)
MaxWidth: 85% screen width
Alignment: Align.End (Right)
Background: Gradient
  - Start: Color(0xFF2563EB) (Telegram Blue)
  - End: Color(0xFF1E40AF) (slightly darker)
  - Angle: 135 degrees (top-left to bottom-right)
  
// Для иллюзии glass effect поверх
Overlay: GlassOverlay20 (20% белый)
Shadow: elevation 3.dp, alpha 0.3 (чуть больше, чтобы выделялось)

Text Properties:
  - Identical to Assistant (15.sp, 1.4 line height)
  - Color: TextPrimary (белый)
  - LetterSpacing: 0.25.sp
  - Padding: 12.dp horizontal, 10.dp vertical

Spacing:
  - Right margin: 12.dp от края
  - Same vertical spacing: 8.dp
```

#### **Timestamp & Read Status**

```kotlin
Position: Below bubble, aligned to bubble edge
Font: 11.sp
Color: TextTertiary (Color(0xFF8B8B9A))
Padding: 6.dp top от bubble
Format: "12:34" (24h format)
Read status: "✓✓" grey (Color(0xFF6B7280)) for assistant side

Code:
Text(
    text = "12:34 ✓✓",
    fontSize = 11.sp,
    color = TextTertiary,
    modifier = Modifier.padding(top = 6.dp)
)
```

### Группировка Сообщений (Consecutive Messages)

```kotlin
// Если 2 сообщения подряд одного автора - убрать top padding
// Если другой автор или перерыв > 3 минут - добавить spacing: 12.dp

SpacingBetweenGroups = 12.dp
SpacingWithinGroup = 2.dp  // Минимальное, чтобы не слилось

// Пример структуры
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)  // Between groups
) {
    // Group 1: Assistant
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MessageBubble(text = "First message")
        MessageBubble(text = "Second message")
    }
    
    // Group 2: User
    MessageBubble(text = "User response", isUser = true)
}
```

### Обработка Длинного Текста

```kotlin
Text(
    text = message,
    fontSize = 15.sp,
    lineHeight = 23.sp,
    softWrap = true,        // ОБЯЗАТЕЛЬНО
    overflow = TextOverflow.Clip,
    modifier = Modifier.widthIn(max = MaxBubbleWidth)  // Не давай тексту расширяться более 85%
)

// Для кода внутри сообщений
CodeBlock(
    code = "fun main() { }",
    modifier = Modifier
        .fillMaxWidth(0.9f)
        .padding(8.dp)
        .background(
            color = Color(0xFF0F1419),
            shape = RoundedCornerShape(6.dp)
        )
        .padding(8.dp)
)
```

### Input Field (TextField Area)

```kotlin
Container:
  Background: GlassSurface (Color(0xFF1A1F3A)) with GlassOverlay10
  Shape: RoundedCornerShape(12.dp)
  Elevation: 2.dp
  Padding: 12.dp (horizontal), 8.dp (vertical around content)
  Margin: 12.dp (horizontal from screen edges), 16.dp (bottom)

TextField:
  - Hint: "Напишите сообщение..." (TextSecondary color)
  - TextColor: TextPrimary
  - CursorColor: GlassAccent (индиго)
  - SelectionColor: GlassAccent with alpha 0.3
  - MaxLines: 4 (allow wrapping but don't make it huge)
  - ImeAction: Send
  - KeyboardType: Text

Action Buttons (Right side):
  - Attachment button: 📎 (или custom icon, НЕ emoji!)
  - Voice button: 🎤 (иконка микрофона, не цветной emoji)
  - Send button: ➤ (стрелка, заполняется при наличии текста)
  - Button size: 36.dp
  - Button color when inactive: TextTertiary
  - Button color when active: GlassAccent
  - Spacing between buttons: 8.dp

Code example:
```kotlin
OutlinedTextField(
    value = messageText,
    onValueChange = { messageText = it },
    modifier = Modifier
        .fillMaxWidth(0.9f)
        .heightIn(min = 48.dp, max = 120.dp)
        .background(
            color = GlassSurface,
            shape = RoundedCornerShape(12.dp)
        ),
    placeholder = {
        Text("Напишите сообщение...", color = TextTertiary)
    },
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GlassAccent,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = GlassAccent
    ),
    trailingIcon = {
        Row(
            modifier = Modifier
                .padding(end = 8.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { /* attach */ }, modifier = Modifier.size(36.dp)) {
                Icon(painter = painterResource(R.drawable.ic_attach), 
                     contentDescription = null)
            }
            IconButton(onClick = { /* send */ }, modifier = Modifier.size(36.dp)) {
                Icon(painter = painterResource(R.drawable.ic_send),
                     contentDescription = null)
            }
        }
    }
)
```

---

## 🌟 GLASSMORPHISM ЭФФЕКТЫ

### Фоновый Градиент (Весь App)

```kotlin
// Основной фон должен быть динамичным градиентом
Background Gradient (для всего экрана):
  - Цвет 1 (top-left): Color(0xFF0A0E27)
  - Цвет 2 (center): Color(0xFF1A1F3A)
  - Цвет 3 (bottom-right): Color(0xFF0F1B2E)
  - Type: LinearGradient или RadialGradient для глубины
  - Angle: 135 градусов (diagonal)

// Луши/light leak effects (опционально, но крас иво):
  - Blur effect за message bubbles: BlurredEdge(4.dp)
  - Subtle glow на Accent элементах: GlassAccent with alpha 0.15

Code:
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            brush = linearGradient(
                colors = listOf(
                    Color(0xFF0A0E27),
                    Color(0xFF1A1F3A),
                    Color(0xFF0F1B2E)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        )
)
```

### Glass Surface Effect (Bubble Background)

```kotlin
// Структурированный эффект для каждого bubble
Surface(
    modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(
            brush = linearGradient(
                colors = listOf(
                    GlassSurface.copy(alpha = 0.9f),
                    GlassSurface.copy(alpha = 0.7f)
                )
            )
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f),  // Subtle frost line
            shape = RoundedCornerShape(12.dp)
        ),
    shape = RoundedCornerShape(12.dp),
    shadowElevation = 2.dp,
    color = Color.Transparent  // Пусть градиент работает
) {
    // Content here
}
```

### Тень и Depth

```kotlin
// Правильная тень для glass effect
shadowElevation = 2.dp          // Основное сообщение
shadowColor = Color.Black,      // Чёрная тень (не серая)
shadowAlpha = 0.25f             // Полупрозрачная

// Для более выделяющихся элементов (user messages)
shadowElevation = 3.dp
shadowAlpha = 0.3f

// Blur radius (если использовать Modifier.shadow)
blur = 8.dp
offset = Offset(0f, 2f)
```

### Прозрачность (Opacity) Rules

```kotlin
GlassOverlay20 (0xFF33FFFFFF)   = используется для тонких линий/borders
GlassOverlay10 (0xFF1AFFFFFF)   = для очень тонких разделителей
GlassOverlay05 (0xFF0DFFFFFF)   = для едва видимых эффектов
DarkOverlay30 (0xFF4D000000)    = для background behind modals

// Правило: если не видно разницы между двумя элементами
// - сделай прозрачность ниже или выше на 10%
```

---

## 📱 ТАБЛИЦА РАЗМЕРОВ И ОТСТУПОВ

```
FONT SIZES
─────────────────────────────────
Heading (TopAppBar title):    18.sp, weight 600
Message text (primary):       15.sp, weight 400
Message text (code/special):  13.sp, weight 500 (monospace)
Timestamp:                    11.sp, weight 400
Input placeholder:            15.sp, weight 400, italic

SPACING
─────────────────────────────────
Screen edge:                  12.dp
Between bubbles (same author): 2.dp
Between bubble groups:        12.dp
Between sections:             16.dp
Input area bottom:            16.dp
Bubble internal:              12.dp (h), 10.dp (v)
Icon/button size:             36.dp (pressable area)
Icon visual size:             24.dp

BORDER RADIUS
─────────────────────────────────
Bubbles:                      12.dp (corners), 4.dp (conversation corner)
Input field:                  12.dp
Buttons:                      8.dp
Modals/Dialogs:               16.dp
Chips/Tags:                   6.dp (tighter)

ELEVATION
─────────────────────────────────
Assistant bubble:             2.dp
User bubble:                  3.dp
Input field:                  2.dp
TopAppBar:                    2.dp
Modal/Dialog:                 4.dp
Floating button:              6.dp

LINE HEIGHT
─────────────────────────────────
Message text:                 1.4 (23.sp for 15.sp)
Title:                        1.2 (21.6sp for 18sp)
Caption:                      1.3
Code:                         1.5 (для лучшей читаемости кода)
```

---

## 🔄 АНИМАЦИИ И ПЕРЕХОДЫ

### Появление Сообщения

```kotlin
var messageAlpha by remember { mutableStateOf(0f) }

LaunchedEffect(message) {
    animate(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300, easing = EaseOutCubic)
    ) { value ->
        messageAlpha = value
    }
}

modifier = Modifier
    .alpha(messageAlpha)
    .animateContentSize(animationSpec = spring(dampingRatio = 0.8f))
```

### Scroll Animation

```kotlin
// Плавный scroll при появлении нового сообщения
val scrollState = rememberLazyListState()
LaunchedEffect(messages.size) {
    scrollState.animateScrollToItem(messages.lastIndex)
}
```

### Input Field Focus

```kotlin
var isFocused by remember { mutableStateOf(false) }
val focusColor = animateColorAsState(
    targetValue = if (isFocused) GlassAccent else GlassSurface,
    label = "InputFocus"
)

OutlinedTextField(
    // ...
    modifier = Modifier
        .onFocusEvent { isFocused = it.isFocused }
        .background(color = focusColor.value)
)
```

### Loading Animation (Typing Indicator)

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.padding(12.dp)
) {
    repeat(3) { index ->
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TextSecondary)
                .animateContentSize()
                .alpha(
                    animateFloatAsState(
                        targetValue = if (animationIndex == index) 1f else 0.4f,
                        label = "TypingBounce"
                    ).value
                )
        )
    }
}
```

---

## 🎯 ДРУГИЕ ВКЛАДКИ

### General Surface Style

```kotlin
// Для всех остальных вкладок (Documents, Profile, Settings)
используй тот же glass effect:

Background: GlassBackground gradient
Surfaces: GlassSurface с GlassOverlay10 border
Cards: RoundedCornerShape(12.dp) + тень 2.dp
Buttons:
  - Primary: GlassAccent gradient (индиго → фиолетовый)
  - Secondary: GlassSurface с border GlassAccent
  - Tertiary: Transparent с border
```

### Icons & Assets

```
ИКОНКИ - САМОЕ ВАЖНОЕ:
✗ Не используй emoji (выглядит дешево и нестабильно на разных версиях)
✗ Не используй Material Icons по умолчанию (они слишком угловатые)
✓ Используй Font Awesome, Feather Icons или собственные SVG
✓ Icon size: 24.dp (visual), padding around = 36dp pressable
✓ Color: TextTertiary (by default), GlassAccent (when active/hover)
✓ Stroke width: 1.5dp (для тонких линий)
✓ Style: Minimal, clean lines, no fill (outline only)

Recommended icon set:
- Send: стрелка right (➤ но как иконка, не emoji)
- Attach: скрепка (╰⟡╮ но clean)
- Voice: микрофон с 2-3 волнами
- Menu: три горизонтальные точки
- Back: стрелка left
- Settings: gear / слегка декоративный
- Documents: папка или листок бумаги
- Profile: круг с инициалами (не аватар по умолчанию)
```

---

## ✅ ФИНАЛЬНЫЙ ЧЕКЛИСТ КАЧЕСТВА

```
[ ] Все bubbles имеют правильные margins и padding
[ ] Message text читабелен (15sp, 1.4 line height)
[ ] Timestamps видны но не назойливы (11sp, TextTertiary)
[ ] Input field имеет фокус feedback (border color change)
[ ] Icons НЕ emoji, clean stroke weight
[ ] Группы сообщений правильно разделены (12dp между группами)
[ ] User bubbles справа, Assistant слева
[ ] Градиент на фоне целостный и не отвлекающий
[ ] Тени subtle но видны (2-3.dp elevation)
[ ] Прозрачность используется правильно (borders 10%, overlay 20%)
[ ] Нет лишних линий и рамок
[ ] Анимации smooth (300ms для появления, 150ms для focus)
[ ] Весь текст единым шрифтом и весом (Roboto 400w regular)
[ ] Цветовая гамма: индиго + синий + глубокий чёрный (не яркие цвета)
[ ] Glass effect видна на bubbles (border + gradient background)
[ ] Тёмный режим (light mode - optional)
[ ] LazyColumn с правильной структурой (мессажи, не пересчитываются)
[ ] RTL support if needed (для русского должен быть OK)
```

---

## 📝 ПРИМЕР ПОЛНОГО КОДА ДЛЯ CHAT MESSAGE

```kotlin
@Composable
fun ChatMessageItem(
    message: Message,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = (LocalConfiguration.current.screenWidthDp * 0.85).dp)
                .shadow(
                    elevation = if (isUser) 3.dp else 2.dp,
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 12.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    ),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(
                topStart = if (isUser) 12.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = if (isUser) 
                Color.Transparent else 
                GlassSurface,
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) 
                    Color.White.copy(alpha = 0.1f) else 
                    Color.White.copy(alpha = 0.08f)
            )
        ) {
            if (isUser) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = linearGradient(
                                colors = listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF1E40AF)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 1000f)
                            )
                        )
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp, 10.dp),
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = TextPrimary,
                        softWrap = true
                    )
                }
            } else {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp, 10.dp),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    color = TextPrimary,
                    softWrap = true
                )
            }
        }
        
        Text(
            text = message.timestamp,
            modifier = Modifier.padding(top = 6.dp),
            fontSize = 11.sp,
            color = TextTertiary
        )
    }
}
```

---

## 🔗 РЕКОМЕНДАЦИИ ИЗ ДРУГИХ ПРИЛОЖЕНИЙ

**Telegram** ✓
- Правильная ширина чата (85% screen)
- Простые пузыри без лишнего
- Текст читаем, spacing правильный
- Группировка сообщений

**Telegram Web (stylized)**
- Красивое отступление сообщений
- Glass effect в некоторых версиях
- Timestamp рядом с bubble

**Apple iMessage (macOS)**
- Smooth animations при появлении
- Правильная прозрачность
- Минимум UI элементов
- Фокус на контенте

**Discord (Dark Mode)**
- Правильный цветовой контраст
- Readable fonts
- Nice spacing
- Clean icons

**Anthropic Claude Web UI**
- Минималистичная палитра
- Gradient фоны но subtle
- Правильный line height
- Clean typography

**Копируй подходы отсюда, но создавай свой уникальный стиль через:**
1. Правильное использование прозрачности
2. Subtle градиенты (не яркие)
3. Идеальный typography
4. Minimal но красивые иконки
5. Glass effect через border + subtle gradient background

---

## 🚀 ДЛЯ ПЕРЕДАЧИ CLAUDE OPUS

**Начни диалог так:**

```
Я разрабатываю Android-приложение на Kotlin Compose для чата с AI.
Важно: нужен ТОЧНЫЙ следование дизайн-спецификации без отступлений.
Вот полная спецификация дизайна (glassmorphism стиль):

[Вставь этот документ полностью]

ВАЖНЫЕ ТРЕБОВАНИЯ:
1. Все размеры, цвета, padding ТОЧНО как в спецификации
2. Иконки - НЕ emoji, только Font Awesome/Feather
3. Чат максимум 85% ширины экрана
4. Message spacing: 8dp между bubble groups, 2dp внутри группы
5. Animations: 300ms для появления сообщения, smooth scroll
6. Glass effect: border 1.dp white 10% + gradient background
7. Текст: 15sp, line height 1.4 для всех сообщений
8. Тени: 2dp для assistant, 3dp для user
9. Градиент фона: Color(0xFF0A0E27) → Color(0xFF1A1F3A) → Color(0xFF0F1B2E)
10. Input field: GlassSurface background, focus → GlassAccent border

Не добавляй ничего от себя. Все элементы дизайна должны 
точно соответствовать спецификации. Лучше переспроси, 
чем добавь что-то новое.

[ДАЛЕЕ ОПИСЫВАЕШЬ КОНКРЕТНОЕ, ЧТО НУЖНО РЕАЛИЗОВАТЬ]
```

---

## 📊 ЦВЕТОВАЯ ТАБЛИЦА (для быстрого копирования)

```kotlin
// Копируй и вставь в свой Color.kt файл
object GlassColors {
    // Backgrounds
    val GlassBackground = Color(0xFF0A0E27)
    val GlassSurface = Color(0xFF1A1F3A)
    val GlassAltSurface = Color(0xFF252D45)
    val GlassAccent = Color(0xFF6366F1)        // Indigo
    val GlassAccentLight = Color(0xFF818CF8)   // Indigo Light
    
    // Text
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0C0)
    val TextTertiary = Color(0xFF8B8B9A)
    
    // Chat
    val UserBubble = Color(0xFF2563EB)
    val UserBubbleDark = Color(0xFF1E40AF)
    val AssistantBubble = Color(0xFF1A1F3A)
    
    // Overlays
    val WhiteOverlay20 = Color(0x33FFFFFF)
    val WhiteOverlay10 = Color(0x1AFFFFFF)
    val WhiteOverlay05 = Color(0x0DFFFFFF)
    val BlackOverlay30 = Color(0x4D000000)
}
```

