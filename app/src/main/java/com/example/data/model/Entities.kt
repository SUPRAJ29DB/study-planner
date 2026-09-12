package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val googleUid: String,
    val name: String,
    val email: String,
    val photoUrl: String = "",
    val course: String = "B.Tech",
    val semester: String = "5",
    val dailyStudyHours: Int = 3,
    val preferredTime: String = "Evening",
    val isOnboarded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val name: String,
    val description: String = "",
    val importance: String = "High", // Low, Medium, High
    val colorHex: Long = 0xFF4F46E5
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val subjectId: Long,
    val name: String,
    val description: String = "",
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val importance: String = "High", // Low, Medium, High
    val estimatedMinutes: Int = 45,
    val status: String = "Not Started", // Not Started, In Progress, Completed
    val isCompleted: Boolean = false
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val subjectId: Long = 0,
    val subjectName: String,
    val examName: String,
    val examDateEpochMs: Long,
    val importance: String = "High" // Low, Medium, High
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val topicId: Long = 0,
    val subjectId: Long = 0,
    val subjectName: String,
    val topicName: String,
    val durationMinutes: Int = 45,
    val isCompleted: Boolean = false,
    val plannedDate: String = "",
    val priorityScore: Double = 1.0
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val subjectName: String,
    val topicName: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val subjectName: String,
    val topicName: String,
    val front: String,
    val back: String,
    val isMastered: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val title: String,
    val description: String = "",
    val type: String = "Study", // Study, Exam, Assignment, Project, Revision, Custom
    val relatedSubject: String = "",
    val relatedTopic: String = "",
    val reminderDateEpochMs: Long,
    val reminderTimeFormatted: String = "07:00 PM",
    val repeatRule: String = "Does not repeat", // Does not repeat, Daily, Weekly, Monthly
    val notificationMinutesBefore: Int = 15,
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val reminderId: Long? = null,
    val title: String,
    val message: String,
    val type: String = "Study", // Study, Exam, Revision, MissedTask, System
    val actionRoute: String = "", // "planner", "exams", "subjects", "reminders"
    val relatedId: Long = 0,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1,
    val studyReminders: Boolean = true,
    val examReminders: Boolean = true,
    val revisionReminders: Boolean = true,
    val taskReminders: Boolean = true,
    val inAppNotifications: Boolean = true,
    val defaultMinutesBefore: Int = 15,
    val autoExamReminders: Boolean = true,
    val autoStudyReminders: Boolean = true,
    val autoRevisionReminders: Boolean = true
)
