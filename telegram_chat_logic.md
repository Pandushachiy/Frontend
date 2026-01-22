# Полная логика Telegram Chat для Kotlin Compose

## 1. АРХИТЕКТУРА СТРУКТУРЫ

### Основные компоненты:
```
ChatScreen
├── ChatTopBar (фиксирована вверху)
├── LazyColumn для сообщений (занимает свободное пространство)
├── InputArea (фиксирована внизу)
└── SystemInsets обработка
```

## 2. КЛЮЧЕВАЯ ЛОГИКА: IMESTRICTBOX И SYSTEMWINDOWINSETS

### Главный принцип Telegram:
- Когда появляется клавиатура → контент НЕ ужимается, а **скроллится вверх**
- Input field остается ВСЕГДА видимым внизу (над клавиатурой)
- При скрытии клавиатуры → сообщения автоматически прокручиваются вниз
- Нет резких скачков, все плавные анимации

## 3. РЕАЛИЗАЦИЯ НА KOTLIN COMPOSE

### Шаг 1: Получение высоты клавиатуры

```kotlin
// В MainActivity или ComposeActivity
WindowCompat.setDecorFitsSystemWindows(window, false)

// В composable
val insets = WindowInsets.ime  // IME = Input Method Editor (клавиатура)
val imeAnimation = animateFloatAsState(
    targetValue = if (insets.isVisible) 1f else 0f,
    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
)
```

### Шаг 2: Структура экрана

```kotlin
@Composable
fun ChatScreen() {
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current).dp
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()  // КРИТИЧНО: добавляет паддинг под клавиатуру
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ChatHeader()
            
            // Messages container - ДОЛЖНА ЗАНИМАТЬ ВСЕ СВОБОДНОЕ МЕСТО
            LazyColumn(
                modifier = Modifier
                    .weight(1f)  // Занимает все доступное пространство
                    .fillMaxWidth(),
                state = listState,
                reverseLayout = true,  // Сообщения от конца, как в Telegram
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages) { message ->
                    ChatMessageBubble(message)
                }
            }
            
            // Input area - ФИКСИРОВАНА ВНИЗУ
            ChatInputField(
                value = inputText,
                onValueChange = { 
                    inputText = it
                    isKeyboardVisible = true
                },
                onSend = {
                    // Отправка сообщения
                    coroutineScope.launch {
                        // Скролл к низу после отправки
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    }
}
```

### Шаг 3: Auto-scroll логика

```kotlin
// КЛЮЧЕВОЙ МЕХАНИЗМ: автоматическая прокрутка
LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
        // Плавная прокрутка к последнему сообщению
        listState.animateScrollToItem(
            index = 0,  // Потому что reverseLayout = true
            scrollOffset = 0
        )
    }
}

// Когда пользователь фокусирует input - скролл вниз
fun onInputFocus() {
    coroutineScope.launch {
        delay(200)  // Ждем, пока клавиатура появится
        listState.animateScrollToItem(0)
    }
}
```

### Шаг 4: Input field с динамической высотой

```kotlin
@Composable
fun ChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    var textFieldHeight by remember { mutableStateOf(0.dp) }
    val maxHeight = 100.dp
    val minHeight = 40.dp
    val density = LocalDensity.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = minHeight, max = maxHeight)
                .background(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .onSizeChanged { size ->
                    textFieldHeight = with(density) { size.height.toDp() }
                },
            textStyle = TextStyle(fontSize = 15.sp),
            singleLine = false,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        "Напишите сообщение...",
                        color = Color(0xFF999999),
                        fontSize = 15.sp
                    )
                }
                innerTextField()
            }
        )
        
        Button(
            onClick = onSend,
            modifier = Modifier
                .size(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0088CC)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("→", fontSize = 18.sp, color = Color.White)
        }
    }
}
```

### Шаг 5: Обработка фокуса и клавиатуры

```kotlin
val focusRequester = remember { FocusRequester() }

BasicTextField(
    modifier = Modifier
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                // Когда input получает фокус
                coroutineScope.launch {
                    delay(200)  // Даем время клавиатуре появиться
                    listState.animateScrollToItem(0)
                }
            }
        }
)

// Для программного фокуса
LaunchedEffect(Unit) {
    focusRequester.requestFocus()
}
```

