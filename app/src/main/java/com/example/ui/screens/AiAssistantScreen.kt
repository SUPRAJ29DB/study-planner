package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.StudyPlannerViewModel

@Composable
fun AiAssistantScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.aiActiveTab.collectAsState()
    val topicInput by viewModel.aiTopicInput.collectAsState()
    val subjectInput by viewModel.aiSubjectInput.collectAsState()
    val explanation by viewModel.aiExplanation.collectAsState()
    val quizList by viewModel.aiQuiz.collectAsState()
    val flashcards by viewModel.aiFlashcards.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var customTopicText by remember(topicInput) { mutableStateOf(topicInput) }
    var selectedSubjectName by remember(subjectInput) { mutableStateOf(subjectInput) }

    // Flashcard interactive state
    var currentCardIndex by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }

    // Quiz interactive state
    var selectedOptionIndex by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "AI Study Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Powered by Google Gemini for smart topic breakdown & practice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- Tabs ---
        item {
            ScrollableTabRow(
                selectedTabIndex = when (activeTab) {
                    "Explain" -> 0
                    "Quiz" -> 1
                    "Flashcards" -> 2
                    else -> 3
                },
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = activeTab == "Explain",
                    onClick = { viewModel.setAiActiveTab("Explain") },
                    text = { Text("Topic Explainer") }
                )
                Tab(
                    selected = activeTab == "Quiz",
                    onClick = {
                        viewModel.setAiActiveTab("Quiz")
                        if (quizList.isEmpty()) viewModel.requestAiQuiz()
                    },
                    text = { Text("AI Quiz") }
                )
                Tab(
                    selected = activeTab == "Flashcards",
                    onClick = {
                        viewModel.setAiActiveTab("Flashcards")
                        if (flashcards.isEmpty()) viewModel.requestAiFlashcards()
                    },
                    text = { Text("Flashcards") }
                )
                Tab(
                    selected = activeTab == "Strategy",
                    onClick = {
                        viewModel.setAiActiveTab("Strategy")
                        viewModel.requestAiAdvice()
                    },
                    text = { Text("Study Strategy") }
                )
            }
        }

        // --- Subject & Topic Selector Bar ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select Subject & Topic:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.take(4).forEach { sub ->
                            FilterChip(
                                selected = selectedSubjectName.equals(sub.name, ignoreCase = true),
                                onClick = {
                                    selectedSubjectName = sub.name
                                    viewModel.setAiTopic(customTopicText, sub.name)
                                },
                                label = { Text(sub.name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customTopicText,
                        onValueChange = {
                            customTopicText = it
                            viewModel.setAiTopic(it, selectedSubjectName)
                        },
                        label = { Text("Topic to study (e.g. Normalization, Backpropagation)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.setAiTopic(customTopicText, selectedSubjectName)
                                when (activeTab) {
                                    "Explain" -> viewModel.requestAiExplanation()
                                    "Quiz" -> viewModel.requestAiQuiz()
                                    "Flashcards" -> viewModel.requestAiFlashcards()
                                    else -> viewModel.requestAiAdvice()
                                }
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Generate")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("ai_topic_search_field")
                    )
                }
            }
        }

        // --- Loading Indicator ---
        if (isLoading) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Consulting Gemini Academic Engine...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // --- Tab 1: Topic Explainer ---
            if (activeTab == "Explain" || activeTab == "Strategy") {
                item {
                    val displayContent = if (explanation.isNotBlank()) explanation else {
                        "Tap the generate icon above to get an exam-focused explanation of '$customTopicText' in '$selectedSubjectName'."
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (activeTab == "Strategy") "Strategic Study Plan" else "$selectedSubjectName — $customTopicText",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = {
                                        if (activeTab == "Strategy") viewModel.requestAiAdvice() else viewModel.requestAiExplanation()
                                    },
                                    modifier = Modifier.testTag("request_ai_btn")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (activeTab == "Strategy") "Refresh Advice" else "Explain")
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = displayContent,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
                            )
                        }
                    }
                }
            }

            // --- Tab 2: AI Quiz ---
            if (activeTab == "Quiz") {
                if (quizList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Ready to test your knowledge on $customTopicText?")
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.requestAiQuiz() }) {
                                    Icon(Icons.Default.Quiz, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate 3-Question Quiz")
                                }
                            }
                        }
                    }
                } else {
                    items(quizList.indices.toList()) { qIndex ->
                        val item = quizList[qIndex]
                        val userChoice = selectedOptionIndex[qIndex]

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Question ${qIndex + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))

                                item.options.forEachIndexed { optIndex, optionText ->
                                    val isSelected = userChoice == optIndex
                                    val isCorrect = item.correctIndex == optIndex
                                    val showResult = userChoice != null

                                    val bgColor = when {
                                        !showResult && isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        showResult && isCorrect -> Color(0xFF10B981).copy(alpha = 0.2f)
                                        showResult && isSelected && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }

                                    Surface(
                                        color = bgColor,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedOptionIndex = selectedOptionIndex + (qIndex to optIndex)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${('A' + optIndex)}. ",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = optionText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (showResult && isCorrect) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981))
                                            } else if (showResult && isSelected && !isCorrect) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }

                                if (userChoice != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "💡 Explanation: ${item.explanation}",
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                selectedOptionIndex = emptyMap()
                                viewModel.requestAiQuiz()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate New Quiz")
                        }
                    }
                }
            }

            // --- Tab 3: AI Flashcards ---
            if (activeTab == "Flashcards") {
                if (flashcards.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Generate interactive study flashcards for $customTopicText")
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { viewModel.requestAiFlashcards() }) {
                                    Icon(Icons.Default.Style, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate Flashcards")
                                }
                            }
                        }
                    }
                } else {
                    val card = flashcards.getOrNull(currentCardIndex)
                    if (card != null) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Card ${currentCardIndex + 1} of ${flashcards.size}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(8.dp))

                                // Flashcard surface
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clickable { isCardFlipped = !isCardFlipped }
                                        .testTag("flashcard_surface"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCardFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Surface(
                                                color = if (isCardFlipped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isCardFlipped) "ANSWER / BACK" else "QUESTION / FRONT",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = if (isCardFlipped) card.back else card.front,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = "Tap to flip",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (currentCardIndex > 0) {
                                                currentCardIndex--
                                                isCardFlipped = false
                                            }
                                        },
                                        enabled = currentCardIndex > 0
                                    ) {
                                        Text("Previous")
                                    }
                                    Button(
                                        onClick = {
                                            if (currentCardIndex < flashcards.size - 1) {
                                                currentCardIndex++
                                                isCardFlipped = false
                                            }
                                        },
                                        enabled = currentCardIndex < flashcards.size - 1
                                    ) {
                                        Text("Next Card")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
