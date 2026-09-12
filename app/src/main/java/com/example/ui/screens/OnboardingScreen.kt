package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.StudyPlannerViewModel

@Composable
fun OnboardingScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    var course by remember { mutableStateOf("B.Tech") }
    var semester by remember { mutableStateOf("5") }
    var dailyHours by remember { mutableStateOf(3) }
    var preferredTime by remember { mutableStateOf("Evening") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .testTag("onboarding_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Welcome to Study Planner! 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Let's personalize your academic schedule",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()

                Text("Course / Degree:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("B.Tech", "B.Sc", "BCA", "Medical", "Other").forEach { c ->
                        FilterChip(
                            selected = course == c,
                            onClick = { course = c },
                            label = { Text(c) }
                        )
                    }
                }

                Text("Semester / Year:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8").forEach { sem ->
                        FilterChip(
                            selected = semester == sem,
                            onClick = { semester = sem },
                            label = { Text("Sem $sem") }
                        )
                    }
                }

                Text("Target Daily Study Time:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4, 5).forEach { h ->
                        FilterChip(
                            selected = dailyHours == h,
                            onClick = { dailyHours = h },
                            label = { Text("${h} hrs") }
                        )
                    }
                }

                Text("Preferred Study Time:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Morning", "Afternoon", "Evening", "Night").forEach { time ->
                        FilterChip(
                            selected = preferredTime == time,
                            onClick = { preferredTime = time },
                            label = { Text(time) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.completeOnboarding(course, semester, dailyHours, preferredTime)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_continue_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue to Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
