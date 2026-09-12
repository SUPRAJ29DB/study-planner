package com.example.data.repository

import com.example.data.db.StudyPlannerDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class StudyPlannerRepository(private val dao: StudyPlannerDao) {

    // --- User & Auth ---
    suspend fun getUserByGoogleUid(googleUid: String): UserEntity? {
        return dao.getUserByGoogleUid(googleUid)
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return dao.getUserById(userId)
    }

    suspend fun signInWithGoogle(
        googleUid: String,
        name: String,
        email: String,
        photoUrl: String
    ): UserEntity {
        val existing = dao.getUserByGoogleUid(googleUid)
        if (existing != null) {
            val updated = existing.copy(
                name = if (name.isNotBlank()) name else existing.name,
                email = if (email.isNotBlank()) email else existing.email,
                photoUrl = if (photoUrl.isNotBlank()) photoUrl else existing.photoUrl,
                lastLogin = System.currentTimeMillis()
            )
            dao.updateUser(updated)
            return updated
        } else {
            val newUser = UserEntity(
                googleUid = googleUid,
                name = name,
                email = email,
                photoUrl = photoUrl,
                isOnboarded = false,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            val newId = dao.insertUser(newUser)
            val created = newUser.copy(id = newId)

            // Seed initial preferences & default academic curriculum for this user
            dao.insertOrUpdatePreferences(
                NotificationPreferencesEntity(userId = newId)
            )
            seedSampleDataForUser(newId)
            return created
        }
    }

    suspend fun completeOnboarding(
        userId: Long,
        course: String,
        semester: String,
        dailyHours: Int,
        preferredTime: String
    ) {
        val user = dao.getUserById(userId) ?: return
        dao.updateUser(
            user.copy(
                course = course,
                semester = semester,
                dailyStudyHours = dailyHours,
                preferredTime = preferredTime,
                isOnboarded = true
            )
        )
    }

    // --- Subjects & Topics (Scoped to userId) ---
    fun getAllSubjects(userId: Long): Flow<List<SubjectEntity>> = dao.getAllSubjects(userId)
    fun getAllTopics(userId: Long): Flow<List<TopicEntity>> = dao.getAllTopics(userId)
    fun getAllExams(userId: Long): Flow<List<ExamEntity>> = dao.getAllExams(userId)
    fun getAllTasks(userId: Long): Flow<List<StudyTaskEntity>> = dao.getAllTasks(userId)
    fun getAllSessions(userId: Long): Flow<List<StudySessionEntity>> = dao.getAllSessions(userId)
    fun getAllFlashcards(userId: Long): Flow<List<FlashcardEntity>> = dao.getAllFlashcards(userId)

    // --- Reminders & Notifications (Scoped to userId) ---
    fun getAllReminders(userId: Long): Flow<List<ReminderEntity>> = dao.getAllReminders(userId)
    fun getAllNotifications(userId: Long): Flow<List<NotificationEntity>> = dao.getAllNotifications(userId)
    fun getUnreadNotificationsCount(userId: Long): Flow<Int> = dao.getUnreadNotificationsCount(userId)
    fun getPreferencesFlow(userId: Long): Flow<NotificationPreferencesEntity?> = dao.getPreferencesFlow(userId)

    suspend fun insertReminder(reminder: ReminderEntity): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = dao.deleteReminder(reminder)

    suspend fun insertNotification(notification: NotificationEntity): Long = dao.insertNotification(notification)
    suspend fun markNotificationAsRead(notification: NotificationEntity) {
        dao.updateNotification(notification.copy(isRead = true))
    }
    suspend fun dismissNotification(notification: NotificationEntity) {
        dao.updateNotification(notification.copy(isDismissed = true))
    }
    suspend fun markAllNotificationsAsRead(userId: Long) = dao.markAllNotificationsAsRead(userId)

    suspend fun updatePreferences(pref: NotificationPreferencesEntity) {
        dao.insertOrUpdatePreferences(pref)
    }

    suspend fun checkAndSeedInitialData(userId: Long) {
        if (dao.getSubjectCount(userId) == 0) {
            seedSampleDataForUser(userId)
        }
    }

    suspend fun seedSampleDataForUser(userId: Long) {
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        // 1. Subjects
        val dbmsId = dao.insertSubject(
            SubjectEntity(
                userId = userId,
                name = "DBMS",
                description = "Database Management Systems & Relational Design",
                importance = "High",
                colorHex = 0xFF4F46E5
            )
        )
        val aiId = dao.insertSubject(
            SubjectEntity(
                userId = userId,
                name = "Artificial Intelligence",
                description = "Neural Networks, Deep Learning & Search Algorithms",
                importance = "High",
                colorHex = 0xFF06B6D4
            )
        )
        val qcId = dao.insertSubject(
            SubjectEntity(
                userId = userId,
                name = "Quantum Computing",
                description = "Qubits, Quantum Gates & Quantum Circuits",
                importance = "Medium",
                colorHex = 0xFF8B5CF6
            )
        )
        val mathId = dao.insertSubject(
            SubjectEntity(
                userId = userId,
                name = "Mathematics",
                description = "Linear Algebra, Calculus & Discrete Structures",
                importance = "Medium",
                colorHex = 0xFF10B981
            )
        )

        // 2. Topics
        dao.insertTopics(
            listOf(
                TopicEntity(userId = userId, subjectId = dbmsId, name = "ER Model", description = "Entity Relationship Diagrams", difficulty = "Easy", importance = "Medium", estimatedMinutes = 30, status = "Completed", isCompleted = true),
                TopicEntity(userId = userId, subjectId = dbmsId, name = "Relational Algebra", description = "Selection, Projection, Joins", difficulty = "Medium", importance = "High", estimatedMinutes = 45, status = "Completed", isCompleted = true),
                TopicEntity(userId = userId, subjectId = dbmsId, name = "Normalization", description = "1NF, 2NF, 3NF, BCNF Decomposition", difficulty = "Hard", importance = "High", estimatedMinutes = 60, status = "In Progress", isCompleted = false),
                TopicEntity(userId = userId, subjectId = dbmsId, name = "Transactions & ACID", description = "Concurrency control and isolation levels", difficulty = "Hard", importance = "High", estimatedMinutes = 45, status = "Not Started", isCompleted = false),
                TopicEntity(userId = userId, subjectId = dbmsId, name = "Indexing & B-Trees", description = "B+ Trees and Clustered indexing", difficulty = "Medium", importance = "Medium", estimatedMinutes = 45, status = "Not Started", isCompleted = false),

                TopicEntity(userId = userId, subjectId = aiId, name = "ANN & Perceptrons", description = "Feedforward networks and perceptrons", difficulty = "Medium", importance = "High", estimatedMinutes = 45, status = "Completed", isCompleted = true),
                TopicEntity(userId = userId, subjectId = aiId, name = "Backpropagation", description = "Gradient descent and chain rule weight updates", difficulty = "Hard", importance = "High", estimatedMinutes = 60, status = "In Progress", isCompleted = false),
                TopicEntity(userId = userId, subjectId = aiId, name = "Activation Functions", description = "ReLU, Sigmoid, Softmax, GELU", difficulty = "Easy", importance = "Medium", estimatedMinutes = 30, status = "Not Started", isCompleted = false),
                TopicEntity(userId = userId, subjectId = aiId, name = "CNN & Pooling", description = "Convolutional layers, feature maps and strides", difficulty = "Hard", importance = "High", estimatedMinutes = 60, status = "Not Started", isCompleted = false),

                TopicEntity(userId = userId, subjectId = qcId, name = "Qubits & Superposition", description = "State vectors and Bloch sphere representation", difficulty = "Medium", importance = "High", estimatedMinutes = 45, status = "Completed", isCompleted = true),
                TopicEntity(userId = userId, subjectId = qcId, name = "Hadamard Gate", description = "Creating uniform quantum superposition", difficulty = "Easy", importance = "High", estimatedMinutes = 30, status = "In Progress", isCompleted = false),
                TopicEntity(userId = userId, subjectId = qcId, name = "Pauli Gates (X, Y, Z)", description = "Bit-flip and phase-flip single qubit gates", difficulty = "Medium", importance = "Medium", estimatedMinutes = 30, status = "Not Started", isCompleted = false),
                TopicEntity(userId = userId, subjectId = qcId, name = "CNOT Gate & Entanglement", description = "Two-qubit controlled operations and Bell states", difficulty = "Hard", importance = "High", estimatedMinutes = 45, status = "Not Started", isCompleted = false),

                TopicEntity(userId = userId, subjectId = mathId, name = "Eigenvalues & Eigenvectors", description = "Matrix diagonalization and transformations", difficulty = "Hard", importance = "High", estimatedMinutes = 60, status = "In Progress", isCompleted = false),
                TopicEntity(userId = userId, subjectId = mathId, name = "Probability Distributions", description = "Gaussian, Poisson and Bayes' Theorem", difficulty = "Medium", importance = "Medium", estimatedMinutes = 45, status = "Not Started", isCompleted = false)
            )
        )

        // 3. Exams
        dao.insertExam(
            ExamEntity(
                userId = userId,
                subjectId = dbmsId,
                subjectName = "DBMS",
                examName = "DBMS Midterm Examination",
                examDateEpochMs = now + (5 * oneDay),
                importance = "High"
            )
        )
        dao.insertExam(
            ExamEntity(
                userId = userId,
                subjectId = aiId,
                subjectName = "Artificial Intelligence",
                examName = "AI & Deep Learning Final",
                examDateEpochMs = now + (12 * oneDay),
                importance = "High"
            )
        )
        dao.insertExam(
            ExamEntity(
                userId = userId,
                subjectId = qcId,
                subjectName = "Quantum Computing",
                examName = "Quantum Systems Theory Exam",
                examDateEpochMs = now + (20 * oneDay),
                importance = "Medium"
            )
        )

        // 4. Initial Study Tasks
        dao.insertTasks(
            listOf(
                StudyTaskEntity(userId = userId, subjectName = "DBMS", topicName = "Normalization", durationMinutes = 45, isCompleted = true, priorityScore = 95.0),
                StudyTaskEntity(userId = userId, subjectName = "Artificial Intelligence", topicName = "Neural Networks", durationMinutes = 60, isCompleted = true, priorityScore = 90.0),
                StudyTaskEntity(userId = userId, subjectName = "Quantum Computing", topicName = "Hadamard Gate", durationMinutes = 30, isCompleted = false, priorityScore = 75.0),
                StudyTaskEntity(userId = userId, subjectName = "DBMS", topicName = "Revision & Practice Questions", durationMinutes = 30, isCompleted = false, priorityScore = 70.0)
            )
        )

        // 5. Initial Sessions
        dao.insertSession(StudySessionEntity(userId = userId, subjectName = "DBMS", topicName = "ER Model", durationMinutes = 45, timestamp = now - (2 * oneDay)))
        dao.insertSession(StudySessionEntity(userId = userId, subjectName = "Artificial Intelligence", topicName = "ANN & Perceptrons", durationMinutes = 60, timestamp = now - oneDay))
        dao.insertSession(StudySessionEntity(userId = userId, subjectName = "DBMS", topicName = "Relational Algebra", durationMinutes = 45, timestamp = now - (3 * 3600 * 1000)))
        dao.insertSession(StudySessionEntity(userId = userId, subjectName = "Quantum Computing", topicName = "Qubits & Superposition", durationMinutes = 30, timestamp = now - (1 * 3600 * 1000)))

        // 6. Flashcards
        dao.insertFlashcards(
            listOf(
                FlashcardEntity(userId = userId, subjectName = "DBMS", topicName = "Normalization", front = "What is the primary condition for 2nd Normal Form (2NF)?", back = "It must be in 1NF and have NO partial dependency (all non-prime attributes must depend fully on the primary key)."),
                FlashcardEntity(userId = userId, subjectName = "DBMS", topicName = "Transactions", front = "What does ACID stand for in Database Systems?", back = "Atomicity, Consistency, Isolation, and Durability."),
                FlashcardEntity(userId = userId, subjectName = "Artificial Intelligence", topicName = "Backpropagation", front = "What is the purpose of Backpropagation in Deep Learning?", back = "To calculate gradients of the loss function with respect to weights using the chain rule, enabling gradient descent optimization."),
                FlashcardEntity(userId = userId, subjectName = "Quantum Computing", topicName = "Hadamard Gate", front = "What transformation does a Hadamard gate perform on |0⟩?", back = "It creates an equal superposition: (|0⟩ + |1⟩) / √2.")
            )
        )

        // 7. Initial Reminders
        dao.insertReminder(
            ReminderEntity(
                userId = userId,
                title = "DBMS — Normalization",
                description = "Focus on 3NF and BCNF problems",
                type = "Study",
                relatedSubject = "DBMS",
                relatedTopic = "Normalization",
                reminderDateEpochMs = now,
                reminderTimeFormatted = "07:00 PM",
                notificationMinutesBefore = 15
            )
        )
        dao.insertReminder(
            ReminderEntity(
                userId = userId,
                title = "Revise Backpropagation",
                description = "Go over gradient descent equations",
                type = "Revision",
                relatedSubject = "Artificial Intelligence",
                relatedTopic = "Backpropagation",
                reminderDateEpochMs = now + oneDay,
                reminderTimeFormatted = "06:00 PM",
                notificationMinutesBefore = 30
            )
        )
        dao.insertReminder(
            ReminderEntity(
                userId = userId,
                title = "DBMS Midterm Examination",
                description = "Hall B, Bring scientific calculator",
                type = "Exam",
                relatedSubject = "DBMS",
                reminderDateEpochMs = now + (5 * oneDay),
                reminderTimeFormatted = "09:00 AM",
                notificationMinutesBefore = 60
            )
        )

        // 8. Initial Notifications
        dao.insertNotifications(
            listOf(
                NotificationEntity(
                    userId = userId,
                    title = "DBMS Study Session",
                    message = "Normalization focus block starts in 15 minutes",
                    type = "Study",
                    actionRoute = "timer",
                    isRead = false,
                    createdAt = now - (15 * 60 * 1000)
                ),
                NotificationEntity(
                    userId = userId,
                    title = "Revision Reminder",
                    message = "Backpropagation is due today for high retention",
                    type = "Revision",
                    actionRoute = "subjects",
                    isRead = false,
                    createdAt = now - (60 * 60 * 1000)
                ),
                NotificationEntity(
                    userId = userId,
                    title = "Upcoming Exam Alert",
                    message = "DBMS Midterm Examination in 5 days. Ensure topics are reviewed.",
                    type = "Exam",
                    actionRoute = "exams",
                    isRead = false,
                    createdAt = now - (3 * 3600 * 1000)
                )
            )
        )
    }

    // CRUD operations
    suspend fun insertSubject(subject: SubjectEntity) = dao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = dao.updateSubject(subject)
    suspend fun deleteSubject(subject: SubjectEntity) {
        dao.deleteTopicsForSubject(subject.userId, subject.id)
        dao.deleteSubject(subject)
    }

    suspend fun insertTopic(topic: TopicEntity) = dao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = dao.updateTopic(topic)
    suspend fun deleteTopic(topic: TopicEntity) = dao.deleteTopic(topic)

    suspend fun insertExam(exam: ExamEntity) = dao.insertExam(exam)
    suspend fun updateExam(exam: ExamEntity) = dao.updateExam(exam)
    suspend fun deleteExam(exam: ExamEntity) = dao.deleteExam(exam)

    suspend fun insertTask(task: StudyTaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: StudyTaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(task: StudyTaskEntity) = dao.deleteTask(task)

    suspend fun insertSession(session: StudySessionEntity) = dao.insertSession(session)

    suspend fun insertFlashcard(flashcard: FlashcardEntity) = dao.insertFlashcard(flashcard)
    suspend fun updateFlashcard(flashcard: FlashcardEntity) = dao.updateFlashcard(flashcard)
    suspend fun deleteFlashcard(flashcard: FlashcardEntity) = dao.deleteFlashcard(flashcard)

    // Plan My Day intelligent rule-based scheduler
    suspend fun generateAutomaticPlan(
        userId: Long,
        availableHours: Int,
        energyLevel: String,
        goal: String,
        createReminders: Boolean = true,
        reminderMinutesBefore: Int = 15
    ) {
        val topics = dao.getAllTopics(userId).first()
        val exams = dao.getAllExams(userId).first()
        val subjects = dao.getAllSubjects(userId).first().associateBy { it.id }

        val now = System.currentTimeMillis()
        val totalAvailableMinutes = availableHours * 60

        val subjectUrgencyMap = mutableMapOf<String, Double>()
        for (exam in exams) {
            val diffMs = exam.examDateEpochMs - now
            val daysRemaining = maxOf(0.0, diffMs.toDouble() / (1000.0 * 60 * 60 * 24))
            val urgency = 100.0 / (daysRemaining + 1.0)
            val currentMax = subjectUrgencyMap[exam.subjectName] ?: 0.0
            subjectUrgencyMap[exam.subjectName] = maxOf(currentMax, urgency)
        }

        data class ScoredTopic(val topic: TopicEntity, val subjectName: String, val score: Double)

        val scoredList = topics.map { topic ->
            val subject = subjects[topic.subjectId]
            val subName = subject?.name ?: "General"
            val examUrgency = subjectUrgencyMap[subName] ?: 10.0

            val subjectWeight = when (subject?.importance) {
                "High" -> 3.0
                "Medium" -> 2.0
                else -> 1.0
            }

            val difficultyWeight = when (topic.difficulty) {
                "Hard" -> 3.0
                "Medium" -> 2.0
                else -> 1.0
            }

            val topicImportanceWeight = when (topic.importance) {
                "High" -> 3.0
                "Medium" -> 2.0
                else -> 1.0
            }

            val workWeight = when {
                goal == "Revision" && topic.isCompleted -> 3.5
                goal == "Learn New Topics" && !topic.isCompleted && topic.status == "Not Started" -> 3.5
                !topic.isCompleted && topic.status == "In Progress" -> 3.0
                !topic.isCompleted -> 2.5
                else -> 1.0
            }

            val energyMultiplier = when (energyLevel) {
                "High" -> if (topic.difficulty == "Hard") 1.4 else 1.0
                "Low" -> if (topic.difficulty == "Easy") 1.5 else 0.7
                else -> 1.0
            }

            val totalScore = examUrgency * subjectWeight * difficultyWeight * topicImportanceWeight * workWeight * energyMultiplier
            ScoredTopic(topic, subName, totalScore)
        }.sortedByDescending { it.score }

        var accumulatedMinutes = 0
        val selectedTasks = mutableListOf<StudyTaskEntity>()

        for (scored in scoredList) {
            val duration = if (scored.topic.estimatedMinutes > 0) scored.topic.estimatedMinutes else 45
            if (accumulatedMinutes + duration <= totalAvailableMinutes) {
                accumulatedMinutes += duration
                selectedTasks.add(
                    StudyTaskEntity(
                        userId = userId,
                        topicId = scored.topic.id,
                        subjectId = scored.topic.subjectId,
                        subjectName = scored.subjectName,
                        topicName = scored.topic.name,
                        durationMinutes = duration,
                        isCompleted = false,
                        priorityScore = scored.score
                    )
                )
            }
            if (accumulatedMinutes >= totalAvailableMinutes) break
        }

        if (totalAvailableMinutes - accumulatedMinutes >= 20) {
            val remaining = totalAvailableMinutes - accumulatedMinutes
            selectedTasks.add(
                StudyTaskEntity(
                    userId = userId,
                    subjectName = "Review & Practice",
                    topicName = "Flashcards & Quick Recall",
                    durationMinutes = remaining,
                    isCompleted = false,
                    priorityScore = 50.0
                )
            )
        }

        dao.clearAllTasks(userId)
        dao.insertTasks(selectedTasks)

        // Optionally create reminders for newly generated plan
        if (createReminders && selectedTasks.isNotEmpty()) {
            for (task in selectedTasks.take(3)) {
                dao.insertReminder(
                    ReminderEntity(
                        userId = userId,
                        title = "${task.subjectName} — ${task.topicName}",
                        description = "Scheduled ${task.durationMinutes} min study block",
                        type = "Study",
                        relatedSubject = task.subjectName,
                        relatedTopic = task.topicName,
                        reminderDateEpochMs = now,
                        reminderTimeFormatted = "Today",
                        notificationMinutesBefore = reminderMinutesBefore
                    )
                )
            }
        }
    }

    // Smart automatic reminders check
    suspend fun evaluateSmartReminders(userId: Long) {
        val prefs = dao.getPreferencesForUser(userId) ?: return
        val now = System.currentTimeMillis()
        val oneDay = TimeUnit.DAYS.toMillis(1)

        // 1. Exam countdown reminders
        if (prefs.autoExamReminders && prefs.examReminders) {
            val exams = dao.getAllExams(userId).first()
            for (exam in exams) {
                val diffMs = exam.examDateEpochMs - now
                val daysRemaining = diffMs / oneDay
                if (daysRemaining in 1..7) {
                    // Check if already notified in last 24h
                    dao.insertNotification(
                        NotificationEntity(
                            userId = userId,
                            title = "Upcoming Exam: ${exam.subjectName}",
                            message = "${exam.examName} is in $daysRemaining days.",
                            type = "Exam",
                            actionRoute = "exams",
                            relatedId = exam.id,
                            isRead = false,
                            createdAt = now
                        )
                    )
                }
            }
        }

        // 2. Missed task check
        if (prefs.taskReminders) {
            val tasks = dao.getAllTasks(userId).first()
            val incomplete = tasks.filter { !it.isCompleted }
            if (incomplete.isNotEmpty()) {
                val missed = incomplete.first()
                dao.insertNotification(
                    NotificationEntity(
                        userId = userId,
                        title = "⚠️ Missed Study Task",
                        message = "You haven't completed: ${missed.subjectName} — ${missed.topicName}. Reschedule it today?",
                        type = "MissedTask",
                        actionRoute = "planner",
                        relatedId = missed.id,
                        isRead = false,
                        createdAt = now
                    )
                )
            }
        }
    }
}
