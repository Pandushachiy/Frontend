package com.health.companion.presentation.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.data.remote.api.*
import com.health.companion.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    onComplete: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val questionnaire by viewModel.questionnaire.collectAsState()
    val currentSection by viewModel.currentSection.collectAsState()
    val answers by viewModel.answers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val sections = viewModel.sections
    val sectionTitles = mapOf(
        "basic" to Pair("👤", "Базовая информация"),
        "health" to Pair("🏥", "Здоровье"),
        "lifestyle" to Pair("🌅", "Образ жизни"),
        "social" to Pair("👥", "Окружение"),
        "goals" to Pair("🎯", "Цели"),
        "mental" to Pair("🧠", "Ментальное здоровье")
    )
    
    val currentQuestions = remember(questionnaire, currentSection) {
        val sectionKey = sections.getOrNull(currentSection)
        when (sectionKey) {
            "basic" -> questionnaire?.basic?.questions
            "health" -> questionnaire?.health?.questions
            "lifestyle" -> questionnaire?.lifestyle?.questions
            "social" -> questionnaire?.social?.questions
            "goals" -> questionnaire?.goals?.questions
            "mental" -> questionnaire?.mental?.questions
            else -> null
        } ?: emptyList()
    }
    
    GlassBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            QuestionnaireTopBar(
                currentSection = currentSection,
                totalSections = sections.size,
                onBack = onBack
            )
            
            // Progress Indicator
            ProgressIndicator(
                current = currentSection,
                total = sections.size,
                sectionTitles = sectionTitles,
                sections = sections
            )
            
            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentSection,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "section"
                ) { section ->
                    val sectionKey = sections.getOrNull(section)
                    val (emoji, title) = sectionTitles[sectionKey] ?: Pair("📝", "Анкета")
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        // Section Header
                        item {
                            SectionHeader(emoji = emoji, title = title)
                        }
                        
                        // Questions
                        items(currentQuestions) { question ->
                            QuestionCard(
                                question = question,
                                answer = answers[question.key],
                                onAnswerChange = { viewModel.setAnswer(question.key, it) }
                            )
                        }
                        
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
            
            // Bottom Navigation
            NavigationButtons(
                currentSection = currentSection,
                totalSections = sections.size,
                isSaving = isSaving,
                onBack = { viewModel.previousSection() },
                onNext = { viewModel.nextSection() },
                onSave = { viewModel.saveAnswers { onComplete() } }
            )
        }
        
        // Error Snackbar
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = GlassColors.accent)
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
    }
}

@Composable
private fun QuestionnaireTopBar(
    currentSection: Int,
    totalSections: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Назад",
                tint = GlassColors.textPrimary
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "${currentSection + 1} / $totalSections",
            style = GlassTypography.labelMedium
        )
    }
}

@Composable
private fun ProgressIndicator(
    current: Int,
    total: Int,
    sectionTitles: Map<String, Pair<String, String>>,
    sections: List<String>
) {
    val progress by animateFloatAsState(
        targetValue = (current + 1f) / total,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GlassColors.whiteOverlay10)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(GlassColors.accent, GlassColors.mint)
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Section dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            sections.forEachIndexed { index, key ->
                val (emoji, _) = sectionTitles[key] ?: Pair("•", "")
                val isActive = index == current
                val isCompleted = index < current
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* Could allow jumping to section */ }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 36.dp else 28.dp)
                            .background(
                                color = when {
                                    isActive -> GlassColors.accent
                                    isCompleted -> GlassColors.mint.copy(alpha = 0.5f)
                                    else -> GlassColors.whiteOverlay10
                                },
                                shape = CircleShape
                            )
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = if (isActive) GlassColors.accent.copy(alpha = 0.5f) else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCompleted && !isActive) "✓" else emoji,
                            fontSize = if (isActive) 18.sp else 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(text = emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = GlassTypography.titleLarge)
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    answer: Any?,
    onAnswerChange: (Any) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Question text
            Row {
                Text(
                    text = question.question,
                    style = GlassTypography.labelMedium
                )
                if (question.required) {
                    Text(
                        text = " *",
                        style = GlassTypography.labelMedium.copy(color = GlassColors.coral)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Input based on type
            when (question.type) {
                "text", "textarea" -> TextInput(
                    value = answer as? String ?: "",
                    onValueChange = onAnswerChange,
                    placeholder = question.placeholder ?: "",
                    multiline = question.type == "textarea"
                )
                
                "number" -> NumberInput(
                    value = answer as? Int,
                    onValueChange = onAnswerChange,
                    placeholder = question.placeholder ?: ""
                )
                
                "choice" -> ChoiceInput(
                    options = question.options ?: emptyList(),
                    selected = answer as? String,
                    onSelect = onAnswerChange
                )
                
                "list" -> ListInput(
                    options = question.options ?: emptyList(),
                    selected = (answer as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    onSelect = onAnswerChange
                )
                
                "slider" -> SliderInput(
                    value = (answer as? Number)?.toFloat() ?: question.min?.toFloat() ?: 0f,
                    min = question.min?.toFloat() ?: 0f,
                    max = question.max?.toFloat() ?: 10f,
                    onValueChange = { onAnswerChange(it.toInt()) }
                )
                
                "boolean" -> BooleanInput(
                    value = answer as? Boolean,
                    onValueChange = onAnswerChange
                )
                
                "date" -> DateInput(
                    value = answer as? String ?: "",
                    onValueChange = onAnswerChange,
                    placeholder = question.placeholder ?: "YYYY-MM-DD"
                )
            }
        }
    }
}

@Composable
private fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        maxLines = if (multiline) 5 else 1,
        singleLine = !multiline
    )
}

