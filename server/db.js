import pg from 'pg';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DATA_DIR = path.resolve(__dirname, '../data');
const BACKUP_FILE = path.join(DATA_DIR, 'study_planner_store.json');

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

let pool = null;
let isPostgres = false;
let dbStatus = {
  connected: false,
  persistent: true,
  type: 'Local File Store',
  message: 'DATABASE_URL not detected in secrets. Using local persistent store.'
};

// In-memory / file-backed fallback store
let fallbackStore = {
  users: {},
  subjects: [],
  topics: [],
  exams: [],
  study_tasks: [],
  study_sessions: [],
  reminders: [],
  notifications: []
};

// Load initial store if exists
if (fs.existsSync(BACKUP_FILE)) {
  try {
    const raw = fs.readFileSync(BACKUP_FILE, 'utf8');
    fallbackStore = { ...fallbackStore, ...JSON.parse(raw) };
  } catch (err) {
    console.error('Error loading fallback store:', err.message);
  }
}

function persistFallback() {
  try {
    fs.writeFileSync(BACKUP_FILE, JSON.stringify(fallbackStore, null, 2), 'utf8');
  } catch (err) {
    console.error('Error saving fallback store:', err.message);
  }
}

export async function initDatabase() {
  const dbUrl = process.env.DATABASE_URL;

  if (dbUrl && dbUrl.startsWith('postgres')) {
    try {
      console.log('Connecting to PostgreSQL via DATABASE_URL...');
      pool = new pg.Pool({
        connectionString: dbUrl,
        ssl: dbUrl.includes('localhost') ? false : { rejectUnauthorized: false },
        max: 10,
        idleTimeoutMillis: 30000,
        connectionTimeoutMillis: 5000
      });

      // Test connection
      const client = await pool.connect();
      try {
        const schemaPath = path.join(__dirname, 'schema.sql');
        if (fs.existsSync(schemaPath)) {
          const sql = fs.readFileSync(schemaPath, 'utf8');
          await client.query(sql);
          console.log('PostgreSQL schema initialized successfully.');
        }
        isPostgres = true;
        dbStatus = {
          connected: true,
          persistent: true,
          type: 'PostgreSQL Live',
          message: 'Connected to remote PostgreSQL instance. Production-ready.'
        };
      } finally {
        client.release();
      }
    } catch (err) {
      console.warn('PostgreSQL connection attempt failed:', err.message);
      console.warn('Falling back to local persistent store so app runs seamlessly.');
      dbStatus = {
        connected: false,
        persistent: true,
        type: 'Local File Store',
        message: `PostgreSQL connection error: ${err.message}. Running on persistent local storage.`
      };
      isPostgres = false;
    }
  } else {
    console.log('No DATABASE_URL configured. Running on persistent local storage.');
    isPostgres = false;
  }
  return dbStatus;
}

export function getDatabaseStatus() {
  return dbStatus;
}

/* -------------------------------------------------------------
   USER OPERATIONS
------------------------------------------------------------- */
export async function getOrCreateUser({ id, email, name, photoUrl }) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO users (id, email, name, photo_url, last_login)
       VALUES ($1, $2, $3, $4, CURRENT_TIMESTAMP)
       ON CONFLICT (id) DO UPDATE
       SET last_login = CURRENT_TIMESTAMP, name = EXCLUDED.name, email = EXCLUDED.email
       RETURNING *`,
      [id, email, name, photoUrl || null]
    );
    return res.rows[0];
  } else {
    if (!fallbackStore.users[id]) {
      fallbackStore.users[id] = {
        id,
        email,
        name,
        photo_url: photoUrl || null,
        created_at: new Date().toISOString(),
        last_login: new Date().toISOString()
      };
      // Seed default sample data for new user if empty
      seedDefaultDataForUser(id);
      persistFallback();
    } else {
      fallbackStore.users[id].last_login = new Date().toISOString();
      persistFallback();
    }
    return fallbackStore.users[id];
  }
}

/* -------------------------------------------------------------
   SUBJECTS CRUD (User Scoped)
------------------------------------------------------------- */
export async function getSubjects(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM subjects WHERE user_id = $1 ORDER BY created_at ASC', [userId]);
    return res.rows;
  }
  return fallbackStore.subjects.filter(s => s.user_id === userId);
}

export async function saveSubject(userId, subject) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO subjects (id, user_id, name, description, importance, color)
       VALUES ($1, $2, $3, $4, $5, $6)
       ON CONFLICT (id) DO UPDATE
       SET name = EXCLUDED.name, description = EXCLUDED.description,
           importance = EXCLUDED.importance, color = EXCLUDED.color
       RETURNING *`,
      [subject.id, userId, subject.name, subject.description, subject.importance, subject.color]
    );
    return res.rows[0];
  }
  const idx = fallbackStore.subjects.findIndex(s => s.id === subject.id && s.user_id === userId);
  const record = { ...subject, user_id: userId };
  if (idx >= 0) fallbackStore.subjects[idx] = record;
  else fallbackStore.subjects.push(record);
  persistFallback();
  return record;
}

