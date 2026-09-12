package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.ReminderEntity
import com.example.ui.StudyPlannerViewModel
import java.util.concurrent.TimeUnit

@Composable
fun RemindersScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by viewModel.reminders.collectAsState()

    val now = System.currentTimeMillis()
    val oneDay = TimeUnit.DAYS.toMillis(1)

    val todayReminders = reminders.filter {
        val diff = it.reminderDateEpochMs - now
        diff in 0..oneDay || it.reminderTimeFormatted.contains("Today", ignoreCase = true)
    }
    val tomorrowReminders = reminders.filter {
        val diff = it.reminderDateEpochMs - now
        diff in (oneDay + 1)..(2 * oneDay) || it.reminderTimeFormatted.contains("Tomorrow", ignoreCase = true)
    }
    val upcomingReminders = reminders.filter {
        val diff = it.reminderDateEpochMs - now
        diff > (2 * oneDay) && !it.reminderTimeFormatted.contains("Today", ignoreCase = true) && !it.reminderTimeFormatted.contains("Tomorrow", ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setAddReminderDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_reminder_fab")
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = "Add Reminder")
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
                            "Study Reminders",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Timely alerts for sessions, revision, exams, and projects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.setAddReminderDialog(true) },
                        modifier = Modifier.testTag("add_reminder_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            if (reminders.isEmpty()) {
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
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("No reminders scheduled.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Set study, exam, or revision alerts so you never miss a session.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.setAddReminderDialog(true) }) {
                                Text("+ Add Reminder")
                            }
                        }
                    }
                }
            } else {
                // Section: Today
                if (todayReminders.isNotEmpty()) {
                    item {
                        ReminderSectionHeader(title = "Today", icon = Icons.Default.Today)
                    }
                    items(todayReminders, key = { it.id }) { rem ->
                        ReminderCardItem(
                            reminder = rem,
                            onToggleActive = { viewModel.toggleReminderActive(rem) },
                            onDelete = { viewModel.deleteReminder(rem) }
                        )
                    }
                }

                // Section: Tomorrow
                if (tomorrowReminders.isNotEmpty()) {
                    item {
                        ReminderSectionHeader(title = "Tomorrow", icon = Icons.Default.CalendarToday)
                    }
                    items(tomorrowReminders, key = { it.id }) { rem ->
                        ReminderCardItem(
                            reminder = rem,
                            onToggleActive = { viewModel.toggleReminderActive(rem) },
                            onDelete = { viewModel.deleteReminder(rem) }
                        )
                    }
                }

                // Section: Upcoming
                if (upcomingReminders.isNotEmpty()) {
                    item {
                        ReminderSectionHeader(title = "Upcoming", icon = Icons.Default.Event)
                    }
                    items(upcomingReminders, key = { it.id }) { rem ->
                        ReminderCardItem(
                            reminder = rem,
                            onToggleActive = { viewModel.toggleReminderActive(rem) },
                            onDelete = { viewModel.deleteReminder(rem) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ReminderCardItem(
    reminder: ReminderEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (reminder.type) {
        "Exam" -> Color(0xFFEF4444)
        "Revision" -> Color(0xFF8B5CF6)
        "Assignment" -> Color(0xFFF59E0B)
        "Project" -> Color(0xFF06B6D4)
        else -> Color(0xFF4F46E5)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (reminder.isActive) typeColor else Color.Gray)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = typeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = reminder.type,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = typeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⏰ ${reminder.reminderTimeFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• ${reminder.notificationMinutesBefore}m before",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reminder.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.testTag("switch_reminder_${reminder.id}")
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete Reminder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
