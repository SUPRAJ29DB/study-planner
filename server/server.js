import express from 'express';
import cors from 'cors';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';

// Load .env if present
dotenv.config();

import {
  initDatabase,
  getDatabaseStatus,
  getSubjects,
  saveSubject,
  deleteSubject,
  getTopics,
  saveTopic,
  updateTopicStatus,
  deleteTopic,
  getExams,
  saveExam,
  deleteExam,
  getTodayTasks,
  saveTodayPlan,
  toggleTask,
  getSessions,
  recordSession,
  getReminders,
  saveReminder,
  deleteReminder,
  getNotifications,
  addNotification,
  markNotificationsRead
} from './db.js';

import { authMiddleware } from './authMiddleware.js';
import {
  askGemini,
  explainTopicWithGemini,
  generateQuizWithGemini,
  optimizePlanWithGemini
} from './geminiService.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DIST_DIR = path.resolve(__dirname, '../dist');

const app = express();
const PORT = process.env.DEFAULT_APP_PORT || 3000;

app.use(cors());
app.use(express.json());

// Initialize Database connection on boot
await initDatabase();

// Log status on startup
console.log('--- Study Planner Server Status ---');
console.log('Database Status:', JSON.stringify(getDatabaseStatus()));
console.log('Gemini AI Key Configured:', !!process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'MY_GEMINI_API_KEY');
console.log('Port:', PORT);
console.log('-----------------------------------');

/* ------------------------------------------------------------------
   PUBLIC STATUS ENDPOINT
------------------------------------------------------------------ */
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    database: getDatabaseStatus(),
    geminiConfigured: !!process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'MY_GEMINI_API_KEY',
    nodeVersion: process.version,
    uptime: Math.floor(process.uptime()),
    timestamp: new Date().toISOString()
  });
});

/* ------------------------------------------------------------------
   PROTECTED API ROUTES (Scoped to req.user.id)
------------------------------------------------------------------ */
app.use('/api', authMiddleware);

// 1. Current Authenticated User Profile
app.get('/api/user/me', (req, res) => {
  res.json(req.user);
});

// 2. Subjects CRUD
app.get('/api/subjects', async (req, res) => {
  try {
    const subjects = await getSubjects(req.user.id);
    res.json(subjects);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/subjects', async (req, res) => {
  try {
    const subject = await saveSubject(req.user.id, req.body);
    res.json(subject);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/subjects/:id', async (req, res) => {
  try {
    await deleteSubject(req.user.id, req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 3. Topics CRUD
app.get('/api/topics', async (req, res) => {
  try {
    const topics = await getTopics(req.user.id);
    res.json(topics);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/topics', async (req, res) => {
  try {
    const topic = await saveTopic(req.user.id, req.body);
    res.json(topic);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.patch('/api/topics/:id/status', async (req, res) => {
  try {
    const updated = await updateTopicStatus(req.user.id, req.params.id, req.body.status);
    res.json(updated);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/topics/:id', async (req, res) => {
  try {
    await deleteTopic(req.user.id, req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 4. Exams CRUD
app.get('/api/exams', async (req, res) => {
  try {
    const exams = await getExams(req.user.id);
    res.json(exams);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/exams', async (req, res) => {
  try {
    const exam = await saveExam(req.user.id, req.body);
    res.json(exam);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/exams/:id', async (req, res) => {
  try {
    await deleteExam(req.user.id, req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 5. Today's Study Tasks & Plans
app.get('/api/plan/today', async (req, res) => {
  try {
    const tasks = await getTodayTasks(req.user.id);
    res.json(tasks);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/plan/today', async (req, res) => {
  try {
    const savedTasks = await saveTodayPlan(req.user.id, req.body.tasks || []);
    res.json(savedTasks);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/plan/tasks/:id/toggle', async (req, res) => {
  try {
    const task = await toggleTask(req.user.id, req.params.id);
    res.json(task);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 6. Study Sessions (Timer Logs)
app.get('/api/sessions', async (req, res) => {
  try {
    const sessions = await getSessions(req.user.id);
    res.json(sessions);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/sessions', async (req, res) => {
  try {
    const session = await recordSession(req.user.id, req.body);
    res.json(session);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 7. Reminders CRUD
app.get('/api/reminders', async (req, res) => {
  try {
    const reminders = await getReminders(req.user.id);
    res.json(reminders);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/reminders', async (req, res) => {
  try {
    const reminder = await saveReminder(req.user.id, req.body);
    res.json(reminder);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.delete('/api/reminders/:id', async (req, res) => {
  try {
    await deleteReminder(req.user.id, req.params.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 8. Notifications CRUD
app.get('/api/notifications', async (req, res) => {
  try {
    const notifications = await getNotifications(req.user.id);
    res.json(notifications);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/notifications/mark-read', async (req, res) => {
  try {
    await markNotificationsRead(req.user.id);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 9. Server-Side Gemini AI Endpoints
app.post('/api/ai/explain', async (req, res) => {
  try {
    const { subject, topic } = req.body;
    const explanation = await explainTopicWithGemini(subject || 'General', topic || 'Core Concepts');
    res.json({ text: explanation });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/ai/quiz', async (req, res) => {
  try {
    const { subject, topic } = req.body;
    const quiz = await generateQuizWithGemini(subject || 'General', topic || 'Core Concepts');
    res.json({ text: quiz });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/ai/chat', async (req, res) => {
  try {
    const { prompt } = req.body;
    const reply = await askGemini(prompt, "You are a helpful university study assistant.");
    res.json({ text: reply });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

/* ------------------------------------------------------------------
   STATIC ASSETS & SPA CLIENT SERVING
------------------------------------------------------------------ */
if (fs.existsSync(DIST_DIR)) {
  app.use(express.static(DIST_DIR));
  app.get('*', (req, res) => {
    res.sendFile(path.join(DIST_DIR, 'index.html'));
  });
} else {
  // If dist has not been built yet, serve a clear ready page
  app.get('*', (req, res) => {
    res.send(`
      <!DOCTYPE html>
      <html>
        <head><title>Study Planner Web Server</title></head>
        <body style="font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; background: #0E1524; color: #ECF0F6;">
          <div style="text-align: center; max-width: 500px; padding: 2rem; border: 1px solid #25314F; border-radius: 12px; background: #141D33;">
            <h2 style="margin-bottom: 0.5rem;">Study Planner Web Backend is Live</h2>
            <p style="color: #94A1BC;">Building frontend bundle. Refresh in a few moments.</p>
            <p style="font-size: 0.85rem; color: #7C97FF;">Status: API operational at /api/status</p>
          </div>
        </body>
      </html>
    `);
  });
}

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server listening on http://0.0.0.0:${PORT}`);
});
