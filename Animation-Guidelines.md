# Animation Guidelines - Полное руководство по анимациям

> **ВЕРСИЯ**: 1.0  
> **ДАТА**: 2026-02-05  
> **НАЗНАЧЕНИЕ**: Единая система анимаций для всего приложения

---

## 📋 Содержание

1. [Основные принципы анимаций](#основные-принципы)
2. [Duration - Длительность](#duration)
3. [Easing - Функции сглаживания](#easing)
4. [Типы анимаций](#типы-анимаций)
5. [Навигационные переходы](#навигационные-переходы)
6. [Примеры для каждого случая](#примеры-для-каждого-случая)

---

## 🎯 Основные принципы

### Зачем нужны анимации?
- **Понимание**: Показывают причинно-следственные связи
- **Плавность**: Делают интерфейс живым и отзывчивым
- **Ориентация**: Помогают понять где находишься
- **Внимание**: Направляют фокус пользователя

### Правила хороших анимаций
1. **Быстрые**: Пользователь не должен ждать (100-500ms)
2. **Естественные**: Физика мира (замедление на конце)
3. **Консистентные**: Одинаковые для похожих действий
4. **Тонкие**: Не отвлекают от контента

### ❌ Плохие анимации
- Слишком медленные (>500ms)
- Линейные (без easing)
- Разные для одинаковых действий
- Слишком агрессивные (большие смещения, резкие появления)

---

## ⏱️ Duration

### Система длительностей

```kotlin
// ui/theme/Animation.kt
object AnimationDuration {
    const val INSTANT = 0           // Нет анимации
    const val FAST = 150           // Быстрые микроанимации
    const val NORMAL = 300         // Стандартная длительность
    const val MODERATE = 500       // Умеренная для сложных переходов
    const val SLOW = 700           // Медленная для эффектных переходов
}
```

### Когда что использовать

| Длительность | Значение | Применение |
|--------------|----------|------------|
| `INSTANT` | 0ms | Без анимации (редко) |
| `FAST` | 150ms | Hover, ripple, toggle, мелкие изменения |
| `NORMAL` | 300ms | **90% анимаций**: появление/скрытие, смена цвета, fade |
| `MODERATE` | 500ms | Навигация между экранами, сложные трансформации |
| `SLOW` | 700ms | Драматические эффекты, редко используется |

### Правило: Используй `NORMAL` если сомневаешься

---

## 🎨 Easing

### Функции сглаживания (Easing Functions)

```kotlin
// ui/theme/Animation.kt
object AppEasing {
    // Material Design standard easing
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1f)
    val Accelerate = CubicBezierEasing(0.4f, 0.0f, 1f, 1f)
    
    // Linear (использовать редко!)
    val Linear = LinearEasing
}
```

### Визуальное представление

```
Standard (0.4, 0.0, 0.2, 1.0):
    ^
   /
  /
 /      - Плавное ускорение и замедление
/_____>

Decelerate (0.0, 0.0, 0.2, 1.0):
    ^
   /
  /       - Быстрый старт, плавное замедление
 /        - Для элементов входящих на экран
/______>

Accelerate (0.4, 0.0, 1.0, 1.0):
^
 \
  \       - Плавный старт, резкое завершение
   \      - Для элементов уходящих с экрана
    \___>
```

### Когда что использовать

| Easing | Применение |
|--------|------------|
| `Standard` | **Универсальное** - fade, color change, большинство анимаций |
| `Decelerate` | Элементы **появляются** (slide in, scale up) |
| `Accelerate` | Элементы **исчезают** (slide out, scale down) |
| `Emphasized` | Акцентные анимации (FAB expand, bottom sheet) |
| `Linear` | Только для бесконечных анимаций (loading spinner) |

---

## 🎬 Типы анимаций

### 1. Fade (Появление/Исчезновение)

**Когда использовать:**
- Появление/скрытие элементов
- Смена контента
- Overlay (затемнение фона)

**Код:**

```kotlin
// Простой fade
var visible by remember { mutableStateOf(false) }

AnimatedVisibility(
    visible = visible,
    enter = fadeIn(
        animationSpec = tween(
            durationMillis = AnimationDuration.NORMAL,
            easing = AppEasing.Standard
        )
    ),
    exit = fadeOut(
        animationSpec = tween(
            durationMillis = AnimationDuration.NORMAL,
            easing = AppEasing.Standard
        )
    )
) {
    Content()
}

// Альтернатива через alpha
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(
        durationMillis = AnimationDuration.NORMAL,
        easing = AppEasing.Standard
    ),
    label = "alpha"
)

Box(modifier = Modifier.alpha(alpha)) {
    Content()
}
```

---

### 2. Slide (Скольжение)

**Когда использовать:**
- Элементы входят/выходят с края экрана
- Смена экранов в навигации
- Drawer, Side panels

**Направления:**
- `slideInVertically` / `slideOutVertically` - сверху/снизу
- `slideInHorizontally` / `slideOutHorizontally` - слева/справа

**Код:**

```kotlin
// Появление снизу
AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight }, // Начинает за экраном
        animationSpec = tween(
            durationMillis = AnimationDuration.MODERATE,
            easing = AppEasing.Decelerate
        )
    ),
    exit = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight },
        animationSpec = tween(
            durationMillis = AnimationDuration.NORMAL,
            easing = AppEasing.Accelerate
        )
    )
) {
    Content()
}

// Появление справа
AnimatedVisibility(
    visible = visible,
    enter = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth }
    ),
    exit = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth }
    )
)
```

**Типичные значения offset:**
- `{ it }` - Полная высота/ширина (начинает за экраном)
- `{ it / 2 }` - Половина (мягче)
- `{ it / 3 }` - Треть (ещё мягче, рекомендуется для карточек)

---

### 3. Scale (Масштабирование)

**Когда использовать:**
- Press эффект на кнопках
- Появление FAB
- Акцентные элементы
- Модальные окна

**Код:**

```kotlin
// Появление с увеличением
AnimatedVisibility(
    visible = visible,
    enter = scaleIn(
        initialScale = 0.8f,
        animationSpec = tween(
            durationMillis = AnimationDuration.NORMAL,
            easing = AppEasing.Decelerate
        )
    ),
    exit = scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(
            durationMillis = AnimationDuration.NORMAL,
            easing = AppEasing.Accelerate
        )
    )
) {
    Content()
}

// Press эффект (интерактивный)
val scale = remember { Animatable(1f) }
val coroutineScope = rememberCoroutineScope()

Box(
    modifier = Modifier
        .scale(scale.value)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 0.95f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    tryAwaitRelease()
                    coroutineScope.launch {
                        scale.animateTo(1f)
                    }
                }
            )
        }
) {
    Content()
}
```

---

### 4. Color (Изменение цвета)

**Когда использовать:**
- Смена состояния (selected/unselected)
- Hover эффекты
- Индикаторы активности

**Код:**

```kotlin
val backgroundColor by animateColorAsState(
    targetValue = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    },
    animationSpec = tween(
        durationMillis = AnimationDuration.NORMAL,
        easing = AppEasing.Standard
    ),
    label = "background_color"
)

Box(
    modifier = Modifier.background(backgroundColor)
) {
    Content()
}
```

---

### 5. Size (Изменение размера)

**Когда использовать:**
- Expand/Collapse панели
- Показать/скрыть детали

**Код:**

```kotlin
// AnimateContentSize для автоматического размера
Column(
    modifier = Modifier.animateContentSize(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
) {
    Text("Always visible")
    if (expanded) {
        Text("Expandable content")
    }
}

// Ручное управление высотой
val height by animateDpAsState(
    targetValue = if (expanded) 200.dp else 100.dp,
    animationSpec = spring(),
    label = "height"
)

Box(modifier = Modifier.height(height))
```

---

### 6. Rotation (Вращение)

**Когда использовать:**
- Иконки (chevron, arrow)
- Индикаторы загрузки
- Интерактивные элементы

**Код:**

```kotlin
val rotation by animateFloatAsState(
    targetValue = if (expanded) 180f else 0f,
    animationSpec = tween(
        durationMillis = AnimationDuration.NORMAL,
        easing = AppEasing.Standard
    ),
    label = "rotation"
)

Icon(
    imageVector = Icons.Default.ExpandMore,
    contentDescription = null,
    modifier = Modifier.rotate(rotation)
)
```

---

## 🧭 Навигационные переходы

### Переходы между экранами

**Стандартный переход (Forward navigation):**

```kotlin
// В NavHost
composable(
    route = "details",
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = AnimationDuration.MODERATE,
                easing = AppEasing.Decelerate
            )
        ) + fadeIn(
            animationSpec = tween(AnimationDuration.MODERATE)
        )
    },
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 }, // Немного влево
            animationSpec = tween(
                durationMillis = AnimationDuration.MODERATE,
                easing = AppEasing.Standard
            )
        ) + fadeOut(
            animationSpec = tween(AnimationDuration.MODERATE)
        )
    },
    popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = AnimationDuration.MODERATE,
                easing = AppEasing.Decelerate
            )
        ) + fadeIn(
            animationSpec = tween(AnimationDuration.MODERATE)
        )
    },
    popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = AnimationDuration.MODERATE,
                easing = AppEasing.Accelerate
            )
        ) + fadeOut(
            animationSpec = tween(AnimationDuration.MODERATE)
        )
    }
)
```

### Bottom Sheet появление

```kotlin
val sheetState = rememberModalBottomSheetState()

ModalBottomSheet(
    onDismissRequest = { },
    sheetState = sheetState
) {
    // Content
}

// Анимация управляется автоматически:
// - slideInVertically снизу
// - fadeIn
// - MODERATE duration
```

---

## 📦 Примеры для каждого случая

### Появление карточки при скролле

```kotlin
@Composable
fun AnimatedCard(
    visible: Boolean,
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible) {
            delay(delay.toLong())
            isVisible = true
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AnimationDuration.NORMAL,
                easing = AppEasing.Standard
            )
        ) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(
                durationMillis = AnimationDuration.NORMAL,
                easing = AppEasing.Decelerate
            )
        )
    ) {
        content()
    }
}

// Использование:
LazyColumn {
    itemsIndexed(items) { index, item ->
        AnimatedCard(
            visible = true,
            delay = index * 50 // Staggered animation
        ) {
            ItemCard(item)
        }
    }
}
```

---

### Кнопки (Press effect)

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    
    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale.value)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scale.animateTo(
                                targetValue = 0.95f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        tryAwaitRelease()
                        coroutineScope.launch {
                            scale.animateTo(1f)
                        }
                    }
                )
            }
    ) {
        Text(text)
    }
}
```

---

### Toggle (Switch/Checkbox)

```kotlin
@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val checkmarkColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(
            durationMillis = AnimationDuration.FAST,
            easing = AppEasing.Standard
        ),
        label = "checkmark_color"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(
            durationMillis = AnimationDuration.FAST
        ),
        label = "background_color"
    )
    
    IconButton(
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(CornerRadius.small)
            )
    ) {
        Icon(
            imageVector = if (checked) {
                Icons.Default.CheckBox
            } else {
                Icons.Default.CheckBoxOutlineBlank
            },
            contentDescription = null,
            tint = checkmarkColor
        )
    }
}
```

---

### Чат-сообщения (появление снизу)

```kotlin
@Composable
fun ChatMessage(
    message: Message,
    isNew: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(isNew) {
        if (isNew) {
            delay(50) // Небольшая задержка
            visible = true
        } else {
            visible = true // Старые сразу видимы
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(
                durationMillis = AnimationDuration.NORMAL,
                easing = AppEasing.Decelerate
            )
        ) + fadeIn(
            animationSpec = tween(AnimationDuration.NORMAL)
        )
    ) {
        MessageBubble(message)
    }
}
```

---

### Loading состояния

```kotlin
@Composable
fun LoadingIndicator(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
```

---

### Snackbar / Toast

```kotlin
@Composable
fun AnimatedSnackbar(
    message: String,
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = AnimationDuration.NORMAL,
                easing = AppEasing.Decelerate
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = AnimationDuration.NORMAL,
                easing = AppEasing.Accelerate
            )
        ) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            shape = RoundedCornerShape(CornerRadius.medium),
            color = MaterialTheme.colorScheme.inverseSurface
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(Spacing.medium),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}
```

---

## 🔧 Дополнительные техники

### Staggered Animations (Поочередное появление)

```kotlin
@Composable
fun StaggeredList(items: List<Item>) {
    LazyColumn {
        itemsIndexed(items) { index, item ->
            var visible by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                delay(index * 50L) // 50ms задержка между элементами
                visible = true
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically()
            ) {
                ItemCard(item)
            }
        }
    }
}
```

---

### Spring Animations (Пружинные анимации)

**Когда использовать:** Интерактивные элементы, где нужна физика

```kotlin
val offset = remember { Animatable(0f) }

