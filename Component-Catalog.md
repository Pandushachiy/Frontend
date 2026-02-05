# Component Catalog - Каталог готовых компонентов

> **ВЕРСИЯ**: 1.0  
> **ДАТА**: 2026-02-05  
> **НАЗНАЧЕНИЕ**: Библиотека переиспользуемых UI компонентов

---

## 📋 Содержание

1. [AppTopBar - Верхняя панель](#apptopbar)
2. [AppButton - Кнопки](#appbutton)
3. [AppCard - Карточки](#appcard)
4. [AppTextField - Текстовые поля](#apptextfield)
5. [AppBottomSheet - Bottom Sheet](#appbottomsheet)
6. [AppDialog - Диалоги](#appdialog)
7. [SettingsCard - Карточка настроек](#settingscard)
8. [AppAvatar - Аватары](#appavatar)
9. [AppChip - Чипы и теги](#appchip)
10. [EmptyState - Пустые состояния](#emptystate)
11. [LoadingIndicator - Индикаторы загрузки](#loadingindicator)

---

## 🎯 Принципы компонентов

1. **Переиспользуемость** - один компонент для множества случаев
2. **Параметризация** - гибкость через параметры
3. **Консистентность** - все компоненты следуют Design System
4. **Простота** - легко использовать, легко понять
5. **Анимации** - встроенная поддержка анимаций

---

## 📱 AppTopBar

**Назначение:** Верхняя панель навигации для экранов

**Варианты:**
- С кнопкой "Назад"
- С actions (иконки справа)
- Центрованный заголовок
- С подзаголовком

### Код компонента

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = AppIcons.Back,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
```

### Использование

```kotlin
// Простой вариант
AppTopBar(
    title = "Настройки",
    onBackClick = { navController.popBackStack() }
)

// С подзаголовком
AppTopBar(
    title = "Дмитрий",
    subtitle = "pandushachiy@gmail.com",
    onBackClick = { navController.popBackStack() }
)

// С actions
AppTopBar(
    title = "Чат",
    onBackClick = { navController.popBackStack() },
    actions = {
        IconButton(onClick = { /* search */ }) {
            Icon(Icons.Default.Search, "Поиск")
        }
        IconButton(onClick = { /* more */ }) {
            Icon(Icons.Default.MoreVert, "Ещё")
        }
    }
)
```

---

## 🔘 AppButton

**Назначение:** Универсальная кнопка с анимацией нажатия

**Варианты:**
- Primary (основная)
- Secondary (вторичная)
- Outline (контурная)
- Text (текстовая)
- С иконкой

### Код компонента

```kotlin
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: ImageVector? = null
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    
    val colors = when (variant) {
        ButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        ButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        ButtonVariant.Outline -> ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
        ButtonVariant.Text -> ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
    
    val buttonModifier = modifier
        .scale(scale.value)
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch {
                            scale.animateTo(
                                0.95f,
                                spring(
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
        }
    
    when (variant) {
        ButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                colors = colors,
                shape = RoundedCornerShape(CornerRadius.medium),
                contentPadding = Spacing.buttonPadding
            ) {
                ButtonContent(text, loading, icon)
            }
        }
        ButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                colors = colors
            ) {
                ButtonContent(text, loading, icon)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                colors = colors,
                shape = RoundedCornerShape(CornerRadius.medium),
                contentPadding = Spacing.buttonPadding
            ) {
                ButtonContent(text, loading, icon)
            }
        }
    }
}

@Composable
private fun ButtonContent(
    text: String,
    loading: Boolean,
    icon: ImageVector?
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

enum class ButtonVariant {
    Primary,
    Secondary,
    Outline,
    Text
}
```

### Использование

```kotlin
// Primary кнопка
AppButton(
    text = "Сохранить",
    onClick = { /* action */ }
)

// С иконкой
AppButton(
    text = "Добавить",
    icon = Icons.Default.Add,
    onClick = { /* action */ }
)

// Loading состояние
AppButton(
    text = "Загрузка...",
    loading = true,
    onClick = { /* action */ }
)

// Secondary кнопка
AppButton(
    text = "Отмена",
    variant = ButtonVariant.Secondary,
    onClick = { /* action */ }
)
```

---

## 🃏 AppCard

**Назначение:** Универсальная карточка для контента

**Варианты:**
- Обычная карточка
- Кликабельная карточка
- С анимацией появления

### Код компонента

```kotlin
@Composable
fun AppCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    elevation: Dp = AppElevation.level1,
    animateAppearance: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var visible by remember { mutableStateOf(!animateAppearance) }
    
    LaunchedEffect(animateAppearance) {
        if (animateAppearance) {
            delay(50)
            visible = true
        }
    }
    
    AnimatedVisibility(
        visible = visible,
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
        if (onClick != null) {
            Card(
                onClick = onClick,
                modifier = modifier,
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = elevation
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.cardPadding),
                    content = content
                )
            }
        } else {
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(CornerRadius.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = elevation
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.cardPadding),
                    content = content
                )
            }
        }
    }
}
```

### Использование

```kotlin
// Простая карточка
AppCard {
    Text("Заголовок", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(Spacing.small))
    Text("Описание", style = MaterialTheme.typography.bodyMedium)
}

// Кликабельная карточка
AppCard(
    onClick = { /* action */ }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Icon(Icons.Default.Settings, "Настройки")
        Column(modifier = Modifier.weight(1f)) {
            Text("Настройки", style = MaterialTheme.typography.titleMedium)
            Text("Управление приложением", style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null)
    }
}

// С анимацией
AppCard(
    animateAppearance = true
) {
    // content
}
```

---

## ✏️ AppTextField

**Назначение:** Текстовое поле ввода с валидацией

### Код компонента

```kotlin
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = if (placeholder != null) {
                { Text(placeholder) }
            } else null,
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else null,
            trailingIcon = if (trailingIcon != null) {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null
                        )
                    }
                }
            } else null,
            isError = error != null,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = RoundedCornerShape(CornerRadius.small),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = Spacing.medium,
                    top = Spacing.extraSmall
                )
            )
        }
    }
}
```

### Использование

```kotlin
var email by remember { mutableStateOf("") }
var emailError by remember { mutableStateOf<String?>(null) }

