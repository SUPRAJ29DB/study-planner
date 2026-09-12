package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.model.StudyTaskEntity
import com.example.ui.StudyPlannerViewModel

@Composable
fun PlannerScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()

    // Calculate timetable intervals (starting at 09:00)
    var currentHour = 9
    var currentMinute = 0

    fun formatTime(h: Int, m: Int): String {
        return "%02d:%02d".format(h, m)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Intelligent Study Schedule",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Balanced with optimal study blocks and 15-min rest intervals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.setPlanMyDayDialog(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("planner_plan_my_day_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoMode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Re-Plan My Day (Auto Calibrate)")
                    }
                }
            }
        }

        item {
            Text(
                "Today's Timetable",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (tasks.isEmpty()) {
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
                        Text("No schedule generated yet.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.setPlanMyDayDialog(true) }) {
                            Text("Click Plan My Day")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(tasks) { index, task ->
                val startH = currentHour
                val startM = currentMinute
                val totalStartMinutes = startH * 60 + startM
                val totalEndMinutes = totalStartMinutes + task.durationMinutes
                val endH = (totalEndMinutes / 60) % 24
                val endM = totalEndMinutes % 60

                // Update tracker for next slot (including 15m break if not last item)
                currentHour = ((totalEndMinutes + 15) / 60) % 24
                currentMinute = (totalEndMinutes + 15) % 60

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Task Card
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_slot_${task.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(90.dp)
                            ) {
                                Text(
                                    text = "${formatTime(startH, startM)} – ${formatTime(endH, endM)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${task.durationMinutes} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp)
                                    .padding(horizontal = 4.dp)
                            )
                            Spacer(Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.subjectName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = task.topicName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.selectTaskForTimer(
                                        subject = task.subjectName,
                                        task = task.topicName,
                                        durationMinutes = task.durationMinutes
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Break indicator
                    if (index < tasks.size - 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${formatTime(endH, endM)} – ${formatTime(currentHour, currentMinute)}  ☕ 15 min Break",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
