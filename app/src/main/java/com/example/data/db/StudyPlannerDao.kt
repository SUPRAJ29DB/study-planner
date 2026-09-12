package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlannerDao {

    // --- Users ---
    @Query("SELECT * FROM users WHERE googleUid = :uid LIMIT 1")
    suspend fun getUserByGoogleUid(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    // --- Subjects (Scoped to userId) ---
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getAllSubjects(userId: Long): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    // --- Topics (Scoped to userId) ---
    @Query("SELECT * FROM topics WHERE userId = :userId ORDER BY id ASC")
    fun getAllTopics(userId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE userId = :userId AND subjectId = :subjectId ORDER BY id ASC")
    fun getTopicsForSubject(userId: Long, subjectId: Long): Flow<List<TopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE userId = :userId AND subjectId = :subjectId")
    suspend fun deleteTopicsForSubject(userId: Long, subjectId: Long)

    // --- Exams (Scoped to userId) ---
    @Query("SELECT * FROM exams WHERE userId = :userId ORDER BY examDateEpochMs ASC")
    fun getAllExams(userId: Long): Flow<List<ExamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Update
    suspend fun updateExam(exam: ExamEntity)

    @Delete
    suspend fun deleteExam(exam: ExamEntity)

    // --- Tasks (Scoped to userId) ---
    @Query("SELECT * FROM study_tasks WHERE userId = :userId ORDER BY isCompleted ASC, priorityScore DESC, id ASC")
    fun getAllTasks(userId: Long): Flow<List<StudyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Delete
    suspend fun deleteTask(task: StudyTaskEntity)

    @Query("DELETE FROM study_tasks WHERE userId = :userId")
    suspend fun clearAllTasks(userId: Long)

    // --- Sessions (Scoped to userId) ---
    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessions(userId: Long): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    // --- Flashcards (Scoped to userId) ---
    @Query("SELECT * FROM flashcards WHERE userId = :userId ORDER BY id ASC")
    fun getAllFlashcards(userId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(card: FlashcardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(card: FlashcardEntity)

    @Delete
    suspend fun deleteFlashcard(card: FlashcardEntity)

    // --- Reminders (Scoped to userId) ---
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY reminderDateEpochMs ASC, id ASC")
    fun getAllReminders(userId: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    // --- Notifications (Scoped to userId) ---
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isDismissed = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0 AND isDismissed = 0")
    fun getUnreadNotificationsCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Long)

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    // --- Notification Preferences ---
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getPreferencesForUser(userId: Long): NotificationPreferencesEntity?

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    fun getPreferencesFlow(userId: Long): Flow<NotificationPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreferences(pref: NotificationPreferencesEntity): Long

    // Count helpers
    @Query("SELECT COUNT(*) FROM subjects WHERE userId = :userId")
    suspend fun getSubjectCount(userId: Long): Int
}