LaunchedEffect(trigger) {
    offset.animateTo(
        targetValue = 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5f - немного отскакивает
            stiffness = Spring.StiffnessMedium                // 1500f - средняя скорость
        )
    )
}

// Параметры Spring:
// DampingRatioNoBouncy - без отскока (1.0f)
// DampingRatioLowBouncy - слабый отскок (0.75f)
// DampingRatioMediumBouncy - средний отскок (0.5f)
// DampingRatioHighBouncy - сильный отскок (0.25f)

// StiffnessHigh - быстрая (10000f)
// StiffnessMedium - средняя (1500f)
// StiffnessLow - медленная (200f)
// StiffnessVeryLow - очень медленная (50f)
```

---

## ✅ Чеклист анимаций

Перед тем как закоммитить анимацию, проверь:

- [ ] Используется `AnimationDuration.*` вместо произвольных ms
- [ ] Используется `AppEasing.*` вместо дефолтного easing
- [ ] Есть `label` параметр в `animate*AsState`
- [ ] Анимация не длится >500ms
- [ ] Анимация естественная (не линейная)
- [ ] Консистентна с другими похожими анимациями
- [ ] Не мешает восприятию контента
- [ ] Протестирована на медленных устройствах (если есть возможность)

---

## 🎓 Шпаргалка

### Что использовать для частых случаев:

| Сценарий | Анимация | Duration | Easing |
|----------|----------|----------|--------|
| Появление карточки | fadeIn + slideInVertically | NORMAL | Decelerate |
| Исчезновение карточки | fadeOut + slideOutVertically | NORMAL | Accelerate |
| Навигация вперёд | slideInHorizontally (справа) + fadeIn | MODERATE | Decelerate |
| Навигация назад | slideOutHorizontally (вправо) + fadeOut | MODERATE | Accelerate |
| Смена цвета | animateColorAsState | NORMAL | Standard |
| Press эффект | scale (spring) | - | Spring |
| Toggle switch | animateColorAsState | FAST | Standard |
| Expand/Collapse | animateContentSize (spring) | - | Spring |
| Bottom Sheet | slideInVertically (снизу) + fadeIn | MODERATE | Decelerate |
| Dialog | scaleIn + fadeIn | NORMAL | Decelerate |
| Loading | fadeIn + scaleIn | NORMAL | Standard |
| Snackbar | slideInVertically (снизу) + fadeIn | NORMAL | Decelerate |

---

**ВАЖНО:** Анимации делают интерфейс живым, но их избыток раздражает. Используй с умом, тестируй на реальных пользователях, следи за консистентностью.