AppTextField(
    value = email,
    onValueChange = { 
        email = it
        emailError = null
    },
    label = "Email",
    placeholder = "example@mail.com",
    leadingIcon = Icons.Default.Email,
    error = emailError,
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
)

// Пароль с показом/скрытием
var password by remember { mutableStateOf("") }
var passwordVisible by remember { mutableStateOf(false) }

AppTextField(
    value = password,
    onValueChange = { password = it },
    label = "Пароль",
    trailingIcon = if (passwordVisible) {
        Icons.Default.Visibility
    } else {
        Icons.Default.VisibilityOff
    },
    onTrailingIconClick = { passwordVisible = !passwordVisible },
    visualTransformation = if (passwordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    },
    keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
    )
)
```

---

## 📋 AppBottomSheet

**Назначение:** Модальное нижнее окно

### Код компонента

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = CornerRadius.extraLarge,
                topEnd = CornerRadius.extraLarge
            ),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.screenPadding,
                        end = Spacing.screenPadding,
                        bottom = Spacing.large
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = Spacing.small)
                    )
                }
                
                content()
            }
        }
    }
}
```

### Использование

```kotlin
var showSheet by remember { mutableStateOf(false) }

AppBottomSheet(
    visible = showSheet,
    onDismiss = { showSheet = false },
    title = "Выберите действие"
) {
    AppCard(onClick = { /* action 1 */ }) {
        Text("Действие 1")
    }
    AppCard(onClick = { /* action 2 */ }) {
        Text("Действие 2")
    }
    AppCard(onClick = { /* action 3 */ }) {
        Text("Действие 3")
    }
}
```

---

## 💬 AppDialog

**Назначение:** Модальный диалог для подтверждений

### Код компонента

