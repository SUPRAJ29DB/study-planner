package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Study Planner", appName)
    }

    @Test
    fun `user data isolation test`() = runBlocking {
        val dao = db.studyPlannerDao()

        // Create User 1 and User 2
        val u1Id = dao.insertUser(UserEntity(googleUid = "u1", name = "Student One", email = "student1@test.com"))
        val u2Id = dao.insertUser(UserEntity(googleUid = "u2", name = "Student Two", email = "student2@test.com"))

        // Add subjects for User 1
        dao.insertSubject(SubjectEntity(userId = u1Id, name = "DBMS"))
        // Add subjects for User 2
        dao.insertSubject(SubjectEntity(userId = u2Id, name = "Organic Chemistry"))

        val u1Subs = dao.getAllSubjects(u1Id).first()
        val u2Subs = dao.getAllSubjects(u2Id).first()

        assertEquals(1, u1Subs.size)
        assertEquals("DBMS", u1Subs[0].name)

        assertEquals(1, u2Subs.size)
        assertEquals("Organic Chemistry", u2Subs[0].name)
    }

    @Test
    fun `reminders and notifications test`() = runBlocking {
        val dao = db.studyPlannerDao()
        val uid = dao.insertUser(UserEntity(googleUid = "u1", name = "Student", email = "student@test.com"))

        // Insert reminder
        val remId = dao.insertReminder(
            ReminderEntity(
                userId = uid,
                title = "DBMS — Normalization",
                type = "Study",
                reminderDateEpochMs = System.currentTimeMillis(),
                reminderTimeFormatted = "07:00 PM"
            )
        )
        assertTrue(remId > 0)

        // Insert notification
        val notifId = dao.insertNotification(
            NotificationEntity(
                userId = uid,
                title = "Upcoming Exam",
                message = "DBMS Midterm in 5 days",
                type = "Exam",
                isRead = false
            )
        )
        assertTrue(notifId > 0)

        val unreadCount = dao.getUnreadNotificationsCount(uid).first()
        assertEquals(1, unreadCount)

        dao.markAllNotificationsAsRead(uid)
        val afterCount = dao.getUnreadNotificationsCount(uid).first()
        assertEquals(0, afterCount)
    }
}
