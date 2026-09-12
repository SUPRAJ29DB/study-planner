import React, { useState, useEffect, useRef } from "react";
import {
  LayoutDashboard, BookOpen, ListChecks, CalendarClock, Sparkles, Timer as TimerIcon,
  TrendingUp, Bot, Settings as SettingsIcon, Plus, X, Check, Play, Pause, Square,
  Sun, Moon, Flame, Trash2, Pencil, Menu, Circle, CheckCircle2, Bell, Database,
  RefreshCw, Send, ShieldCheck
} from "lucide-react";

/* ---------------------------------------------------------------
   DESIGN TOKENS & ACADEMIC STYLING
----------------------------------------------------------------*/
const THEME = {
  light: {
    bg: "#F3F5F1", surface: "#FFFFFF", ink: "#16213A", inkSoft: "#5B6472",
    border: "#DEE3E0", accent: "#2F5DE3", accentInk: "#FFFFFF",
    urgent: "#E23B6E", warn: "#C9840F", good: "#1E8F6F", grid: "rgba(22,33,58,0.055)",
    sidebar: "#111A2E", sidebarInk: "#C9D3E6", sidebarActive: "#1E2B48",
  },
  dark: {
    bg: "#0E1524", surface: "#141D33", ink: "#ECF0F6", inkSoft: "#94A1BC",
    border: "#25314F", accent: "#7C97FF", accentInk: "#0E1524",
    urgent: "#FF7A9C", warn: "#F0A94E", good: "#3FCBA0", grid: "rgba(255,255,255,0.045)",
    sidebar: "#0A101F", sidebarInk: "#8C99B8", sidebarActive: "#161F38",
  },
};

const SUBJECT_PALETTE = ["#2F5DE3", "#E23B6E", "#1E8F6F", "#C9840F", "#7A5CFA", "#0EA5B7"];

const uid = () => 'id_' + Math.random().toString(36).slice(2, 10);
const todayISO = () => new Date().toISOString().slice(0, 10);
const daysBetween = (dateStr) => {
  if (!dateStr) return 30;
  const d = new Date(dateStr + "T00:00:00");
  const now = new Date(); now.setHours(0, 0, 0, 0);
  return Math.round((d - now) / 86400000);
};
const IMPORTANCE_WEIGHT = { Low: 1, Medium: 2, High: 3 };
const DIFFICULTY_WEIGHT = { Easy: 1, Medium: 2, Hard: 3 };

function priorityScore(topic, subject, exam) {
  const daysLeft = exam ? Math.max(daysBetween(exam.date || exam.exam_date), 1) : 30;
  const urgency = 1 / daysLeft;
  const subjImportance = IMPORTANCE_WEIGHT[subject?.importance || "Medium"];
  const difficulty = DIFFICULTY_WEIGHT[topic.difficulty || "Medium"];
  const topicImportance = IMPORTANCE_WEIGHT[topic.importance || "Medium"];
  const remainingWork = topic.status === "Completed" ? 0.15 : topic.status === "In Progress" ? 0.8 : 1;
  return urgency * 100 * subjImportance * difficulty * topicImportance * remainingWork;
}

function buildTodayPlan({ subjects, topics, exams }, minutesAvailable, goal) {
  const examBySubject = {};
  exams.forEach((e) => {
    const sId = e.subjectId || e.subject_id;
    const eDate = e.date || e.exam_date;
    if (!examBySubject[sId] || daysBetween(eDate) < daysBetween(examBySubject[sId].date || examBySubject[sId].exam_date)) {
      examBySubject[sId] = e;
    }
  });

  let pool = topics.filter((t) => t.status !== "Completed" || goal === "Revision");
  if (goal === "New Topics") pool = pool.filter((t) => t.status === "Not Started");
  if (goal === "Revision") pool = topics;

  const scored = pool.map((t) => {
    const sId = t.subjectId || t.subject_id;
    const subject = subjects.find((s) => s.id === sId);
    const exam = examBySubject[sId];
    return { topic: t, subject, score: priorityScore(t, subject, exam) };
  }).sort((a, b) => b.score - a.score);

  const plan = [];
  let remaining = minutesAvailable;
  let clock = 9 * 60;
  let sinceBreak = 0;

  for (const item of scored) {
    if (!item.subject) continue;
    if (remaining <= 10) break;
    const est = item.topic.estMinutes || item.topic.est_minutes || 30;
    const duration = Math.min(est, remaining);
    if (duration < 10) continue;
    plan.push({
      id: uid(),
      subject: item.subject.name,
      color: item.subject.color,
      topic: item.topic.name,
      duration,
      start: clock,
      completed: false
    });
    clock += duration; remaining -= duration; sinceBreak += duration;
    if (sinceBreak >= 45 && remaining > 15) {
      plan.push({ id: uid(), isBreak: true, duration: 15, start: clock });
      clock += 15; remaining -= 15; sinceBreak = 0;
    }
  }
  if (remaining >= 15) {
    plan.push({ id: uid(), subject: "Revision", color: "#8A93A6", topic: "Review weak topics", duration: remaining, start: clock, completed: false });
  }
  return plan;
}

function minutesToClock(mins) {
  const h = Math.floor(mins / 60) % 24;
  const m = mins % 60;
  const ampm = h >= 12 ? "PM" : "AM";
  const h12 = ((h + 11) % 12) + 1;
  return `${String(h12).padStart(2, "0")}:${String(m).padStart(2, "0")} ${ampm}`;
}

