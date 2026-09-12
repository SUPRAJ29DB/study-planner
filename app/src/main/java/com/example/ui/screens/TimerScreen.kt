package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import com.example.ui.StudyPlannerViewModel

@Composable
fun TimerScreen(
    viewModel: StudyPlannerViewModel,
    modifier: Modifier = Modifier
) {
    val subject by viewModel.timerSubject.collectAsState()
    val task by viewModel.timerTask.collectAsState()
    val totalSeconds by viewModel.totalTimerSeconds.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val isCompleted by viewModel.isTimerCompleted.collectAsState()

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val progress = if (totalSeconds > 0) {
        (totalSeconds - secondsRemaining).toFloat() / totalSeconds.toFloat()
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Header / Task info ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = subject,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = task,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Deep Focus Study Session",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Circular Display ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isRunning) "Focus Mode Active" else if (isCompleted) "Completed! 🎉" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Completion Banner ---
        AnimatedVisibility(visible = isCompleted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Study Session Completed ✓", fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                        Text("Duration: ${totalSeconds / 60} minutes logged to analytics.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                    }
                }
            }
        }

        // --- Control Buttons ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isRunning) {
                    Button(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("start_timer_btn"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (secondsRemaining < totalSeconds) "Resume" else "Start Session")
                    }
                } else {
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("pause_timer_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pause")
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.resetTimer() },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("reset_timer_btn"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
            }

            TextButton(
                onClick = { viewModel.finishSessionManually() },
                modifier = Modifier.testTag("finish_session_manually_btn")
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Finish Session & Log Progress")
            }
        }
    }
}