## 4. ПОЛНАЯ ВИЭ МОДЕЛЬ

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    fun sendMessage(text: String) {
        viewModelScope.launch {
            if (text.isNotBlank()) {
                val newMessage = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = text,
                    isSent = true,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = listOf(newMessage) + _messages.value
                _inputText.value = ""
                
                // Имитация ответа
                delay(500)
                val reply = ChatMessage(
                    id = System.currentTimeMillis(),
                    text = "Спасибо за сообщение!",
                    isSent = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = listOf(reply) + _messages.value
            }
        }
    }
}
```

## 5. ОБРАБОТКА СИСТЕМНЫХ INSETS

```kotlin
// AndroidManifest.xml - убедись, что у Activity есть:
android:windowSoftInputMode="adjustResize|stateHidden"

// Или в коде Activity:
WindowCompat.setDecorFitsSystemWindows(window, false)

// В Compose используй:
Scaffold(
    modifier = Modifier.imePadding()  // Этот модификатор КРИТИЧЕН!
) {
    // Твой контент
}
```

## 6. КРИТИЧЕСКИЕ МОМЕНТЫ

### ✅ ДЕЛАЙ ТАК:
- `reverseLayout = true` в LazyColumn для правильного порядка
- `weight(1f)` для message container чтобы занимал все свободное место
- `imePadding()` чтобы не перекрывала клавиатура
- `animateScrollToItem()` вместо `scrollToItem()` для плавности
- Задержка `delay(200ms)` после фокуса перед scroll
- `LaunchedEffect(messages.size)` для auto-scroll при новых сообщениях

### ❌ НЕ ДЕЛАЙ:
- Не используй `Modifier.padding()` вместо `imePadding()`
- Не ставь input field в LazyColumn
- Не игнорируй `WindowInsets.ime`
- Не делай `scrollToItem()` без анимации
- Не забывай про `WindowCompat.setDecorFitsSystemWindows(window, false)`

## 7. ДОПОЛНИТЕЛЬНЫЕ ФИШКИ TELEGRAM

### Прокрутка при печати
```kotlin
LaunchedEffect(inputText) {
    if (inputText.isNotEmpty()) {
        // Если уже внизу - остаемся внизу
        if (listState.firstVisibleItemIndex < 3) {
            listState.animateScrollToItem(0)
        }
    }
}
```

### Reaction на скролл вверх (загрузка старых сообщений)
```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .collect { index ->
            if (index > messages.size - 5) {
                // Загрузить старые сообщения
                viewModel.loadMoreMessages()
            }
        }
}
```

### Плавное появление/скрытие клавиатуры
```kotlin
val imeVisible = WindowInsets.ime.isVisible
val animatedPadding = animateDpAsState(
    targetValue = if (imeVisible) 0.dp else 16.dp
)
```

## 8. СТРУКТУРА ДАННЫХ

```kotlin
data class ChatMessage(
    val id: Long,
    val text: String,
    val isSent: Boolean,
    val timestamp: Long,
    val isEdited: Boolean = false,
    val replyTo: ChatMessage? = null
)
```

## 9. ОСНОВНОЙ FLOW

```
1. Пользователь фокусирует input → keyboardHeight увеличивается
2. imePadding() автоматически добавляет паддинг к контенту
3. LazyColumn занимает вес(1f), поэтому сжимается но НЕ прокручивается
4. Триггер onFocusChanged → animateScrollToItem(0) прокручивает к низу
5. Пользователь пишет текст → input высота растет, остальное сжимается
6. Клавиатура скрывается → imePadding() убирается, layout возвращается в норму
7. При новом сообщении → LaunchedEffect(messages.size) скроллит вниз
```

## 10. ПОЛНЫЙ ПРИМЕР COMPOSABLE

```kotlin
@Composable
fun ChatScreenFull(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Хедер
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Сообщения
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(12.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }
            
            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateInputText(it) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 100.dp)
                        .background(
                            Color(0xFFF5F5F5),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(12.dp)
                        .focusRequester(focusRequester)
                )
                
                Button(
                    onClick = {
                        viewModel.sendMessage(inputText)
                        coroutineScope.launch {
                            delay(100)
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Text("→")
                }
            }
        }
    }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}
```

---

## SUMMARY

**Основа Telegram логики:**
1. `WindowCompat.setDecorFitsSystemWindows(window, false)` - системные insets не игнорируются
2. `imePadding()` - автоматическое добавление паддинга под клавиатуру
3. `reverseLayout = true` - правильный порядок сообщений
4. `weight(1f)` для LazyColumn - занимает все место
5. `animateScrollToItem()` - плавная прокрутка
6. `LaunchedEffect(messages.size)` - auto-scroll при новых сообщениях
7. Задержка перед scroll после фокуса - даем время клавиатуре

Это полная реализация механики Telegram в Compose!