export default function StudyPlannerApp() {
  const [theme, setTheme] = useState("light");
  const T = THEME[theme];
  const [page, setPage] = useState("Dashboard");
  const [navOpen, setNavOpen] = useState(false);

  // User Identity
  const [currentUser, setCurrentUser] = useState({
    name: "Suprakash Ghosh",
    email: "suprakashg21@gmail.com",
    role: "Student"
  });

  // Server & Database Status
  const [dbStatus, setDbStatus] = useState({
    type: "Connecting...",
    connected: false,
    persistent: true,
    message: "Initializing cloud storage"
  });

  // App Data
  const [data, setData] = useState({
    subjects: [],
    topics: [],
    exams: [],
    sessions: [],
    reminders: [],
    notifications: []
  });

  const [todayPlan, setTodayPlan] = useState([]);
  const [plannerOpen, setPlannerOpen] = useState(false);
  const [notifModalOpen, setNotifModalOpen] = useState(false);
  const [activeStudy, setActiveStudy] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load Initial Data from Backend API
  const refreshData = async () => {
    try {
      setIsLoading(true);
      const [statusRes, userRes, subjRes, topicsRes, examsRes, tasksRes, sessionsRes, remRes, notifRes] = await Promise.all([
        fetch('/api/status').then(r => r.json()).catch(() => null),
        fetch('/api/user/me').then(r => r.json()).catch(() => null),
        fetch('/api/subjects').then(r => r.json()).catch(() => []),
        fetch('/api/topics').then(r => r.json()).catch(() => []),
        fetch('/api/exams').then(r => r.json()).catch(() => []),
        fetch('/api/plan/today').then(r => r.json()).catch(() => []),
        fetch('/api/sessions').then(r => r.json()).catch(() => []),
        fetch('/api/reminders').then(r => r.json()).catch(() => []),
        fetch('/api/notifications').then(r => r.json()).catch(() => [])
      ]);

      if (statusRes?.database) setDbStatus(statusRes.database);
      if (userRes?.name) setCurrentUser(userRes);

      const subjects = Array.isArray(subjRes) ? subjRes : [];
      const topics = Array.isArray(topicsRes) ? topicsRes : [];
      const exams = Array.isArray(examsRes) ? examsRes : [];
      const sessions = Array.isArray(sessionsRes) ? sessionsRes : [];
      const reminders = Array.isArray(remRes) ? remRes : [];
      const notifications = Array.isArray(notifRes) ? notifRes : [];

      setData({ subjects, topics, exams, sessions, reminders, notifications });

      if (tasksRes && tasksRes.length > 0) {
        setTodayPlan(tasksRes.map(t => ({
          id: t.id,
          subject: t.subject_name || t.subject,
          topic: t.topic_name || t.topic,
          duration: t.duration_minutes || t.duration,
          start: t.start_time_minutes || t.start,
          isBreak: !!t.is_break,
          completed: !!t.is_completed,
          color: t.color || "#2F5DE3"
        })));
      } else if (subjects.length > 0 && topics.length > 0) {
        const generated = buildTodayPlan({ subjects, topics, exams }, 180, "Exam Preparation");
        setTodayPlan(generated);
      }
    } catch (err) {
      console.error("Failed to load initial data:", err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    refreshData();
  }, []);

  const completedTasks = todayPlan.filter((t) => !t.isBreak && t.completed).length;
  const totalTasks = todayPlan.filter((t) => !t.isBreak).length;
  const progressPct = totalTasks ? Math.round((completedTasks / totalTasks) * 100) : 0;
  const unreadNotifs = data.notifications.filter(n => !n.is_read).length;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";

  const toggleTaskDone = async (id) => {
    setTodayPlan((p) => p.map((t) => (t.id === id ? { ...t, completed: !t.completed } : t)));
    try {
      await fetch(`/api/plan/tasks/${id}/toggle`, { method: 'POST' });
    } catch (e) {
      console.error(e);
    }
  };

  const handleGeneratePlan = async (minutes, goal) => {
    const newPlan = buildTodayPlan(data, minutes, goal);
    setTodayPlan(newPlan);
    setPlannerOpen(false);
    setPage("Dashboard");
    try {
      await fetch('/api/plan/today', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tasks: newPlan })
      });
    } catch (e) {
      console.error(e);
    }
  };

  const navItems = [
    { key: "Dashboard", icon: LayoutDashboard },
    { key: "Subjects", icon: BookOpen },
    { key: "Syllabus", icon: ListChecks },
    { key: "Exams", icon: CalendarClock },
    { key: "Study", icon: TimerIcon },
    { key: "Progress", icon: TrendingUp },
    { key: "AI Assistant", icon: Bot },
    { key: "Settings", icon: SettingsIcon },
  ];

  return (
    <div style={{ background: T.bg, color: T.ink, minHeight: "100vh", fontFamily: "Inter, ui-sans-serif, system-ui" }}>
      <style>{`
        .sp-display { font-family: 'Fraunces', serif; }
        .sp-mono { font-family: 'IBM Plex Mono', monospace; font-variant-numeric: tabular-nums; }
        .sp-ruled { background-image: linear-gradient(${T.grid} 1px, transparent 1px); background-size: 100% 2.35rem; }
        .sp-scroll::-webkit-scrollbar { width: 6px; height: 6px; }
        .sp-scroll::-webkit-scrollbar-thumb { background: ${T.border}; border-radius: 4px; }
        .sp-focus:focus-visible { outline: 2px solid ${T.accent}; outline-offset: 2px; }
      `}</style>

      <div className="flex" style={{ minHeight: "100vh" }}>
        {/* SIDEBAR */}
        <aside
          className={`fixed md:static z-30 h-full md:h-auto transition-transform duration-200 ${navOpen ? "translate-x-0" : "-translate-x-full"} md:translate-x-0`}
          style={{ background: T.sidebar, width: 240, flexShrink: 0 }}
        >
          <div className="px-5 py-6 flex items-center justify-between" style={{ borderBottom: `1px solid ${T.border}30` }}>
            <div className="flex items-center gap-2">
              <div style={{ width: 10, height: 10, background: T.accent, borderRadius: 2 }} />
              <span className="sp-display" style={{ color: "#fff", fontSize: 19, letterSpacing: 0.2 }}>Study Planner</span>
            </div>
            <button onClick={refreshData} title="Sync database" className="sp-focus text-xs p-1 rounded" style={{ color: T.sidebarInk }}>
              <RefreshCw size={14} className={isLoading ? "animate-spin" : ""} />
            </button>
          </div>

          <nav className="px-3 py-4 flex flex-col gap-1">
            {navItems.map(({ key, icon: Icon }) => (
              <button
                key={key}
                onClick={() => { setPage(key); setNavOpen(false); }}
                className="sp-focus flex items-center gap-3 px-3 py-2.5 rounded-md text-sm text-left transition-colors"
                style={{
                  background: page === key ? T.sidebarActive : "transparent",
                  color: page === key ? "#fff" : T.sidebarInk,
                  fontWeight: page === key ? 600 : 500,
                }}
              >
                <Icon size={17} strokeWidth={2} />
                {key}
              </button>
            ))}
          </nav>

          {/* User & Database Status in Sidebar */}
          <div className="px-4 mt-auto pb-6">
            <div className="p-3 rounded-md mb-3" style={{ background: `${T.accent}14`, border: `1px solid ${T.accent}30` }}>
              <div className="flex items-center gap-2 mb-1" style={{ color: T.accent }}>
                <Flame size={15} />
                <span className="text-xs font-semibold">4-day streak</span>
              </div>
              <p className="text-xs" style={{ color: T.sidebarInk }}>Study today to keep it alive.</p>
            </div>

            <div className="p-3 rounded-md" style={{ background: "rgba(255,255,255,0.03)", border: `1px solid ${T.border}20` }}>
              <div className="flex items-center gap-2">
                <Database size={13} style={{ color: dbStatus.connected ? T.good : T.accent }} />
                <span className="text-[11px] font-semibold text-white truncate">{dbStatus.type}</span>
              </div>
              <p className="text-[10px] mt-1 truncate" style={{ color: T.sidebarInk }}>
                {dbStatus.connected ? "Cloud synced" : "Persistent storage"}
              </p>
            </div>
          </div>
        </aside>

        {navOpen && <div className="fixed inset-0 bg-black/40 z-20 md:hidden" onClick={() => setNavOpen(false)} />}

        {/* MAIN BODY */}
        <div className="flex-1 min-w-0 flex flex-col">
          {/* TOP BAR */}
          <header className="flex items-center justify-between px-5 md:px-8 py-4" style={{ borderBottom: `1px solid ${T.border}` }}>
            <div className="flex items-center gap-3">
              <button className="md:hidden sp-focus" onClick={() => setNavOpen(true)} style={{ color: T.ink }}>
                <Menu size={22} />
              </button>
              <div>
                <h1 className="sp-display" style={{ fontSize: 22, lineHeight: 1.1 }}>
                  {greeting}, {currentUser.name.split(" ")[0]}
                </h1>
                <p className="text-xs mt-0.5" style={{ color: T.inkSoft }}>
                  {new Date().toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" })}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3">
              {/* Notification Bell */}
              <button
                onClick={() => setNotifModalOpen(true)}
                className="sp-focus relative w-9 h-9 rounded-md flex items-center justify-center"
                style={{ border: `1px solid ${T.border}`, color: T.inkSoft }}
                aria-label="Notifications"
              >
                <Bell size={16} />
                {unreadNotifs > 0 && (
                  <span
                    className="absolute -top-1 -right-1 w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-bold text-white"
                    style={{ background: T.urgent }}
                  >
                    {unreadNotifs}
                  </span>
                )}
              </button>

              {/* Theme Toggle */}
              <button
                onClick={() => setTheme(theme === "light" ? "dark" : "light")}
                className="sp-focus w-9 h-9 rounded-md flex items-center justify-center"
                style={{ border: `1px solid ${T.border}`, color: T.inkSoft }}
                aria-label="Toggle theme"
              >
                {theme === "light" ? <Moon size={16} /> : <Sun size={16} />}
              </button>

              {/* Plan Day Button */}
              <button
                onClick={() => setPlannerOpen(true)}
                className="sp-focus flex items-center gap-2 px-4 py-2 rounded-md text-sm font-semibold"
                style={{ background: T.accent, color: T.accentInk }}
              >
                <Sparkles size={15} /> Plan my day
              </button>
            </div>
          </header>

          <main className="p-5 md:p-8 flex-1 sp-scroll" style={{ overflowY: "auto" }}>
            {page === "Dashboard" && (
              <Dashboard
                T={T}
                data={data}
                todayPlan={todayPlan}
                toggleTaskDone={toggleTaskDone}
                progressPct={progressPct}
                completedTasks={completedTasks}
                totalTasks={totalTasks}
                onStartTask={(t) => { setActiveStudy(t); setPage("Study"); }}
              />
            )}
            {page === "Subjects" && <SubjectsPage T={T} data={data} setData={setData} refreshData={refreshData} />}
            {page === "Syllabus" && <SyllabusPage T={T} data={data} setData={setData} setPage={setPage} />}
            {page === "Exams" && <ExamsPage T={T} data={data} setData={setData} refreshData={refreshData} />}
            {page === "Study" && (
              <StudyPage
                T={T}
                activeStudy={activeStudy}
                setActiveStudy={setActiveStudy}
                todayPlan={todayPlan}
                onFinishSession={async (session) => {
                  setData((d) => ({ ...d, sessions: [session, ...d.sessions] }));
                  await fetch('/api/sessions', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(session)
                  });
                }}
              />
            )}
            {page === "Progress" && <ProgressPage T={T} data={data} />}
            {page === "AI Assistant" && <AIAssistantPage T={T} data={data} />}
            {page === "Settings" && (
              <SettingsPage
                T={T}
                theme={theme}
                setTheme={setTheme}
                currentUser={currentUser}
                dbStatus={dbStatus}
                refreshData={refreshData}
              />
            )}
          </main>
        </div>
      </div>

      {/* Plan My Day Modal */}
      {plannerOpen && (
        <PlanMyDayModal
          T={T}
          onClose={() => setPlannerOpen(false)}
          onGenerate={handleGeneratePlan}
        />
      )}

      {/* Notification Center Modal */}
      {notifModalOpen && (
        <NotificationCenterModal
          T={T}
          notifications={data.notifications}
          onClose={() => setNotifModalOpen(false)}
          onMarkRead={async () => {
            setData(d => ({ ...d, notifications: d.notifications.map(n => ({ ...n, is_read: true })) }));
            await fetch('/api/notifications/mark-read', { method: 'POST' });
          }}
        />
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   DASHBOARD
----------------------------------------------------------------*/
function Dashboard({ T, data, todayPlan, toggleTaskDone, progressPct, completedTasks, totalTasks, onStartTask }) {
  const upcomingExams = [...data.exams].sort((a, b) => daysBetween(a.date || a.exam_date) - daysBetween(b.date || b.exam_date));

  return (
    <div className="flex flex-col gap-6">
      <div className="grid grid-cols-1 md:grid-cols-[280px_1fr] gap-5">
        {/* Progress ring card */}
        <div className="rounded-lg p-5 flex flex-col items-center justify-center gap-3" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
          <p className="text-xs font-semibold tracking-wide" style={{ color: T.inkSoft }}>Today's progress</p>
          <ProgressRing pct={progressPct} T={T} />
          <p className="text-sm" style={{ color: T.inkSoft }}>{completedTasks} of {totalTasks} tasks completed</p>
        </div>

        {/* Today's plan */}
        <div className="rounded-lg p-5 sp-ruled" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
          <div className="flex items-center justify-between mb-3">
            <h2 className="sp-display" style={{ fontSize: 18 }}>Today's plan</h2>
            <span className="sp-mono text-xs" style={{ color: T.inkSoft }}>{todayPlan.reduce((a, t) => a + t.duration, 0)} min total</span>
          </div>
          <div className="flex flex-col">
            {todayPlan.length === 0 && (
              <p className="text-sm py-4 text-center" style={{ color: T.inkSoft }}>No tasks planned for today. Click "Plan my day" to generate your timetable.</p>
            )}
            {todayPlan.map((t) =>
              t.isBreak ? (
                <div key={t.id} className="flex items-center gap-3 py-2 text-sm" style={{ color: T.inkSoft }}>
                  <div style={{ width: 18 }} />
                  <span className="sp-mono">{minutesToClock(t.start)}</span>
                  <span>Break — {t.duration} min</span>
                </div>
              ) : (
                <div key={t.id} className="flex items-center gap-3 py-2">
                  <button onClick={() => toggleTaskDone(t.id)} className="sp-focus" style={{ color: t.completed ? T.good : T.inkSoft }}>
                    {t.completed ? <CheckCircle2 size={19} /> : <Circle size={19} />}
                  </button>
                  <span className="sp-mono text-xs w-20 flex-shrink-0" style={{ color: T.inkSoft }}>{minutesToClock(t.start)}</span>
                  <span className="w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ background: t.color }} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm truncate" style={{ textDecoration: t.completed ? "line-through" : "none", color: t.completed ? T.inkSoft : T.ink }}>
                      <span className="font-medium">{t.subject}</span>: {t.topic}
                    </p>
                  </div>
                  <span className="sp-mono text-xs" style={{ color: T.inkSoft }}>{t.duration}m</span>
                  {!t.completed && (
                    <button onClick={() => onStartTask(t)} className="sp-focus text-xs font-semibold px-2.5 py-1 rounded" style={{ color: T.accent, border: `1px solid ${T.accent}60` }}>
                      Start
                    </button>
                  )}
                </div>
              )
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {/* Upcoming exams */}
        <div>
          <h2 className="sp-display mb-3" style={{ fontSize: 18 }}>Upcoming exams</h2>
          <div className="flex flex-col gap-2.5">
            {upcomingExams.length === 0 && <EmptyState T={T} text="No upcoming exams. Add an exam to let the planner prioritize your studies." />}
            {upcomingExams.map((e) => {
              const days = daysBetween(e.date || e.exam_date);
              const urgent = days <= 5;
              const sId = e.subjectId || e.subject_id;
              const subj = data.subjects.find(s => s.id === sId);
              return (
                <div key={e.id} className="flex items-center justify-between rounded-md px-4 py-3 relative overflow-hidden"
                  style={{ background: T.surface, border: `1px solid ${T.border}`, borderLeft: `3px solid ${urgent ? T.urgent : T.border}` }}>
                  <div>
                    <p className="text-sm font-medium">{e.name}</p>
                    <p className="text-xs" style={{ color: T.inkSoft }}>{subj?.name || "General Subject"}</p>
                  </div>
                  <div className="text-right">
                    <p className="sp-mono text-base font-semibold" style={{ color: urgent ? T.urgent : T.ink }}>{days}</p>
                    <p className="text-[10px]" style={{ color: T.inkSoft }}>days left</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Subjects */}
        <div>
          <h2 className="sp-display mb-3" style={{ fontSize: 18 }}>Your subjects</h2>
          <div className="grid grid-cols-2 gap-2.5">
            {data.subjects.map((s) => {
              const subjTopics = data.topics.filter((t) => (t.subjectId || t.subject_id) === s.id);
              const done = subjTopics.filter((t) => t.status === "Completed").length;
              const pct = subjTopics.length ? Math.round((done / subjTopics.length) * 100) : 0;
              return (
                <div key={s.id} className="rounded-md px-3.5 py-3" style={{ background: T.surface, border: `1px solid ${T.border}`, borderLeft: `3px solid ${s.color}` }}>
                  <p className="text-sm font-medium truncate">{s.name}</p>
                  <p className="text-[11px] mb-2" style={{ color: T.inkSoft }}>{s.importance} priority</p>
                  <div className="h-1.5 rounded-full" style={{ background: T.grid }}>
                    <div className="h-1.5 rounded-full" style={{ width: `${pct}%`, background: s.color }} />
                  </div>
                  <p className="sp-mono text-[10px] mt-1" style={{ color: T.inkSoft }}>{pct}%</p>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}

function ProgressRing({ pct, T }) {
  const r = 46, c = 2 * Math.PI * r;
  return (
    <svg width={120} height={120} viewBox="0 0 120 120">
      <circle cx="60" cy="60" r={r} fill="none" stroke={T.grid} strokeWidth="10" />
      <circle cx="60" cy="60" r={r} fill="none" stroke={T.accent} strokeWidth="10" strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={c - (pct / 100) * c} transform="rotate(-90 60 60)" />
      <text x="60" y="66" textAnchor="middle" className="sp-mono" fontSize="24" fontWeight="600" fill={T.ink}>{pct}%</text>
    </svg>
  );
}

function EmptyState({ T, text, cta, onClick }) {
  return (
    <div className="rounded-md px-5 py-6 text-center" style={{ background: T.surface, border: `1px dashed ${T.border}` }}>
      <p className="text-sm mb-3" style={{ color: T.inkSoft }}>{text}</p>
      {cta && (
        <button onClick={onClick} className="sp-focus inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded"
          style={{ background: T.accent, color: T.accentInk }}>
          <Plus size={13} /> {cta}
        </button>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   SUBJECTS PAGE
----------------------------------------------------------------*/
function SubjectsPage({ T, data, setData, refreshData }) {
  const [form, setForm] = useState(null);

  const save = async (subj) => {
    setData((d) => {
      const exists = d.subjects.some((s) => s.id === subj.id);
      return { ...d, subjects: exists ? d.subjects.map((s) => (s.id === subj.id ? subj : s)) : [...d.subjects, subj] };
    });
    setForm(null);
    try {
      await fetch('/api/subjects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(subj)
      });
    } catch (e) {
      console.error(e);
    }
  };

  const remove = async (id) => {
    setData((d) => ({
      ...d,
      subjects: d.subjects.filter((s) => s.id !== id),
      topics: d.topics.filter(t => (t.subjectId || t.subject_id) !== id)
    }));
    try {
      await fetch(`/api/subjects/${id}`, { method: 'DELETE' });
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="sp-display" style={{ fontSize: 20 }}>Subjects</h2>
        <button onClick={() => setForm({ id: uid(), name: "", description: "", importance: "Medium", color: SUBJECT_PALETTE[data.subjects.length % SUBJECT_PALETTE.length] })}
          className="sp-focus flex items-center gap-1.5 text-sm font-semibold px-3 py-1.5 rounded" style={{ background: T.accent, color: T.accentInk }}>
          <Plus size={14} /> Add subject
        </button>
      </div>

      {data.subjects.length === 0 && (
        <EmptyState T={T} text="No subjects yet. Add your first subject to start planning." cta="Add subject"
          onClick={() => setForm({ id: uid(), name: "", description: "", importance: "Medium", color: SUBJECT_PALETTE[0] })} />
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {data.subjects.map((s) => (
          <div key={s.id} className="rounded-md p-4" style={{ background: T.surface, border: `1px solid ${T.border}`, borderLeft: `4px solid ${s.color}` }}>
            <div className="flex items-start justify-between">
              <div>
                <p className="font-medium text-sm">{s.name}</p>
                <p className="text-xs mt-0.5" style={{ color: T.inkSoft }}>{s.description}</p>
              </div>
              <div className="flex gap-1">
                <button onClick={() => setForm(s)} className="sp-focus" style={{ color: T.inkSoft }}><Pencil size={14} /></button>
                <button onClick={() => remove(s.id)} className="sp-focus" style={{ color: T.inkSoft }}><Trash2 size={14} /></button>
              </div>
            </div>
            <span className="inline-block mt-3 text-[11px] font-semibold px-2 py-0.5 rounded"
              style={{ background: `${s.color}20`, color: s.color }}>{s.importance} importance</span>
          </div>
        ))}
      </div>

      {form && (
        <Modal T={T} title={data.subjects.some(s => s.id === form.id) ? "Edit subject" : "Add subject"} onClose={() => setForm(null)}>
          <FieldText T={T} label="Subject name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} />
          <FieldText T={T} label="Description" value={form.description} onChange={(v) => setForm({ ...form, description: v })} />
          <FieldSelect T={T} label="Importance" value={form.importance} options={["Low", "Medium", "High"]} onChange={(v) => setForm({ ...form, importance: v })} />
          <ModalActions T={T} onCancel={() => setForm(null)} onSave={() => form.name.trim() && save(form)} />
        </Modal>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   SYLLABUS PAGE (With Gemini Explanations)
----------------------------------------------------------------*/
function SyllabusPage({ T, data, setData, setPage }) {
  const [openSubj, setOpenSubj] = useState(data.subjects[0]?.id);
  const [form, setForm] = useState(null);
  const [aiModal, setAiModal] = useState(null); // { topic, subject, text, loading }

  useEffect(() => {
    if (!openSubj && data.subjects[0]) setOpenSubj(data.subjects[0].id);
  }, [data.subjects]);

  const setStatus = async (id, status) => {
    setData((d) => ({ ...d, topics: d.topics.map((t) => (t.id === id ? { ...t, status } : t)) }));
    try {
      await fetch(`/api/topics/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status })
      });
    } catch (e) {
      console.error(e);
    }
  };

  const save = async (topic) => {
    setData((d) => {
      const exists = d.topics.some((t) => t.id === topic.id);
      return { ...d, topics: exists ? d.topics.map((t) => (t.id === topic.id ? topic : t)) : [...d.topics, topic] };
    });
    setForm(null);
    try {
      await fetch('/api/topics', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(topic)
      });
    } catch (e) {
      console.error(e);
    }
  };

  const remove = async (id) => {
    setData((d) => ({ ...d, topics: d.topics.filter((t) => t.id !== id) }));
    try {
      await fetch(`/api/topics/${id}`, { method: 'DELETE' });
    } catch (e) {
      console.error(e);
    }
  };

  const explainWithGemini = async (topicName, subjectName) => {
    setAiModal({ topic: topicName, subject: subjectName, text: "Generating clear academic explanation with Gemini 2.5 Flash...", loading: true });
    try {
      const res = await fetch('/api/ai/explain', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ topic: topicName, subject: subjectName })
      });
      const data = await res.json();
      setAiModal({ topic: topicName, subject: subjectName, text: data.text, loading: false });
    } catch (e) {
      setAiModal({ topic: topicName, subject: subjectName, text: "Error connecting to AI assistant.", loading: false });
    }
  };

  const activeSubject = data.subjects.find(s => s.id === openSubj);

  return (
    <div>
      <h2 className="sp-display mb-4" style={{ fontSize: 20 }}>Syllabus</h2>
      <div className="flex gap-2 mb-5 overflow-x-auto sp-scroll pb-1">
        {data.subjects.map((s) => (
          <button key={s.id} onClick={() => setOpenSubj(s.id)}
            className="sp-focus px-3 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap flex-shrink-0"
            style={{
              background: openSubj === s.id ? s.color : "transparent",
              color: openSubj === s.id ? "#fff" : T.inkSoft,
              border: `1px solid ${openSubj === s.id ? s.color : T.border}`
            }}>
            {s.name}
          </button>
        ))}
      </div>

      <div className="flex items-center justify-between mb-2">
        <p className="text-xs font-semibold" style={{ color: T.inkSoft }}>Topics in {activeSubject?.name || "Subject"}</p>
        <button onClick={() => setForm({ id: uid(), subject_id: openSubj, name: "", description: "", difficulty: "Medium", importance: "Medium", est_minutes: 30, status: "Not Started" })}
          className="sp-focus flex items-center gap-1 text-xs font-semibold" style={{ color: T.accent }}>
          <Plus size={13} /> Add topic
        </button>
      </div>

      <div className="rounded-md sp-ruled" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        {data.topics.filter((t) => (t.subjectId || t.subject_id) === openSubj).map((t) => (
          <div key={t.id} className="flex items-center gap-3 px-4 py-3" style={{ borderBottom: `1px solid ${T.border}` }}>
            <button onClick={() => setStatus(t.id, t.status === "Completed" ? "Not Started" : "Completed")} className="sp-focus"
              style={{ color: t.status === "Completed" ? T.good : T.inkSoft }}>
              {t.status === "Completed" ? <CheckCircle2 size={18} /> : <Circle size={18} />}
            </button>
            <div className="flex-1 min-w-0">
              <p className="text-sm" style={{ textDecoration: t.status === "Completed" ? "line-through" : "none", color: t.status === "Completed" ? T.inkSoft : T.ink }}>{t.name}</p>
              <p className="text-xs" style={{ color: T.inkSoft }}>{t.description}</p>
            </div>
            <button
              onClick={() => explainWithGemini(t.name, activeSubject?.name || "General")}
              title="Explain with Gemini AI"
              className="sp-focus text-xs flex items-center gap-1 px-2 py-1 rounded"
              style={{ background: `${T.accent}14`, color: T.accent, border: `1px solid ${T.accent}30` }}
            >
              <Bot size={13} /> Explain
            </button>
            <Badge T={T} text={t.difficulty} />
            <span className="sp-mono text-xs w-14 text-right" style={{ color: T.inkSoft }}>{t.estMinutes || t.est_minutes || 30}m</span>
            <StatusPill T={T} status={t.status} />
            <button onClick={() => setForm(t)} className="sp-focus" style={{ color: T.inkSoft }}><Pencil size={13} /></button>
            <button onClick={() => remove(t.id)} className="sp-focus" style={{ color: T.inkSoft }}><Trash2 size={13} /></button>
          </div>
        ))}
        {data.topics.filter((t) => (t.subjectId || t.subject_id) === openSubj).length === 0 && (
          <p className="text-sm px-4 py-6 text-center" style={{ color: T.inkSoft }}>No topics yet for this subject.</p>
        )}
      </div>

      {/* Add/Edit Topic Modal */}
      {form && (
        <Modal T={T} title={data.topics.some(t => t.id === form.id) ? "Edit topic" : "Add topic"} onClose={() => setForm(null)}>
          <FieldText T={T} label="Topic name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} />
          <FieldText T={T} label="Description" value={form.description} onChange={(v) => setForm({ ...form, description: v })} />
          <div className="grid grid-cols-2 gap-3">
            <FieldSelect T={T} label="Difficulty" value={form.difficulty} options={["Easy", "Medium", "Hard"]} onChange={(v) => setForm({ ...form, difficulty: v })} />
            <FieldSelect T={T} label="Importance" value={form.importance} options={["Low", "Medium", "High"]} onChange={(v) => setForm({ ...form, importance: v })} />
          </div>
          <FieldText T={T} label="Estimated minutes" type="number" value={form.estMinutes || form.est_minutes || 30} onChange={(v) => setForm({ ...form, estMinutes: Number(v) || 0, est_minutes: Number(v) || 0 })} />
          <ModalActions T={T} onCancel={() => setForm(null)} onSave={() => form.name.trim() && save(form)} />
        </Modal>
      )}

      {/* Gemini Explanation Popup */}
      {aiModal && (
        <Modal T={T} title={`Gemini Explanation: ${aiModal.topic}`} onClose={() => setAiModal(null)}>
          <div className="text-xs uppercase tracking-wider font-semibold" style={{ color: T.accent }}>
            Subject: {aiModal.subject}
          </div>
          <div className="text-sm whitespace-pre-wrap leading-relaxed max-h-[60vh] overflow-y-auto sp-scroll p-3 rounded"
            style={{ background: T.grid, color: T.ink }}>
            {aiModal.text}
          </div>
          <div className="flex justify-end mt-2">
            <button onClick={() => setAiModal(null)} className="sp-focus px-4 py-1.5 rounded text-sm font-semibold" style={{ background: T.accent, color: T.accentInk }}>
              Close
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   EXAMS PAGE
----------------------------------------------------------------*/
function ExamsPage({ T, data, setData, refreshData }) {
  const [form, setForm] = useState(null);

  const save = async (exam) => {
    setData((d) => {
      const exists = d.exams.some((e) => e.id === exam.id);
      return { ...d, exams: exists ? d.exams.map((e) => (e.id === exam.id ? exam : e)) : [...d.exams, exam] };
    });
    setForm(null);
    try {
      await fetch('/api/exams', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(exam)
      });
    } catch (e) {
      console.error(e);
    }
  };

  const remove = async (id) => {
    setData((d) => ({ ...d, exams: d.exams.filter((e) => e.id !== id) }));
    try {
      await fetch(`/api/exams/${id}`, { method: 'DELETE' });
    } catch (e) {
      console.error(e);
    }
  };

  const sorted = [...data.exams].sort((a, b) => daysBetween(a.date || a.exam_date) - daysBetween(b.date || b.exam_date));

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="sp-display" style={{ fontSize: 20 }}>Exams</h2>
        <button
          onClick={() => {
            const d = new Date(); d.setDate(d.getDate() + 7);
            setForm({ id: uid(), name: "", subject_id: data.subjects[0]?.id, date: d.toISOString().slice(0, 10), importance: "Medium" });
          }}
          className="sp-focus flex items-center gap-1.5 text-sm font-semibold px-3 py-1.5 rounded"
          style={{ background: T.accent, color: T.accentInk }}
        >
          <Plus size={14} /> Add exam
        </button>
      </div>

      {sorted.length === 0 && <EmptyState T={T} text="No upcoming exams. Add an exam to let the planner prioritize your studies." />}

      <div className="flex flex-col gap-2.5">
        {sorted.map((e) => {
          const days = daysBetween(e.date || e.exam_date);
          const urgent = days <= 5;
          const sId = e.subjectId || e.subject_id;
          const subj = data.subjects.find(s => s.id === sId);
          return (
            <div key={e.id} className="flex items-center gap-4 rounded-md px-4 py-3" style={{ background: T.surface, border: `1px solid ${T.border}`, borderLeft: `4px solid ${urgent ? T.urgent : T.border}` }}>
              <div className="text-center w-14 flex-shrink-0">
                <p className="sp-mono text-xl font-semibold" style={{ color: urgent ? T.urgent : T.ink }}>{days}</p>
                <p className="text-[10px]" style={{ color: T.inkSoft }}>days</p>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium">{e.name}</p>
                <p className="text-xs" style={{ color: T.inkSoft }}>
                  {subj?.name || "Subject"} · {new Date((e.date || e.exam_date) + "T00:00:00").toLocaleDateString(undefined, { month: "short", day: "numeric" })}
                </p>
              </div>
              <Badge T={T} text={e.importance} />
              <button onClick={() => setForm(e)} className="sp-focus" style={{ color: T.inkSoft }}><Pencil size={14} /></button>
              <button onClick={() => remove(e.id)} className="sp-focus" style={{ color: T.inkSoft }}><Trash2 size={14} /></button>
            </div>
          );
        })}
      </div>

      {form && (
        <Modal T={T} title={data.exams.some(e => e.id === form.id) ? "Edit exam" : "Add exam"} onClose={() => setForm(null)}>
          <FieldText T={T} label="Exam name" value={form.name} onChange={(v) => setForm({ ...form, name: v })} />
          <FieldSelect
            T={T}
            label="Subject"
            value={form.subjectId || form.subject_id}
            options={data.subjects.map(s => s.id)}
            labels={data.subjects.map(s => s.name)}
            onChange={(v) => setForm({ ...form, subject_id: v, subjectId: v })}
          />
          <FieldText T={T} label="Exam date" type="date" value={form.date || form.exam_date} onChange={(v) => setForm({ ...form, date: v, exam_date: v })} />
          <FieldSelect T={T} label="Importance" value={form.importance} options={["Low", "Medium", "High"]} onChange={(v) => setForm({ ...form, importance: v })} />
          <ModalActions T={T} onCancel={() => setForm(null)} onSave={() => form.name.trim() && save(form)} />
        </Modal>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   STUDY PAGE (Timer with Persistent Sessions)
----------------------------------------------------------------*/
function StudyPage({ T, activeStudy, setActiveStudy, todayPlan, onFinishSession }) {
  const task = activeStudy || todayPlan.find((t) => !t.isBreak && !t.completed);
  const [seconds, setSeconds] = useState(task ? task.duration * 60 : 0);
  const [running, setRunning] = useState(false);
  const [done, setDone] = useState(false);
  const intervalRef = useRef(null);
  const initialSeconds = task ? task.duration * 60 : 0;

  useEffect(() => {
    setSeconds(task ? task.duration * 60 : 0);
    setDone(false);
    setRunning(false);
  }, [task?.id]);

  useEffect(() => {
    if (running && seconds > 0) {
      intervalRef.current = setInterval(() => setSeconds((s) => s - 1), 1000);
    } else if (seconds === 0 && running) {
      setRunning(false);
      setDone(true);
    }
    return () => clearInterval(intervalRef.current);
  }, [running, seconds]);

  if (!task) {
    return <EmptyState T={T} text="No task selected. Pick a task from today's plan to start studying." />;
  }

  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");
  const pct = initialSeconds ? ((initialSeconds - seconds) / initialSeconds) * 100 : 0;

  const finish = () => {
    const elapsedMinutes = Math.round((initialSeconds - seconds) / 60) || task.duration;
    onFinishSession({
      id: uid(),
      topic: task.topic,
      topicName: task.topic,
      subject: task.subject,
      minutes: elapsedMinutes,
      date: todayISO()
    });
    setDone(true);
    setRunning(false);
  };

  return (
    <div className="max-w-md mx-auto flex flex-col items-center gap-6 pt-6">
      <div>
        <p className="text-xs text-center font-semibold mb-1" style={{ color: T.inkSoft }}>Active Study Session</p>
        <h2 className="sp-display text-center" style={{ fontSize: 22 }}>{task.subject}: {task.topic}</h2>
      </div>

      <div className="relative w-56 h-56 flex items-center justify-center">
        <svg width={224} height={224} viewBox="0 0 224 224" style={{ position: "absolute" }}>
          <circle cx="112" cy="112" r="100" fill="none" stroke={T.grid} strokeWidth="10" />
          <circle cx="112" cy="112" r="100" fill="none" stroke={done ? T.good : T.accent} strokeWidth="10" strokeLinecap="round"
            strokeDasharray={2 * Math.PI * 100} strokeDashoffset={2 * Math.PI * 100 * (1 - pct / 100)} transform="rotate(-90 112 112)"
            style={{ transition: "stroke-dashoffset 1s linear" }} />
        </svg>
        <span className="sp-mono" style={{ fontSize: 40, fontWeight: 600 }}>{mm}:{ss}</span>
      </div>

      {done ? (
        <div className="text-center">
          <p className="font-semibold flex items-center gap-2 justify-center" style={{ color: T.good }}><Check size={18} /> Study session completed & logged</p>
          <p className="text-sm mt-1" style={{ color: T.inkSoft }}>Duration: {task.duration} minutes</p>
        </div>
      ) : (
        <div className="flex gap-3">
          {!running ? (
            <button onClick={() => setRunning(true)} className="sp-focus flex items-center gap-2 px-5 py-2.5 rounded-md font-semibold text-sm" style={{ background: T.accent, color: T.accentInk }}>
              <Play size={15} /> {seconds === initialSeconds ? "Start" : "Resume"}
            </button>
          ) : (
            <button onClick={() => setRunning(false)} className="sp-focus flex items-center gap-2 px-5 py-2.5 rounded-md font-semibold text-sm" style={{ border: `1px solid ${T.border}` }}>
              <Pause size={15} /> Pause
            </button>
          )}
          <button onClick={finish} className="sp-focus flex items-center gap-2 px-5 py-2.5 rounded-md font-semibold text-sm" style={{ border: `1px solid ${T.border}`, color: T.inkSoft }}>
            <Square size={15} /> Finish
          </button>
        </div>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------
   PROGRESS PAGE
----------------------------------------------------------------*/
function ProgressPage({ T, data }) {
  const totalMinutes = data.sessions.reduce((a, s) => a + (s.duration_minutes || s.minutes || 0), 0);
  const todayMinutes = data.sessions.filter((s) => (s.session_date || s.date) === todayISO()).reduce((a, s) => a + (s.duration_minutes || s.minutes || 0), 0);
  const weekMinutes = data.sessions.filter((s) => daysBetween(s.session_date || s.date) >= -7).reduce((a, s) => a + (s.duration_minutes || s.minutes || 0), 0);
  const completedTopics = data.topics.filter((t) => t.status === "Completed").length;
  const overallPct = data.topics.length ? Math.round((completedTopics / data.topics.length) * 100) : 0;

  const stats = [
    { label: "Total study time", value: `${(totalMinutes / 60).toFixed(1)}h` },
    { label: "Today", value: `${todayMinutes}m` },
    { label: "This week", value: `${(weekMinutes / 60).toFixed(1)}h` },
    { label: "Topics completed", value: `${completedTopics}/${data.topics.length}` },
  ];

  return (
    <div>
      <h2 className="sp-display mb-4" style={{ fontSize: 20 }}>Progress Analytics</h2>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        {stats.map((s) => (
          <div key={s.label} className="rounded-md p-4" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
            <p className="sp-mono font-semibold" style={{ fontSize: 22 }}>{s.value}</p>
            <p className="text-xs mt-0.5" style={{ color: T.inkSoft }}>{s.label}</p>
          </div>
        ))}
      </div>

      <div className="rounded-md p-5 mb-6" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold">Overall Syllabus Completion</h3>
          <span className="sp-mono text-sm" style={{ color: T.accent }}>{overallPct}%</span>
        </div>
        <div className="h-2.5 rounded-full" style={{ background: T.grid }}>
          <div className="h-2.5 rounded-full" style={{ width: `${overallPct}%`, background: T.accent }} />
        </div>
      </div>

      <div className="rounded-md p-5" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <h3 className="text-sm font-semibold mb-4">Subject-wise Completion</h3>
        <div className="flex flex-col gap-4">
          {data.subjects.map((s) => {
            const subjTopics = data.topics.filter((t) => (t.subjectId || t.subject_id) === s.id);
            const done = subjTopics.filter((t) => t.status === "Completed").length;
            const pct = subjTopics.length ? Math.round((done / subjTopics.length) * 100) : 0;
            return (
              <div key={s.id}>
                <div className="flex items-center justify-between mb-1.5 text-sm">
                  <span>{s.name}</span>
                  <span className="sp-mono text-xs" style={{ color: T.inkSoft }}>{pct}%</span>
                </div>
                <div className="h-2 rounded-full" style={{ background: T.grid }}>
                  <div className="h-2 rounded-full" style={{ width: `${pct}%`, background: s.color }} />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------
   AI ASSISTANT PAGE (Gemini Powered)
----------------------------------------------------------------*/
function AIAssistantPage({ T, data }) {
  const [messages, setMessages] = useState([
    { role: 'assistant', text: "Hello! I'm your Gemini Study Coach. Ask me to explain difficult topics, create practice exam questions, or optimize your study timetable." }
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);

  const sendMessage = async () => {
    if (!input.trim() || loading) return;
    const userText = input.trim();
    setInput("");
    setMessages(m => [...m, { role: 'user', text: userText }]);
    setLoading(true);

    try {
      const res = await fetch('/api/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: userText })
      });
      const data = await res.json();
      setMessages(m => [...m, { role: 'assistant', text: data.text }]);
    } catch (e) {
      setMessages(m => [...m, { role: 'assistant', text: "Error connecting to AI service." }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto flex flex-col h-[75vh]">
      <div className="flex items-center gap-2 mb-3">
        <Bot size={22} style={{ color: T.accent }} />
        <h2 className="sp-display" style={{ fontSize: 20 }}>Gemini Study Assistant</h2>
      </div>

      <div className="flex-1 overflow-y-auto sp-scroll p-4 rounded-lg flex flex-col gap-3"
        style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        {messages.map((m, idx) => (
          <div key={idx} className={`p-3 rounded-lg text-sm max-w-[85%] whitespace-pre-wrap ${m.role === 'user' ? 'ml-auto' : 'mr-auto'}`}
            style={{
              background: m.role === 'user' ? T.accent : T.grid,
              color: m.role === 'user' ? T.accentInk : T.ink
            }}>
            {m.text}
          </div>
        ))}
        {loading && (
          <div className="mr-auto p-3 rounded-lg text-sm" style={{ background: T.grid, color: T.inkSoft }}>
            Thinking with Gemini 2.5 Flash...
          </div>
        )}
      </div>

      <div className="flex gap-2 mt-3">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Ask a question, e.g., 'Explain BCNF in DBMS with an example'..."
          className="sp-focus flex-1 px-3.5 py-2.5 rounded-md text-sm outline-none"
          style={{ background: T.surface, border: `1px solid ${T.border}`, color: T.ink }}
        />
        <button
          onClick={sendMessage}
          disabled={loading}
          className="sp-focus px-4 py-2.5 rounded-md font-semibold text-sm flex items-center gap-1.5"
          style={{ background: T.accent, color: T.accentInk, opacity: loading ? 0.6 : 1 }}
        >
          <Send size={15} /> Send
        </button>
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------
   SETTINGS PAGE
----------------------------------------------------------------*/
function SettingsPage({ T, theme, setTheme, currentUser, dbStatus, refreshData }) {
  return (
    <div className="max-w-xl flex flex-col gap-5">
      <h2 className="sp-display" style={{ fontSize: 20 }}>Settings & Infrastructure</h2>

      {/* Database & Cloud Architecture Card */}
      <div className="rounded-md p-5 flex flex-col gap-3" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Database size={18} style={{ color: dbStatus.connected ? T.good : T.accent }} />
            <h3 className="text-sm font-semibold">Database Engine</h3>
          </div>
          <span className="text-xs px-2 py-0.5 rounded font-semibold"
            style={{
              background: dbStatus.connected ? `${T.good}20` : `${T.accent}20`,
              color: dbStatus.connected ? T.good : T.accent
            }}>
            {dbStatus.type}
          </span>
        </div>
        <p className="text-xs leading-relaxed" style={{ color: T.inkSoft }}>
          {dbStatus.message}
        </p>
        <div className="pt-2 flex justify-between items-center text-xs" style={{ borderTop: `1px solid ${T.border}` }}>
          <span style={{ color: T.inkSoft }}>Cloud Run Compatibility: <strong className="text-emerald-500">Ready</strong></span>
          <button onClick={refreshData} className="sp-focus px-2.5 py-1 rounded text-xs font-medium" style={{ border: `1px solid ${T.border}` }}>
            Check Connection
          </button>
        </div>
      </div>

      {/* User Profile Card */}
      <div className="rounded-md p-5 flex items-center justify-between" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm text-white" style={{ background: T.accent }}>
            {currentUser.name.split(" ").map(n => n[0]).join("")}
          </div>
          <div>
            <p className="text-sm font-medium">{currentUser.name}</p>
            <p className="text-xs" style={{ color: T.inkSoft }}>{currentUser.email}</p>
          </div>
        </div>
        <span className="text-xs px-2.5 py-1 rounded flex items-center gap-1 font-semibold" style={{ background: `${T.good}15`, color: T.good }}>
          <ShieldCheck size={13} /> Authenticated
        </span>
      </div>

      {/* Appearance Card */}
      <div className="rounded-md p-5 flex items-center justify-between" style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <div>
          <p className="text-sm font-medium">Appearance</p>
          <p className="text-xs" style={{ color: T.inkSoft }}>Switch between light and dark mode</p>
        </div>
        <div className="flex rounded-md overflow-hidden" style={{ border: `1px solid ${T.border}` }}>
          {["light", "dark"].map((m) => (
            <button key={m} onClick={() => setTheme(m)} className="sp-focus px-3 py-1.5 text-xs font-semibold capitalize"
              style={{ background: theme === m ? T.accent : "transparent", color: theme === m ? T.accentInk : T.inkSoft }}>
              {m}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------
   MODALS
----------------------------------------------------------------*/
function PlanMyDayModal({ T, onClose, onGenerate }) {
  const [minutes, setMinutes] = useState(180);
  const [energy, setEnergy] = useState("High");
  const [goal, setGoal] = useState("Exam Preparation");

  return (
    <Modal T={T} title="Plan my day" onClose={onClose}>
      <div>
        <label className="text-xs font-semibold" style={{ color: T.inkSoft }}>How much time can you study today?</label>
        <div className="flex items-center gap-3 mt-2">
          <input type="range" min="30" max="480" step="15" value={minutes} onChange={(e) => setMinutes(Number(e.target.value))} className="flex-1" />
          <span className="sp-mono text-sm font-semibold w-16 text-right">{(minutes / 60).toFixed(1)}h</span>
        </div>
      </div>
      <div>
        <label className="text-xs font-semibold" style={{ color: T.inkSoft }}>Energy level</label>
        <div className="flex gap-2 mt-2">
          {["Low", "Medium", "High"].map((lvl) => (
            <button key={lvl} onClick={() => setEnergy(lvl)} className="sp-focus flex-1 py-2 rounded-md text-xs font-semibold"
              style={{ background: energy === lvl ? T.accent : "transparent", color: energy === lvl ? T.accentInk : T.inkSoft, border: `1px solid ${energy === lvl ? T.accent : T.border}` }}>
              {lvl}
            </button>
          ))}
        </div>
      </div>
      <div>
        <label className="text-xs font-semibold" style={{ color: T.inkSoft }}>Today's goal</label>
        <div className="flex flex-col gap-2 mt-2">
          {["New Topics", "Exam Preparation", "Revision"].map((g) => (
            <button key={g} onClick={() => setGoal(g)} className="sp-focus flex items-center justify-between px-3 py-2 rounded-md text-sm"
              style={{ border: `1px solid ${goal === g ? T.accent : T.border}`, color: goal === g ? T.accent : T.ink }}>
              {g === "New Topics" ? "Learn new topics" : g === "Exam Preparation" ? "Exam preparation" : "Revision"}
              {goal === g && <Check size={15} />}
            </button>
          ))}
        </div>
      </div>
      <ModalActions T={T} onCancel={onClose} onSave={() => onGenerate(minutes, goal)} saveLabel="Generate plan" />
    </Modal>
  );
}

function NotificationCenterModal({ T, notifications, onClose, onMarkRead }) {
  return (
    <Modal T={T} title="Notification Center" onClose={onClose}>
      <div className="flex justify-between items-center mb-1">
        <span className="text-xs" style={{ color: T.inkSoft }}>{notifications.length} alerts</span>
        <button onClick={onMarkRead} className="sp-focus text-xs font-semibold" style={{ color: T.accent }}>
          Mark all as read
        </button>
      </div>
      <div className="flex flex-col gap-2 max-h-[50vh] overflow-y-auto sp-scroll">
        {notifications.length === 0 && (
          <p className="text-center py-6 text-sm" style={{ color: T.inkSoft }}>No notifications.</p>
        )}
        {notifications.map((n) => (
          <div key={n.id} className="p-3 rounded-md"
            style={{
              background: n.is_read ? T.grid : `${T.accent}12`,
              border: `1px solid ${n.is_read ? T.border : `${T.accent}40`}`
            }}>
            <p className="text-sm font-semibold">{n.title}</p>
            <p className="text-xs mt-0.5" style={{ color: T.inkSoft }}>{n.message}</p>
          </div>
        ))}
      </div>
    </Modal>
  );
}

function Modal({ T, title, children, onClose }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center p-4" style={{ background: "rgba(10,14,24,0.5)" }} onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="w-full max-w-md rounded-lg p-5 flex flex-col gap-4 max-h-[90vh] overflow-y-auto sp-scroll"
        style={{ background: T.surface, border: `1px solid ${T.border}` }}>
        <div className="flex items-center justify-between">
          <h3 className="sp-display" style={{ fontSize: 18 }}>{title}</h3>
          <button onClick={onClose} className="sp-focus" style={{ color: T.inkSoft }}><X size={18} /></button>
        </div>
        {children}
      </div>
    </div>
  );
}

function ModalActions({ T, onCancel, onSave, saveLabel = "Save" }) {
  return (
    <div className="flex gap-2 justify-end mt-1">
      <button onClick={onCancel} className="sp-focus px-4 py-2 rounded-md text-sm font-medium" style={{ border: `1px solid ${T.border}`, color: T.inkSoft }}>Cancel</button>
      <button onClick={onSave} className="sp-focus px-4 py-2 rounded-md text-sm font-semibold" style={{ background: T.accent, color: T.accentInk }}>{saveLabel}</button>
    </div>
  );
}

function FieldText({ T, label, value, onChange, type = "text" }) {
  return (
    <div>
      <label className="text-xs font-semibold" style={{ color: T.inkSoft }}>{label}</label>
      <input type={type} value={value || ""} onChange={(e) => onChange(e.target.value)}
        className="sp-focus w-full mt-1.5 px-3 py-2 rounded-md text-sm outline-none"
        style={{ background: "transparent", border: `1px solid ${T.border}`, color: T.ink }} />
    </div>
  );
}

function FieldSelect({ T, label, value, options, labels, onChange }) {
  return (
    <div>
      <label className="text-xs font-semibold" style={{ color: T.inkSoft }}>{label}</label>
      <select value={value || options[0]} onChange={(e) => onChange(e.target.value)}
        className="sp-focus w-full mt-1.5 px-3 py-2 rounded-md text-sm outline-none"
        style={{ background: T.surface, border: `1px solid ${T.border}`, color: T.ink }}>
        {options.map((o, i) => <option key={o} value={o}>{labels ? labels[i] : o}</option>)}
      </select>
    </div>
  );
}

function Badge({ T, text }) {
  const color = text === "Hard" || text === "High" ? T.urgent : text === "Medium" ? T.warn : T.good;
  return <span className="text-[10px] font-semibold px-2 py-0.5 rounded flex-shrink-0" style={{ background: `${color}20`, color }}>{text}</span>;
}

function StatusPill({ T, status }) {
  const color = status === "Completed" ? T.good : status === "In Progress" ? T.accent : T.inkSoft;
  return <span className="text-[10px] font-semibold px-2 py-0.5 rounded flex-shrink-0" style={{ background: `${color}18`, color }}>{status}</span>;
}
