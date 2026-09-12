package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NavDestination
import com.example.ui.StudyPlannerViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StudyPlannerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerApp(
    viewModel: StudyPlannerViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentNav by viewModel.currentNav.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()

    val showPlanMyDay by viewModel.showPlanMyDayDialog.collectAsState()
    val showAddSubject by viewModel.showAddSubjectDialog.collectAsState()
    val showAddTopic by viewModel.showAddTopicDialog.collectAsState()
    val showAddExam by viewModel.showAddExamDialog.collectAsState()
    val showAddReminder by viewModel.showAddReminderDialog.collectAsState()
    val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsState()

    val subjects by viewModel.subjects.collectAsState()
    val selectedSubjectForTopic by viewModel.selectedSubjectForTopic.collectAsState()

    // 1. Guard: Login Screen if not authenticated
    if (currentUser == null || currentNav == NavDestination.LOGIN) {
        LoginScreen(viewModel = viewModel)
        return
    }

    // 2. Guard: Onboarding Screen if profile not yet configured
    if (!currentUser!!.isOnboarded || currentNav == NavDestination.ONBOARDING) {
        OnboardingScreen(viewModel = viewModel)
        return
    }

    // 3. Main Authenticated Application View
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Study Planner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = currentNav.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick Plan Day button
                    FilledTonalButton(
                        onClick = { viewModel.setPlanMyDayDialog(true) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("top_bar_plan_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Plan")
                    }

                    // Notification Bell with Unread Badge (Part 9)
                    IconButton(
                        onClick = { viewModel.setNotificationsSheet(true) },
                        modifier = Modifier.testTag("notification_bell_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Profile Initials Avatar (Part 4)
                    val initials = currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull() }?.take(2)?.joinToString("") ?: "U"
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.navigateTo(NavDestination.SETTINGS) }
                            .testTag("top_bar_profile_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentNav == NavDestination.DASHBOARD,
                    onClick = { viewModel.navigateTo(NavDestination.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.PLANNER,
                    onClick = { viewModel.navigateTo(NavDestination.PLANNER) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Planner") },
                    label = { Text("Planner") },
                    modifier = Modifier.testTag("nav_planner")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.REMINDERS,
                    onClick = { viewModel.navigateTo(NavDestination.REMINDERS) },
                    icon = { Icon(Icons.Default.Alarm, contentDescription = "Reminders") },
                    label = { Text("Reminders") },
                    modifier = Modifier.testTag("nav_reminders")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.SUBJECTS,
                    onClick = { viewModel.navigateTo(NavDestination.SUBJECTS) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Subjects") },
                    label = { Text("Syllabus") },
                    modifier = Modifier.testTag("nav_subjects")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.EXAMS,
                    onClick = { viewModel.navigateTo(NavDestination.EXAMS) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Exams") },
                    label = { Text("Exams") },
                    modifier = Modifier.testTag("nav_exams")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.TIMER,
                    onClick = { viewModel.navigateTo(NavDestination.TIMER) },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Timer") },
                    label = { Text("Timer") },
                    modifier = Modifier.testTag("nav_timer")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.PROGRESS,
                    onClick = { viewModel.navigateTo(NavDestination.PROGRESS) },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Progress") },
                    label = { Text("Stats") },
                    modifier = Modifier.testTag("nav_progress")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.AI_ASSISTANT,
                    onClick = { viewModel.navigateTo(NavDestination.AI_ASSISTANT) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant") },
                    label = { Text("AI") },
                    modifier = Modifier.testTag("nav_ai")
                )
                NavigationBarItem(
                    selected = currentNav == NavDestination.SETTINGS,
                    onClick = { viewModel.navigateTo(NavDestination.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNav) {
                NavDestination.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                NavDestination.SUBJECTS -> SubjectsScreen(viewModel = viewModel)
                NavDestination.EXAMS -> ExamsScreen(viewModel = viewModel)
                NavDestination.PLANNER -> PlannerScreen(viewModel = viewModel)
                NavDestination.TIMER -> TimerScreen(viewModel = viewModel)
                NavDestination.PROGRESS -> ProgressScreen(viewModel = viewModel)
                NavDestination.REMINDERS -> RemindersScreen(viewModel = viewModel)
                NavDestination.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel)
                NavDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                NavDestination.LOGIN -> LoginScreen(viewModel = viewModel)
                NavDestination.ONBOARDING -> OnboardingScreen(viewModel = viewModel)
            }
        }

        // --- Dialogs ---
        if (showPlanMyDay) {
            PlanMyDayDialog(
                onDismiss = { viewModel.setPlanMyDayDialog(false) },
                onGenerate = { hours, energy, goal, createReminders, reminderMinutes ->
                    viewModel.generatePlan(hours, energy, goal, createReminders, reminderMinutes)
                }
            )
        }

        if (showAddReminder) {
            AddReminderDialog(
                subjects = subjects,
                onDismiss = { viewModel.setAddReminderDialog(false) },
                onAdd = { title, desc, type, subject, topic, dateEpochMs, timeFormatted, repeatRule, minutesBefore ->
                    viewModel.addReminder(title, desc, type, subject, topic, dateEpochMs, timeFormatted, repeatRule, minutesBefore)
                }
            )
        }

        if (showNotificationsSheet) {
            NotificationCenterDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.setNotificationsSheet(false) }
            )
        }

        if (showAddSubject) {
            AddSubjectDialog(
                onDismiss = { viewModel.setAddSubjectDialog(false) },
                onAdd = { name, desc, importance ->
                    viewModel.addSubject(name, desc, importance)
                }
            )
        }

        if (showAddTopic) {
            AddTopicDialog(
                subjects = subjects,
                initialSubject = selectedSubjectForTopic,
                onDismiss = { viewModel.closeAddTopicDialog() },
                onAdd = { subjectId, name, desc, difficulty, importance, mins ->
                    viewModel.addTopic(subjectId, name, desc, difficulty, importance, mins)
                }
            )
        }

        if (showAddExam) {
            AddExamDialog(
                subjects = subjects,
                onDismiss = { viewModel.setAddExamDialog(false) },
                onAdd = { subjectName, examName, daysRemaining, importance ->
                    viewModel.addExam(subjectName, examName, daysRemaining, importance)
                }
            )
        }
    }
}
