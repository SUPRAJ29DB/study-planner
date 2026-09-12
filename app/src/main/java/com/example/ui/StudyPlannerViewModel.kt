package com.example.ui

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.*
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.StudyPlannerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class NavDestination(val label: String) {
    DASHBOARD("Dashboard"),
    SUBJECTS("Subjects"),
    EXAMS("Exams"),
    PLANNER("Planner"),
    TIMER("Timer"),
    PROGRESS("Progress"),
    REMINDERS("Reminders"),
    AI_ASSISTANT("AI Assistant"),
    SETTINGS("Settings"),
    LOGIN("Login"),
    ONBOARDING("Onboarding")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StudyPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyPlannerRepository
    private val geminiService = GeminiService()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = StudyPlannerRepository(db.studyPlannerDao())

        // Automatically initialize default profile for seamless demo/testing
        viewModelScope.launch {
            val user = repository.signInWithGoogle(
                googleUid = "google_user_939910611675",
                name = "Suprakash Ghosh",
                email = "suprakashg21@gmail.com",
                photoUrl = ""
            )
            _currentUser.value = user
            _currentNav.value = if (user.isOnboarded) NavDestination.DASHBOARD else NavDestination.ONBOARDING
            repository.evaluateSmartReminders(user.id)
        }
    }

    // --- Authentication State ---
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> = _currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loginWithGoogle(email: String = "suprakashg21@gmail.com", name: String = "Suprakash Ghosh", photoUrl: String = "") {
        viewModelScope.launch {
            val user = repository.signInWithGoogle(
                googleUid = "google_uid_${email.hashCode()}",
                name = name,
                email = email,
                photoUrl = photoUrl
            )
            _currentUser.value = user
            _currentNav.value = if (user.isOnboarded) NavDestination.DASHBOARD else NavDestination.ONBOARDING
            repository.evaluateSmartReminders(user.id)
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentNav.value = NavDestination.LOGIN
    }

    fun completeOnboarding(course: String, semester: String, dailyHours: Int, preferredTime: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.completeOnboarding(user.id, course, semester, dailyHours, preferredTime)
            val refreshed = repository.getUserById(user.id)
            _currentUser.value = refreshed
            _currentNav.value = NavDestination.DASHBOARD
        }
    }

    // Current active user ID flow
    private val currentUserIdFlow: Flow<Long> = _currentUser.map { it?.id ?: 1L }

    // --- Scoped State Streams ---
    val subjects: StateFlow<List<SubjectEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllSubjects(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<TopicEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllTopics(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllExams(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<StudyTaskEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllTasks(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySessionEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllSessions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllFlashcards(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllReminders(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getAllNotifications(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getUnreadNotificationsCount(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val preferences: StateFlow<NotificationPreferencesEntity?> = currentUserIdFlow.flatMapLatest { uid ->
        repository.getPreferencesFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Navigation ---
    private val _currentNav = MutableStateFlow(NavDestination.DASHBOARD)
    val currentNav: StateFlow<NavDestination> = _currentNav.asStateFlow()

    fun navigateTo(destination: NavDestination) {
        if (_currentUser.value == null && destination != NavDestination.LOGIN) {
            _currentNav.value = NavDestination.LOGIN
            return
        }
        _currentNav.value = destination
    }

    // --- Study Timer State ---
    private val _timerSubject = MutableStateFlow("DBMS")
    val timerSubject: StateFlow<String> = _timerSubject.asStateFlow()

    private val _timerTask = MutableStateFlow("Normalization")
    val timerTask: StateFlow<String> = _timerTask.asStateFlow()

    private val _totalTimerSeconds = MutableStateFlow(45 * 60)
    val totalTimerSeconds: StateFlow<Int> = _totalTimerSeconds.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(45 * 60)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isTimerCompleted = MutableStateFlow(false)
    val isTimerCompleted: StateFlow<Boolean> = _isTimerCompleted.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    fun selectTaskForTimer(subject: String, task: String, durationMinutes: Int) {
        countDownTimer?.cancel()
        _timerSubject.value = subject
        _timerTask.value = task
        val sec = if (durationMinutes > 0) durationMinutes * 60 else 45 * 60
        _totalTimerSeconds.value = sec
        _secondsRemaining.value = sec
        _isTimerRunning.value = false
        _isTimerCompleted.value = false
        _currentNav.value = NavDestination.TIMER
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true
        _isTimerCompleted.value = false

        countDownTimer = object : CountDownTimer((_secondsRemaining.value * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _secondsRemaining.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _isTimerRunning.value = false
                _isTimerCompleted.value = true
                _secondsRemaining.value = 0
                recordCompletedSession()
            }
        }.start()
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
        _secondsRemaining.value = _totalTimerSeconds.value
        _isTimerCompleted.value = false
    }

    fun finishSessionManually() {
        countDownTimer?.cancel()
        _isTimerRunning.value = false
        _isTimerCompleted.value = true
        recordCompletedSession()
    }

    private fun recordCompletedSession() {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val elapsedSec = _totalTimerSeconds.value - _secondsRemaining.value
            val elapsedMinutes = maxOf(1, elapsedSec / 60)
            repository.insertSession(
                StudySessionEntity(
                    userId = uid,
                    subjectName = _timerSubject.value,
                    topicName = _timerTask.value,
                    durationMinutes = elapsedMinutes
                )
            )

            // Mark matching study task as completed
            val currentTasks = tasks.value
            val match = currentTasks.find { it.topicName == _timerTask.value && !it.isCompleted }
            if (match != null) {
                repository.updateTask(match.copy(isCompleted = true))
            }
        }
    }

    // --- Plan My Day Dialog & Generation ---
    private val _showPlanMyDayDialog = MutableStateFlow(false)
    val showPlanMyDayDialog: StateFlow<Boolean> = _showPlanMyDayDialog.asStateFlow()

    fun setPlanMyDayDialog(visible: Boolean) {
        _showPlanMyDayDialog.value = visible
    }

    fun generatePlan(availableHours: Int, energyLevel: String, goal: String, createReminders: Boolean = true, reminderMinutes: Int = 15) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.generateAutomaticPlan(uid, availableHours, energyLevel, goal, createReminders, reminderMinutes)
            _showPlanMyDayDialog.value = false
            _currentNav.value = NavDestination.PLANNER
        }
    }

    // --- Reminders Management ---
    private val _showAddReminderDialog = MutableStateFlow(false)
    val showAddReminderDialog: StateFlow<Boolean> = _showAddReminderDialog.asStateFlow()

    fun setAddReminderDialog(visible: Boolean) {
        _showAddReminderDialog.value = visible
    }

    fun addReminder(
        title: String,
        desc: String,
        type: String,
        subject: String,
        topic: String,
        dateEpochMs: Long,
        timeFormatted: String,
        repeatRule: String,
        minutesBefore: Int
    ) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.insertReminder(
                ReminderEntity(
                    userId = uid,
                    title = title,
                    description = desc,
                    type = type,
                    relatedSubject = subject,
                    relatedTopic = topic,
                    reminderDateEpochMs = dateEpochMs,
                    reminderTimeFormatted = timeFormatted,
                    repeatRule = repeatRule,
                    notificationMinutesBefore = minutesBefore,
                    isActive = true
                )
            )
            _showAddReminderDialog.value = false
        }
    }

    fun toggleReminderActive(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- Notifications Sheet & Management ---
    private val _showNotificationsSheet = MutableStateFlow(false)
    val showNotificationsSheet: StateFlow<Boolean> = _showNotificationsSheet.asStateFlow()

    fun setNotificationsSheet(visible: Boolean) {
        _showNotificationsSheet.value = visible
    }

    fun markAllNotificationsAsRead() {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(uid)
        }
    }

    fun markNotificationAsRead(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notification)
        }
    }

    fun dismissNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.dismissNotification(notification)
        }
    }

    fun handleNotificationClick(notification: NotificationEntity) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notification)
            _showNotificationsSheet.value = false
            when (notification.actionRoute) {
                "planner" -> _currentNav.value = NavDestination.PLANNER
                "exams" -> _currentNav.value = NavDestination.EXAMS
                "subjects" -> _currentNav.value = NavDestination.SUBJECTS
                "timer" -> _currentNav.value = NavDestination.TIMER
                "reminders" -> _currentNav.value = NavDestination.REMINDERS
                else -> _currentNav.value = NavDestination.DASHBOARD
            }
        }
    }

    // --- Settings Preferences ---
    fun updatePreferences(
        studyReminders: Boolean,
        examReminders: Boolean,
        revisionReminders: Boolean,
        taskReminders: Boolean,
        inAppNotifications: Boolean,
        defaultMinutesBefore: Int,
        autoExamReminders: Boolean,
        autoStudyReminders: Boolean,
        autoRevisionReminders: Boolean
    ) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val pref = NotificationPreferencesEntity(
                userId = uid,
                studyReminders = studyReminders,
                examReminders = examReminders,
                revisionReminders = revisionReminders,
                taskReminders = taskReminders,
                inAppNotifications = inAppNotifications,
                defaultMinutesBefore = defaultMinutesBefore,
                autoExamReminders = autoExamReminders,
                autoStudyReminders = autoStudyReminders,
                autoRevisionReminders = autoRevisionReminders
            )
            repository.updatePreferences(pref)
        }
    }

    // --- Subject & Topic Management ---
    private val _showAddSubjectDialog = MutableStateFlow(false)
    val showAddSubjectDialog: StateFlow<Boolean> = _showAddSubjectDialog.asStateFlow()

    fun setAddSubjectDialog(visible: Boolean) {
        _showAddSubjectDialog.value = visible
    }

    fun addSubject(name: String, description: String, importance: String) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val color = when (importance) {
                "High" -> 0xFF4F46E5
                "Medium" -> 0xFF06B6D4
                else -> 0xFF10B981
            }
            repository.insertSubject(
                SubjectEntity(userId = uid, name = name, description = description, importance = importance, colorHex = color)
            )
            _showAddSubjectDialog.value = false
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    private val _showAddTopicDialog = MutableStateFlow(false)
    val showAddTopicDialog: StateFlow<Boolean> = _showAddTopicDialog.asStateFlow()

    private val _selectedSubjectForTopic = MutableStateFlow<SubjectEntity?>(null)
    val selectedSubjectForTopic: StateFlow<SubjectEntity?> = _selectedSubjectForTopic.asStateFlow()

    fun openAddTopicDialog(subject: SubjectEntity? = null) {
        _selectedSubjectForTopic.value = subject ?: subjects.value.firstOrNull()
        _showAddTopicDialog.value = true
    }

    fun closeAddTopicDialog() {
        _showAddTopicDialog.value = false
    }

    fun addTopic(subjectId: Long, name: String, description: String, difficulty: String, importance: String, minutes: Int) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            repository.insertTopic(
                TopicEntity(
                    userId = uid,
                    subjectId = subjectId,
                    name = name,
                    description = description,
                    difficulty = difficulty,
                    importance = importance,
                    estimatedMinutes = minutes,
                    status = "Not Started",
                    isCompleted = false
                )
            )
            _showAddTopicDialog.value = false
        }
    }

    fun toggleTopicCompletion(topic: TopicEntity) {
        viewModelScope.launch {
            val updated = topic.copy(
                isCompleted = !topic.isCompleted,
                status = if (!topic.isCompleted) "Completed" else "In Progress"
            )
            repository.updateTopic(updated)
        }
    }

    fun deleteTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
        }
    }

    // --- Exam Management ---
    private val _showAddExamDialog = MutableStateFlow(false)
    val showAddExamDialog: StateFlow<Boolean> = _showAddExamDialog.asStateFlow()

    fun setAddExamDialog(visible: Boolean) {
        _showAddExamDialog.value = visible
    }

    fun addExam(subjectName: String, examName: String, daysFromNow: Int, importance: String) {
        val uid = _currentUser.value?.id ?: return
        viewModelScope.launch {
            val dateMs = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysFromNow.toLong())
            val matchingSub = subjects.value.find { it.name.equals(subjectName, ignoreCase = true) }
            repository.insertExam(
                ExamEntity(
                    userId = uid,
                    subjectId = matchingSub?.id ?: 0,
                    subjectName = subjectName,
                    examName = examName,
                    examDateEpochMs = dateMs,
                    importance = importance
                )
            )
            _showAddExamDialog.value = false
        }
    }

    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }

    // --- Tasks ---
    fun toggleTaskCompletion(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    // --- AI Assistant ---
    private val _aiTopicInput = MutableStateFlow("Normalization")
    val aiTopicInput: StateFlow<String> = _aiTopicInput.asStateFlow()

    private val _aiSubjectInput = MutableStateFlow("DBMS")
    val aiSubjectInput: StateFlow<String> = _aiSubjectInput.asStateFlow()

    private val _aiExplanation = MutableStateFlow("")
    val aiExplanation: StateFlow<String> = _aiExplanation.asStateFlow()

    private val _aiQuiz = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val aiQuiz: StateFlow<List<QuizQuestion>> = _aiQuiz.asStateFlow()

    private val _aiFlashcards = MutableStateFlow<List<GeneratedFlashcard>>(emptyList())
    val aiFlashcards: StateFlow<List<GeneratedFlashcard>> = _aiFlashcards.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiActiveTab = MutableStateFlow("Explain")
    val aiActiveTab: StateFlow<String> = _aiActiveTab.asStateFlow()

    fun setAiActiveTab(tab: String) {
        _aiActiveTab.value = tab
    }

    fun setAiTopic(topic: String, subject: String) {
        _aiTopicInput.value = topic
        _aiSubjectInput.value = subject
    }

    fun requestAiExplanation() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val res = geminiService.explainTopic(_aiTopicInput.value, _aiSubjectInput.value)
            _aiExplanation.value = res
            _isAiLoading.value = false
        }
    }

    fun requestAiQuiz() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val res = geminiService.generateQuiz(_aiTopicInput.value, _aiSubjectInput.value)
            _aiQuiz.value = res
            _isAiLoading.value = false
        }
    }

    fun requestAiFlashcards() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val res = geminiService.generateFlashcards(_aiTopicInput.value, _aiSubjectInput.value)
            _aiFlashcards.value = res
            _isAiLoading.value = false
        }
    }

    fun requestAiAdvice() {
        viewModelScope.launch {
            _isAiLoading.value = true
            val subs = subjects.value.map { it.name }
            val examList = exams.value.map { "${it.subjectName} (${getDaysRemaining(it.examDateEpochMs)}d)" }
            val advice = geminiService.generateAiStudyAdvice(3, subs, examList)
            _aiExplanation.value = advice
            _isAiLoading.value = false
        }
    }

    fun getDaysRemaining(epochMs: Long): Long {
        val diff = epochMs - System.currentTimeMillis()
        return maxOf(0L, diff / (1000 * 60 * 60 * 24))
    }
}