export async function deleteSubject(userId, subjectId) {
  if (isPostgres && pool) {
    await pool.query('DELETE FROM subjects WHERE id = $1 AND user_id = $2', [subjectId, userId]);
    return true;
  }
  fallbackStore.subjects = fallbackStore.subjects.filter(s => !(s.id === subjectId && s.user_id === userId));
  fallbackStore.topics = fallbackStore.topics.filter(t => !(t.subject_id === subjectId && t.user_id === userId));
  fallbackStore.exams = fallbackStore.exams.filter(e => !(e.subject_id === subjectId && e.user_id === userId));
  persistFallback();
  return true;
}

/* -------------------------------------------------------------
   TOPICS CRUD (User Scoped)
------------------------------------------------------------- */
export async function getTopics(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM topics WHERE user_id = $1 ORDER BY created_at ASC', [userId]);
    return res.rows;
  }
  return fallbackStore.topics.filter(t => t.user_id === userId);
}

export async function saveTopic(userId, topic) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO topics (id, user_id, subject_id, name, description, difficulty, importance, est_minutes, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       ON CONFLICT (id) DO UPDATE
       SET name = EXCLUDED.name, description = EXCLUDED.description,
           difficulty = EXCLUDED.difficulty, importance = EXCLUDED.importance,
           est_minutes = EXCLUDED.est_minutes, status = EXCLUDED.status
       RETURNING *`,
      [topic.id, userId, topic.subjectId || topic.subject_id, topic.name, topic.description, topic.difficulty, topic.importance, topic.estMinutes || topic.est_minutes || 30, topic.status]
    );
    return res.rows[0];
  }
  const idx = fallbackStore.topics.findIndex(t => t.id === topic.id && t.user_id === userId);
  const record = {
    ...topic,
    user_id: userId,
    subject_id: topic.subjectId || topic.subject_id,
    est_minutes: topic.estMinutes || topic.est_minutes || 30
  };
  if (idx >= 0) fallbackStore.topics[idx] = record;
  else fallbackStore.topics.push(record);
  persistFallback();
  return record;
}

export async function updateTopicStatus(userId, topicId, status) {
  if (isPostgres && pool) {
    const res = await pool.query(
      'UPDATE topics SET status = $1 WHERE id = $2 AND user_id = $3 RETURNING *',
      [status, topicId, userId]
    );
    return res.rows[0];
  }
  const t = fallbackStore.topics.find(item => item.id === topicId && item.user_id === userId);
  if (t) {
    t.status = status;
    persistFallback();
  }
  return t;
}

export async function deleteTopic(userId, topicId) {
  if (isPostgres && pool) {
    await pool.query('DELETE FROM topics WHERE id = $1 AND user_id = $2', [topicId, userId]);
    return true;
  }
  fallbackStore.topics = fallbackStore.topics.filter(t => !(t.id === topicId && t.user_id === userId));
  persistFallback();
  return true;
}

/* -------------------------------------------------------------
   EXAMS CRUD (User Scoped)
------------------------------------------------------------- */
export async function getExams(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM exams WHERE user_id = $1 ORDER BY exam_date ASC', [userId]);
    return res.rows;
  }
  return fallbackStore.exams.filter(e => e.user_id === userId);
}

export async function saveExam(userId, exam) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO exams (id, user_id, subject_id, name, exam_date, importance)
       VALUES ($1, $2, $3, $4, $5, $6)
       ON CONFLICT (id) DO UPDATE
       SET name = EXCLUDED.name, subject_id = EXCLUDED.subject_id,
           exam_date = EXCLUDED.exam_date, importance = EXCLUDED.importance
       RETURNING *`,
      [exam.id, userId, exam.subjectId || exam.subject_id, exam.name, exam.date || exam.exam_date, exam.importance]
    );
    return res.rows[0];
  }
  const idx = fallbackStore.exams.findIndex(e => e.id === exam.id && e.user_id === userId);
  const record = {
    ...exam,
    user_id: userId,
    subject_id: exam.subjectId || exam.subject_id,
    exam_date: exam.date || exam.exam_date
  };
  if (idx >= 0) fallbackStore.exams[idx] = record;
  else fallbackStore.exams.push(record);
  persistFallback();
  return record;
}

