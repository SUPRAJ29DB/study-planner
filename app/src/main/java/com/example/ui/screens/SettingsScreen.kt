package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.StudyPlannerViewModel

@Composable
fun SettingsScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val preferences by viewModel.preferences.collectAsState()

    var studyReminders by remember(preferences) { mutableStateOf(preferences?.studyReminders ?: true) }
    var examReminders by remember(preferences) { mutableStateOf(preferences?.examReminders ?: true) }
    var revisionReminders by remember(preferences) { mutableStateOf(preferences?.revisionReminders ?: true) }
    var taskReminders by remember(preferences) { mutableStateOf(preferences?.taskReminders ?: true) }
    var inAppNotifs by remember(preferences) { mutableStateOf(preferences?.inAppNotifications ?: true) }
    var defaultMinutes by remember(preferences) { mutableStateOf(preferences?.defaultMinutesBefore ?: 15) }
    var autoExam by remember(preferences) { mutableStateOf(preferences?.autoExamReminders ?: true) }
    var autoStudy by remember(preferences) { mutableStateOf(preferences?.autoStudyReminders ?: true) }
    var autoRevision by remember(preferences) { mutableStateOf(preferences?.autoRevisionReminders ?: true) }

    fun syncPrefs() {
        viewModel.updatePreferences(
            studyReminders, examReminders, revisionReminders, taskReminders,
            inAppNotifs, defaultMinutes, autoExam, autoStudy, autoRevision
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. User Profile Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Initials Avatar
                        val initials = currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.take(2)?.joinToString("") ?: "U"
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.name?.uppercase() ?: "STUDENT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = currentUser?.email ?: "student@gmail.com",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            if (currentUser?.course?.isNotBlank() == true) {
                                Text(
                                    text = "${currentUser?.course} • Sem ${currentUser?.semester}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("logout_btn")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Logout")
                        }
                    }
                }
            }
        }

        // --- 2. Notification Preferences Section ---
        item {
            Text(
                "Notification Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "Study reminders",
                        subtitle = "Alerts before scheduled study blocks",
                        checked = studyReminders,
                        onCheckedChange = { studyReminders = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "Exam reminders",
                        subtitle = "Countdown notifications for upcoming exams",
                        checked = examReminders,
                        onCheckedChange = { examReminders = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "Revision reminders",
                        subtitle = "Prompts to revise weak topics and flashcards",
                        checked = revisionReminders,
                        onCheckedChange = { revisionReminders = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "Task reminders",
                        subtitle = "Notifications when daily study tasks are missed",
                        checked = taskReminders,
                        onCheckedChange = { taskReminders = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "In-App & Push notifications",
                        subtitle = "Show notification banners and bell updates",
                        checked = inAppNotifs,
                        onCheckedChange = { inAppNotifs = it; syncPrefs() }
                    )
                }
            }
        }

        // --- 3. Default Reminder Interval ---
        item {
            Text(
                "Default Reminder Timing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Remind me before each session:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = defaultMinutes == mins,
                                onClick = {
                                    defaultMinutes = mins
                                    syncPrefs()
                                },
                                label = { Text(if (mins == 60) "1 hr before" else "${mins}m before") }
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Automatic Smart Reminders ---
        item {
            Text(
                "Smart Automatic Reminders",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "Automatic Exam Reminders",
                        subtitle = "Calibrate alerts at 7 days, 3 days, and exam eve",
                        checked = autoExam,
                        onCheckedChange = { autoExam = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "Automatic Study Reminders",
                        subtitle = "Auto-schedule reminder when 'Plan My Day' runs",
                        checked = autoStudy,
                        onCheckedChange = { autoStudy = it; syncPrefs() }
                    )
                    Divider()
                    SettingToggleRow(
                        title = "Automatic Revision Reminders",
                        subtitle = "Spaced repetition alerts for completed topics",
                        checked = autoRevision,
                        onCheckedChange = { autoRevision = it; syncPrefs() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