```kotlin
@Composable
fun AppDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    message: String? = null,
    confirmText: String = "OK",
    dismissText: String? = "Отмена",
    onConfirm: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = if (message != null) {
                {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else null,
            confirmButton = {
                AppButton(
                    text = confirmText,
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                )
            },
            dismissButton = if (dismissText != null) {
                {
                    AppButton(
                        text = dismissText,
                        variant = ButtonVariant.Text,
                        onClick = onDismiss
                    )
                }
            } else null,
            shape = RoundedCornerShape(CornerRadius.large),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
```

### Использование

```kotlin
var showDialog by remember { mutableStateOf(false) }

AppDialog(
    visible = showDialog,
    onDismiss = { showDialog = false },
    title = "Удалить элемент?",
    message = "Это действие нельзя отменить",
    confirmText = "Удалить",
    dismissText = "Отмена",
    onConfirm = {
        // delete action
    }
)
```

---

## ⚙️ SettingsCard

**Назначение:** Карточка для настроек (из твоих скриншотов)

### Код компонента

```kotlin
@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(CornerRadius.medium)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Использование

```kotlin
SettingsCard(
    icon = Icons.Default.Person,
    title = "Пользователь",
    description = "pandushachiy@gmail.com",
    onClick = { navController.navigate("profile") }
)

SettingsCard(
    icon = Icons.Default.HealthAndSafety,
    title = "Медицинский помощник",
    description = "Симптомы, подсказки, анализы",
    onClick = { navController.navigate("health") }
)
```

---

## 👤 AppAvatar

**Назначение:** Аватар пользователя

### Код компонента

```kotlin
@Composable
fun AppAvatar(
    imageUrl: String? = null,
    initials: String? = null,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
    
    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            // Здесь загрузка изображения через Coil/Glide
            // AsyncImage(model = imageUrl, contentDescription = null)
        } else if (initials != null) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
```

### Использование

```kotlin
// С инициалами
AppAvatar(
    initials = "ДП",
    size = 48.dp
)

// С изображением
AppAvatar(
    imageUrl = "https://example.com/avatar.jpg",
    size = 64.dp,
    onClick = { /* open profile */ }
)
```

---

## 🏷️ AppChip

**Назначение:** Чипы и теги

### Код компонента

```kotlin
@Composable
fun AppChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "chip_bg"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "chip_text"
    )
    
    Surface(
        modifier = modifier
            .height(32.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(CornerRadius.full),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}
```

### Использование

```kotlin
// Простой чип
AppChip(label = "Тег")

// Кликабельный с выбором
var selected by remember { mutableStateOf(false) }
AppChip(
    label = "Фильтр",
    selected = selected,
    onClick = { selected = !selected }
)

// С иконкой
AppChip(
    label = "Важно",
    icon = Icons.Default.Star,
    selected = true
)
```

---

## 🗂️ EmptyState

**Назначение:** Пустое состояние списков

### Код компонента

```kotlin
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.small))
            AppButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}
```

### Использование

```kotlin
if (items.isEmpty()) {
    EmptyState(
        icon = Icons.Default.Inbox,
        title = "Нет сообщений",
        description = "Здесь будут отображаться ваши чаты",
        actionText = "Создать чат",
        onActionClick = { /* create chat */ }
    )
}
```

---

## ⏳ LoadingIndicator

**Назначение:** Индикатор загрузки

### Код компонента

```kotlin
@Composable
fun LoadingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
    overlay: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (overlay) {
                        Modifier.background(Color.Black.copy(alpha = 0.5f))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }
    }
}
```

### Использование

```kotlin
Box {
    // Content
    Content()
    
    // Loading overlay
    LoadingIndicator(
        visible = isLoading,
        overlay = true
    )
}
```

---

## ✅ Общие рекомендации

1. **Всегда используй готовые компоненты** вместо создания новых
2. **Параметризуй** компоненты для гибкости
3. **Документируй** сложные компоненты
4. **Тестируй** компоненты в разных состояниях
5. **Анимируй** где это улучшает UX

---

**ВАЖНО:** Этот каталог должен расти. Когда создаёшь новый переиспользуемый компонент - добавляй его сюда с документацией и примерами использования.