@Composable
private fun NumberInput(
    value: Int?,
    onValueChange: (Int) -> Unit,
    placeholder: String
) {
    GlassTextField(
        value = value?.toString() ?: "",
        onValueChange = { it.toIntOrNull()?.let(onValueChange) },
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ChoiceInput(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(GlassShapes.chip)
                    .background(
                        if (isSelected) GlassColors.accent.copy(alpha = 0.3f)
                        else GlassColors.whiteOverlay05,
                        GlassShapes.chip
                    )
                    .border(
                        1.dp,
                        if (isSelected) GlassColors.accent else GlassColors.whiteOverlay10,
                        GlassShapes.chip
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = option,
                    style = GlassTypography.labelMedium.copy(
                        color = if (isSelected) GlassColors.accent else GlassColors.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ListInput(
    options: List<String>,
    selected: List<String>,
    onSelect: (List<String>) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val isSelected = option in selected
            Box(
                modifier = Modifier
                    .clip(GlassShapes.chip)
                    .background(
                        if (isSelected) GlassColors.accent.copy(alpha = 0.3f)
                        else GlassColors.whiteOverlay05,
                        GlassShapes.chip
                    )
                    .border(
                        1.dp,
                        if (isSelected) GlassColors.accent else GlassColors.whiteOverlay10,
                        GlassShapes.chip
                    )
                    .clickable {
                        val newSelected = if (isSelected) {
                            selected - option
                        } else {
                            selected + option
                        }
                        onSelect(newSelected)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Text(text = "✓ ", style = GlassTypography.labelSmall.copy(color = GlassColors.accent))
                    }
                    Text(
                        text = option,
                        style = GlassTypography.labelMedium.copy(
                            color = if (isSelected) GlassColors.accent else GlassColors.textPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderInput(
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = min.toInt().toString(), style = GlassTypography.labelSmall)
            Text(
                text = value.toInt().toString(),
                style = GlassTypography.titleSmall.copy(color = GlassColors.accent)
            )
            Text(text = max.toInt().toString(), style = GlassTypography.labelSmall)
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            steps = (max - min).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = GlassColors.accent,
                activeTrackColor = GlassColors.accent,
                inactiveTrackColor = GlassColors.whiteOverlay10
            )
        )
    }
}

@Composable
private fun BooleanInput(
    value: Boolean?,
    onValueChange: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(true to "Да", false to "Нет").forEach { (boolValue, label) ->
            val isSelected = value == boolValue
            Box(
                modifier = Modifier
                    .clip(GlassShapes.medium)
                    .background(
                        if (isSelected) GlassColors.accent.copy(alpha = 0.3f)
                        else GlassColors.whiteOverlay05,
                        GlassShapes.medium
                    )
                    .border(
                        1.dp,
                        if (isSelected) GlassColors.accent else GlassColors.whiteOverlay10,
                        GlassShapes.medium
                    )
                    .clickable { onValueChange(boolValue) }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = label,
                    style = GlassTypography.labelMedium.copy(
                        color = if (isSelected) GlassColors.accent else GlassColors.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun DateInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    GlassTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun NavigationButtons(
    currentSection: Int,
    totalSections: Int,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    val isLastSection = currentSection == totalSections - 1
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        GlassColors.background.copy(alpha = 0.9f)
                    )
                )
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back button
        if (currentSection > 0) {
            GlassButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                isPrimary = false
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = GlassColors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Назад", style = GlassTypography.labelMedium)
            }
        }
        
        // Next / Save button
        GlassButton(
            onClick = if (isLastSection) onSave else onNext,
            modifier = Modifier.weight(if (currentSection > 0) 1f else 2f),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isLastSection) "Сохранить" else "Далее",
                    style = GlassTypography.labelMedium.copy(color = Color.White)
                )
                if (!isLastSection) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}