export async function deleteExam(userId, examId) {
  if (isPostgres && pool) {
    await pool.query('DELETE FROM exams WHERE id = $1 AND user_id = $2', [examId, userId]);
    return true;
  }
  fallbackStore.exams = fallbackStore.exams.filter(e => !(e.id === examId && e.user_id === userId));
  persistFallback();
  return true;
}

/* -------------------------------------------------------------
   TODAY PLAN & TASKS (User Scoped)
------------------------------------------------------------- */
export async function getTodayTasks(userId) {
  if (isPostgres && pool) {
    const res = await pool.query(
      'SELECT * FROM study_tasks WHERE user_id = $1 ORDER BY start_time_minutes ASC',
      [userId]
    );
    return res.rows;
  }
  return fallbackStore.study_tasks.filter(t => t.user_id === userId);
}

export async function saveTodayPlan(userId, tasks) {
  if (isPostgres && pool) {
    await pool.query('DELETE FROM study_tasks WHERE user_id = $1', [userId]);
    for (const t of tasks) {
      await pool.query(
        `INSERT INTO study_tasks (id, user_id, subject_name, topic_name, duration_minutes, start_time_minutes, is_break, is_completed, color)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
        [t.id, userId, t.subject || null, t.topic || null, t.duration, t.start, !!t.isBreak, !!t.completed, t.color || null]
      );
    }
    return getTodayTasks(userId);
  }
  fallbackStore.study_tasks = fallbackStore.study_tasks.filter(t => t.user_id !== userId);
  for (const t of tasks) {
    fallbackStore.study_tasks.push({
      id: t.id,
      user_id: userId,
      subject_name: t.subject,
      topic_name: t.topic,
      duration_minutes: t.duration,
      start_time_minutes: t.start,
      is_break: !!t.isBreak,
      is_completed: !!t.completed,
      color: t.color
    });
  }
  persistFallback();
  return fallbackStore.study_tasks.filter(t => t.user_id === userId);
}

export async function toggleTask(userId, taskId) {
  if (isPostgres && pool) {
    const res = await pool.query(
      'UPDATE study_tasks SET is_completed = NOT is_completed WHERE id = $1 AND user_id = $2 RETURNING *',
      [taskId, userId]
    );
    return res.rows[0];
  }
  const task = fallbackStore.study_tasks.find(t => t.id === taskId && t.user_id === userId);
  if (task) {
    task.is_completed = !task.is_completed;
    persistFallback();
  }
  return task;
}

/* -------------------------------------------------------------
   STUDY SESSIONS (User Scoped)
------------------------------------------------------------- */
export async function getSessions(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM study_sessions WHERE user_id = $1 ORDER BY created_at DESC', [userId]);
    return res.rows;
  }
  return fallbackStore.study_sessions.filter(s => s.user_id === userId);
}

export async function recordSession(userId, session) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO study_sessions (id, user_id, subject_name, topic_name, duration_minutes, session_date)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [session.id, userId, session.subject, session.topicName || session.topic, session.minutes, session.date || new Date().toISOString().slice(0, 10)]
    );
    return res.rows[0];
  }
  const record = {
    id: session.id,
    user_id: userId,
    subject_name: session.subject,
    topic_name: session.topicName || session.topic,
    duration_minutes: session.minutes,
    session_date: session.date || new Date().toISOString().slice(0, 10),
    created_at: new Date().toISOString()
  };
  fallbackStore.study_sessions.push(record);
  persistFallback();
  return record;
}

/* -------------------------------------------------------------
   REMINDERS & NOTIFICATIONS (User Scoped)
------------------------------------------------------------- */
export async function getReminders(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM reminders WHERE user_id = $1 ORDER BY created_at DESC', [userId]);
    return res.rows;
  }
  return fallbackStore.reminders.filter(r => r.user_id === userId);
}

export async function saveReminder(userId, reminder) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO reminders (id, user_id, title, description, type, subject_name, topic_name, reminder_time, reminder_date, lead_minutes, repeat_rule, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       ON CONFLICT (id) DO UPDATE
       SET title = EXCLUDED.title, description = EXCLUDED.description,
           reminder_time = EXCLUDED.reminder_time, is_active = EXCLUDED.is_active
       RETURNING *`,
      [
        reminder.id, userId, reminder.title, reminder.description || null, reminder.type || 'Study',
        reminder.subjectName || reminder.subject_name || null, reminder.topicName || reminder.topic_name || null,
        reminder.timeFormatted || reminder.reminder_time, reminder.date || reminder.reminder_date || new Date().toISOString().slice(0, 10),
        reminder.leadMinutes || reminder.lead_minutes || 15, reminder.repeatRule || reminder.repeat_rule || 'Does not repeat',
        reminder.isActive !== undefined ? reminder.isActive : true
      ]
    );
    return res.rows[0];
  }
  const idx = fallbackStore.reminders.findIndex(r => r.id === reminder.id && r.user_id === userId);
  const record = {
    ...reminder,
    user_id: userId,
    reminder_time: reminder.timeFormatted || reminder.reminder_time,
    reminder_date: reminder.date || reminder.reminder_date || new Date().toISOString().slice(0, 10),
    is_active: reminder.isActive !== undefined ? reminder.isActive : true
  };
  if (idx >= 0) fallbackStore.reminders[idx] = record;
  else fallbackStore.reminders.push(record);
  persistFallback();
  return record;
}

export async function deleteReminder(userId, reminderId) {
  if (isPostgres && pool) {
    await pool.query('DELETE FROM reminders WHERE id = $1 AND user_id = $2', [reminderId, userId]);
    return true;
  }
  fallbackStore.reminders = fallbackStore.reminders.filter(r => !(r.id === reminderId && r.user_id === userId));
  persistFallback();
  return true;
}

export async function getNotifications(userId) {
  if (isPostgres && pool) {
    const res = await pool.query('SELECT * FROM notifications WHERE user_id = $1 ORDER BY created_at DESC LIMIT 50', [userId]);
    return res.rows;
  }
  return (fallbackStore.notifications || []).filter(n => n.user_id === userId);
}

export async function addNotification(userId, { id, title, message, type, actionUrl }) {
  if (isPostgres && pool) {
    const res = await pool.query(
      `INSERT INTO notifications (id, user_id, title, message, type, action_url)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [id, userId, title, message, type || 'Study', actionUrl || null]
    );
    return res.rows[0];
  }
  const record = {
    id,
    user_id: userId,
    title,
    message,
    type: type || 'Study',
    is_read: false,
    action_url: actionUrl || null,
    created_at: new Date().toISOString()
  };
  if (!fallbackStore.notifications) fallbackStore.notifications = [];
  fallbackStore.notifications.unshift(record);
  persistFallback();
  return record;
}

export async function markNotificationsRead(userId) {
  if (isPostgres && pool) {
    await pool.query('UPDATE notifications SET is_read = TRUE WHERE user_id = $1', [userId]);
    return true;
  }
  if (fallbackStore.notifications) {
    fallbackStore.notifications.forEach(n => {
      if (n.user_id === userId) n.is_read = true;
    });
    persistFallback();
  }
  return true;
}

/* -------------------------------------------------------------
   DEFAULT SEED HELPER (FOR NEW USERS)
------------------------------------------------------------- */
function seedDefaultDataForUser(userId) {
  const subjects = [
    { id: 's1_' + userId, user_id: userId, name: "DBMS", description: "Database Management Systems", importance: "High", color: "#2F5DE3" },
    { id: 's2_' + userId, user_id: userId, name: "Artificial Intelligence", description: "Core AI & neural networks", importance: "High", color: "#E23B6E" },
    { id: 's3_' + userId, user_id: userId, name: "Quantum Computing", description: "Intro to qubits & gates", importance: "Medium", color: "#1E8F6F" },
    { id: 's4_' + userId, user_id: userId, name: "Mathematics", description: "Linear algebra & calculus", importance: "Medium", color: "#C9840F" },
    { id: 's5_' + userId, user_id: userId, name: "English", description: "Technical writing", importance: "Low", color: "#7A5CFA" }
  ];
  fallbackStore.subjects.push(...subjects);

  const topics = [
    { id: 't1_' + userId, user_id: userId, subject_id: 's1_' + userId, name: "ER Model", description: "Entity-relationship diagrams", difficulty: "Easy", importance: "Medium", est_minutes: 30, status: "Completed" },
    { id: 't2_' + userId, user_id: userId, subject_id: 's1_' + userId, name: "Relational Algebra", description: "Set operations on relations", difficulty: "Medium", importance: "Medium", est_minutes: 40, status: "Completed" },
    { id: 't3_' + userId, user_id: userId, subject_id: 's1_' + userId, name: "Normalization", description: "1NF through BCNF", difficulty: "Hard", importance: "High", est_minutes: 60, status: "In Progress" },
    { id: 't4_' + userId, user_id: userId, subject_id: 's1_' + userId, name: "Transactions", description: "ACID, concurrency control", difficulty: "Hard", importance: "High", est_minutes: 45, status: "Not Started" },
    { id: 't5_' + userId, user_id: userId, subject_id: 's2_' + userId, name: "CNN", description: "Convolutional networks", difficulty: "Hard", importance: "Medium", est_minutes: 50, status: "In Progress" },
    { id: 't6_' + userId, user_id: userId, subject_id: 's2_' + userId, name: "Backpropagation", description: "Gradient updates", difficulty: "Hard", importance: "High", est_minutes: 45, status: "Not Started" },
    { id: 't7_' + userId, user_id: userId, subject_id: 's3_' + userId, name: "Qubits", description: "Superposition basics", difficulty: "Medium", importance: "High", est_minutes: 30, status: "Completed" }
  ];
  fallbackStore.topics.push(...topics);

  const d = new Date();
  const d5 = new Date(d); d5.setDate(d.getDate() + 5);
  const d12 = new Date(d); d12.setDate(d.getDate() + 12);
  const d20 = new Date(d); d20.setDate(d.getDate() + 20);

  const exams = [
    { id: 'e1_' + userId, user_id: userId, subject_id: 's1_' + userId, name: "DBMS Midterm", exam_date: d5.toISOString().slice(0, 10), importance: "High" },
    { id: 'e2_' + userId, user_id: userId, subject_id: 's2_' + userId, name: "AI Final", exam_date: d12.toISOString().slice(0, 10), importance: "High" },
    { id: 'e3_' + userId, user_id: userId, subject_id: 's3_' + userId, name: "Quantum Computing Quiz", exam_date: d20.toISOString().slice(0, 10), importance: "Medium" }
  ];
  fallbackStore.exams.push(...exams);

  fallbackStore.notifications.push({
    id: 'n1_' + userId,
    user_id: userId,
    title: "Welcome to Study Planner",
    message: "Your timetable priority engine and syllabus tracker are active.",
    type: "Study",
    is_read: false,
    created_at: new Date().toISOString()
  });
}
