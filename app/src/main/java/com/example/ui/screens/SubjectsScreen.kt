package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectEntity
import com.example.data.model.TopicEntity
import com.example.ui.NavDestination
import com.example.ui.StudyPlannerViewModel

@Composable
fun SubjectsScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsState()
    val topics by viewModel.topics.collectAsState()

    var expandedSubjectId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setAddSubjectDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_subject_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Subjects & Syllabus",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Manage curriculum, topics, and completion status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (subjects.isEmpty()) {
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
                            Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("No subjects yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Add your first subject to start planning.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.setAddSubjectDialog(true) }) {
                                Text("+ Add Subject")
                            }
                        }
                    }
                }
            } else {
                items(subjects, key = { it.id }) { subject ->
                    val subjectTopics = topics.filter { it.subjectId == subject.id }
                    val completedCount = subjectTopics.count { it.isCompleted }
                    val totalCount = subjectTopics.size
                    val isExpanded = expandedSubjectId == subject.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_card_${subject.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(subject.colorHex))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (subject.description.isNotBlank()) {
                                            Text(
                                                text = subject.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "$completedCount/$totalCount Topics",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            val frac = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(subject.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { expandedSubjectId = if (isExpanded) null else subject.id }
                                ) {
                                    Text(if (isExpanded) "Hide Syllabus ▲" else "View Syllabus ▼")
                                }

                                Row {
                                    IconButton(onClick = { viewModel.openAddTopicDialog(subject) }) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Topic")
                                    }
                                    IconButton(onClick = { viewModel.deleteSubject(subject) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Subject")
                                    }
                                }
                            }

                            // Expandable Topics List
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Divider()
                                    if (subjectTopics.isEmpty()) {
                                        Text(
                                            "No topics in syllabus yet. Tap + to add topics.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        subjectTopics.forEach { topic ->
                                            TopicRowItem(
                                                topic = topic,
                                                onToggle = { viewModel.toggleTopicCompletion(topic) },
                                                onExplainAi = {
                                                    viewModel.setAiTopic(topic.name, subject.name)
                                                    viewModel.requestAiExplanation()
                                                    viewModel.navigateTo(NavDestination.AI_ASSISTANT)
                                                },
                                                onDelete = { viewModel.deleteTopic(topic) }
                                            )
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
}

@Composable
fun TopicRowItem(
    topic: TopicEntity,
    onToggle: () -> Unit,
    onExplainAi: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = topic.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${topic.difficulty} • ${topic.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = topic.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (topic.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick AI Explain shortcut
            IconButton(
                onClick = onExplainAi,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Explain with AI",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
