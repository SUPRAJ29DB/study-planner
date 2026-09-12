package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectEntity
import java.util.concurrent.TimeUnit

@Composable
fun PlanMyDayDialog(
    onDismiss: () -> Unit,
    onGenerate: (hours: Int, energy: String, goal: String, createReminders: Boolean, reminderMinutes: Int) -> Unit
) {
    var selectedHours by remember { mutableStateOf(3) }
    var selectedEnergy by remember { mutableStateOf("High") }
    var selectedGoal by remember { mutableStateOf("Exam Preparation") }
    var createReminders by remember { mutableStateOf(true) }
    var reminderMinutes by remember { mutableStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Plan My Day", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "How much time can you study today?",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4, 5).forEach { h ->
                        FilterChip(
                            selected = selectedHours == h,
                            onClick = { selectedHours = h },
                            label = { Text("${h}h") },
                            modifier = Modifier.testTag("plan_hours_${h}")
                        )
                    }
                }

                Divider()

                Text("Energy Level:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low", "Medium", "High").forEach { level ->
                        FilterChip(
                            selected = selectedEnergy == level,
                            onClick = { selectedEnergy = level },
                            label = { Text(level) },
                            modifier = Modifier.testTag("energy_${level}")
                        )
                    }
                }

                Divider()

                Text("Today's Primary Goal:", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Exam Preparation", "Learn New Topics", "Revision").forEach { goal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGoal = goal }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedGoal == goal,
                                onClick = { selectedGoal = goal }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(goal, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Divider()

                // Reminder options
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = createReminders,
                        onCheckedChange = { createReminders = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Remind me before each session", style = MaterialTheme.typography.bodyMedium)
                }

                if (createReminders) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = reminderMinutes == mins,
                                onClick = { reminderMinutes = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedHours, selectedEnergy, selectedGoal, createReminders, reminderMinutes) },
                modifier = Modifier.testTag("confirm_plan_day_btn")
            ) {
                Text("Generate Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddReminderDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onAdd: (
        title: String,
        desc: String,
        type: String,
        subject: String,
        topic: String,
        dateEpochMs: Long,
        timeFormatted: String,
        repeatRule: String,
        minutesBefore: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Study") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "DBMS") }
    var topicName by remember { mutableStateOf("") }
    var timeFormatted by remember { mutableStateOf("07:00 PM") }
    var selectedDayOffset by remember { mutableStateOf(0) } // 0 = Today, 1 = Tomorrow
    var repeatRule by remember { mutableStateOf("Does not repeat") }
    var minutesBefore by remember { mutableStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Add Reminder", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title (e.g. DBMS Normalization)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                )

                Text("Reminder Type:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Study", "Exam", "Revision", "Assignment", "Project", "Custom").take(4).forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }

                Text("Date:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Today", 1 to "Tomorrow", 3 to "In 3 days", 7 to "In 1 week").forEach { (offset, label) ->
                        FilterChip(
                            selected = selectedDayOffset == offset,
                            onClick = { selectedDayOffset = offset },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = timeFormatted,
                    onValueChange = { timeFormatted = it },
                    label = { Text("Time (e.g. 07:00 PM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reminder_time_input")
                )

                Text("Notify before:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { mins ->
                        FilterChip(
                            selected = minutesBefore == mins,
                            onClick = { minutesBefore = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }

                Text("Repeat:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Does not repeat", "Daily", "Weekly").forEach { r ->
                        FilterChip(
                            selected = repeatRule == r,
                            onClick = { repeatRule = r },
                            label = { Text(r) }
                        )
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (title.isNotBlank()) title.trim() else "$type Reminder"
                    val epochMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(selectedDayOffset.toLong())
                    onAdd(finalTitle, desc.trim(), type, selectedSubject, topicName.trim(), epochMs, timeFormatted, repeatRule, minutesBefore)
                },
                modifier = Modifier.testTag("save_reminder_btn")
            ) {
                Text("Save Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, desc: String, importance: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf("High") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. DBMS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().testTag("subject_desc_input")
                )
                Text("Importance:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { imp ->
                        FilterChip(
                            selected = importance == imp,
                            onClick = { importance = imp },
                            label = { Text(imp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), desc.trim(), importance) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_subject_btn")
            ) {
                Text("Save Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddTopicDialog(
    subjects: List<SubjectEntity>,
    initialSubject: SubjectEntity?,
    onDismiss: () -> Unit,
    onAdd: (subjectId: Long, name: String, desc: String, difficulty: String, importance: String, minutes: Int) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(initialSubject?.id ?: subjects.firstOrNull()?.id ?: 0L) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var importance by remember { mutableStateOf("High") }
    var minutesText by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Syllabus Topic", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Subject:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.take(4).forEach { sub ->
                        FilterChip(
                            selected = selectedSubjectId == sub.id,
                            onClick = { selectedSubjectId = sub.id },
                            label = { Text(sub.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Topic Name (e.g. Normalization)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("topic_name_input")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description / Key Points") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Difficulty:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff) }
                        )
                    }
                }

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text("Estimated Time (Minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesText.toIntOrNull() ?: 45
                    if (name.isNotBlank() && selectedSubjectId != 0L) {
                        onAdd(selectedSubjectId, name.trim(), desc.trim(), difficulty, importance, mins)
                    }
                },
                enabled = name.isNotBlank() && selectedSubjectId != 0L,
                modifier = Modifier.testTag("save_topic_btn")
            ) {
                Text("Save Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddExamDialog(
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onAdd: (subjectName: String, examName: String, daysRemaining: Int, importance: String) -> Unit
) {
    var selectedSubjectName by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "DBMS") }
    var examName by remember { mutableStateOf("") }
    var daysText by remember { mutableStateOf("7") }
    var importance by remember { mutableStateOf("High") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Upcoming Exam", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Subject:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subjects.take(3).forEach { sub ->
                        FilterChip(
                            selected = selectedSubjectName == sub.name,
                            onClick = { selectedSubjectName = sub.name },
                            label = { Text(sub.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text("Exam Name (e.g. Midterm / Final)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_name_input")
                )

                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Days Remaining (e.g. 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_days_input")
                )

                Text("Exam Importance:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High").forEach { imp ->
                        FilterChip(
                            selected = importance == imp,
                            onClick = { importance = imp },
                            label = { Text(imp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysText.toIntOrNull() ?: 5
                    val finalName = if (examName.isNotBlank()) examName.trim() else "$selectedSubjectName Exam"
                    onAdd(selectedSubjectName, finalName, days, importance)
                },
                modifier = Modifier.testTag("save_exam_btn")
            ) {
                Text("Add Exam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
