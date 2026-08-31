import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  projectApi,
  taskApi,
  Project,
  Task,
  TaskStatus,
  TaskPriority,
} from '../services/api';
import {
  CheckSquare,
  Clock,
  AlertTriangle,
  Ban,
  Layers,
  ExternalLink,
  RefreshCw,
  Filter,
} from 'lucide-react';
import LogoLoader from '../components/LogoLoader';

interface RichTask extends Task {
  projectName: string;
  projectId: number;
}

const STATUS_CONFIG: Record<TaskStatus, { label: string; color: string; bg: string; icon: JSX.Element }> = {
  TODO:        { label: 'To Do',       color: '#94a3b8', bg: 'rgba(148,163,184,0.12)', icon: <Clock size={12} /> },
  IN_PROGRESS: { label: 'In Progress', color: '#38bdf8', bg: 'rgba(56,189,248,0.12)',  icon: <RefreshCw size={12} /> },
  BLOCKED:     { label: 'Blocked',     color: '#f87171', bg: 'rgba(248,113,113,0.12)', icon: <Ban size={12} /> },
  DONE:        { label: 'Done',        color: '#34d399', bg: 'rgba(52,211,153,0.12)',  icon: <CheckSquare size={12} /> },
};

const PRIORITY_CONFIG: Record<TaskPriority, { color: string; dot: string }> = {
  LOW:      { color: '#94a3b8', dot: '#94a3b8' },
  MEDIUM:   { color: '#38bdf8', dot: '#38bdf8' },
  HIGH:     { color: '#fb923c', dot: '#fb923c' },
  CRITICAL: { color: '#f43f5e', dot: '#f43f5e' },
};

export default function Tasks() {
  const [projects, setProjects]     = useState<Project[]>([]);
  const [allTasks, setAllTasks]     = useState<RichTask[]>([]);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState('');
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  // Filters
  const [viewMode, setViewMode]         = useState<'list' | 'kanban'>('list');
  const [searchQ, setSearchQ]           = useState('');
  const [filterStatus, setFilterStatus] = useState<TaskStatus | 'ALL'>('ALL');
  const [filterPriority, setFilterPriority] = useState<TaskPriority | 'ALL'>('ALL');
  const [filterProject, setFilterProject]   = useState<number | 'ALL'>('ALL');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const projs = await projectApi.list();
      setProjects(projs);

      // Fetch tasks from every project in parallel
      const taskArrays = await Promise.all(
        projs.map((p) =>
          taskApi.list(p.id).then((tasks) =>
            tasks.map((t) => ({ ...t, projectName: p.name, projectId: p.id }))
          ).catch(() => [] as RichTask[])
        )
      );
      setAllTasks(taskArrays.flat());
    } catch {
      setError('Failed to load tasks. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  // ── Inline action handlers ──────────────────────────────────────────────────
  const handleStatus = async (task: RichTask, s: TaskStatus) => {
    setUpdatingId(task.id);
    try {
      const updated = await taskApi.status(task.id, s);
      setAllTasks((prev) =>
        prev.map((t) => t.id === task.id ? { ...t, ...updated, projectName: task.projectName, projectId: task.projectId } : t)
      );
    } catch { /* silent */ } finally { setUpdatingId(null); }
  };

  const handlePriority = async (task: RichTask, p: TaskPriority) => {
    setUpdatingId(task.id);
    try {
      const updated = await taskApi.priority(task.id, p);
      setAllTasks((prev) =>
        prev.map((t) => t.id === task.id ? { ...t, ...updated, projectName: task.projectName, projectId: task.projectId } : t)
      );
    } catch { /* silent */ } finally { setUpdatingId(null); }
  };

  const handleProgress = async (task: RichTask, pct: number) => {
    if (pct < 0 || pct > 100) return;
    setUpdatingId(task.id);
    try {
      const updated = await taskApi.progress(task.id, pct);
      setAllTasks((prev) =>
        prev.map((t) => t.id === task.id ? { ...t, ...updated, projectName: task.projectName, projectId: task.projectId } : t)
      );
    } catch { /* silent */ } finally { setUpdatingId(null); }
  };

  // ── Derived / filtered list ─────────────────────────────────────────────────
  const filtered = useMemo(() => {
    const q = searchQ.toLowerCase();
    return allTasks.filter((t) => {
      if (filterStatus   !== 'ALL' && t.status   !== filterStatus)   return false;
      if (filterPriority !== 'ALL' && t.priority  !== filterPriority) return false;
      if (filterProject  !== 'ALL' && t.projectId !== filterProject)  return false;
      if (q && !t.title.toLowerCase().includes(q) && !t.projectName.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [allTasks, filterStatus, filterPriority, filterProject, searchQ]);

  // ── Stats ───────────────────────────────────────────────────────────────────
  const stats = useMemo(() => ({
    total:      allTasks.length,
    done:       allTasks.filter((t) => t.status === 'DONE').length,
    inProgress: allTasks.filter((t) => t.status === 'IN_PROGRESS').length,
    blocked:    allTasks.filter((t) => t.status === 'BLOCKED').length,
    critical:   allTasks.filter((t) => t.priority === 'CRITICAL').length,
  }), [allTasks]);

  const isOverdue = (t: RichTask) =>
    t.dueDate && t.status !== 'DONE' && new Date(t.dueDate) < new Date();

  // ── Render ──────────────────────────────────────────────────────────────────
  if (loading) return <LogoLoader message="Loading all tasks…" />;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto' }}>

      {/* ── Header ─────────────────────────────────────────── */}
      <div style={{ marginBottom: '1.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-full)', color: 'var(--primary)', fontSize: '0.78rem', fontWeight: 700, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
            WORK BREAKDOWN
          </div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', margin: 0 }}>
            All Tasks &amp; Milestones
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', marginTop: '0.25rem' }}>
            Cross-project task tracking across <strong>{projects.length}</strong> project{projects.length !== 1 ? 's' : ''}
          </p>
        </div>
        <button
          onClick={load}
          style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-sm)', color: 'var(--primary)', fontSize: '0.85rem', fontWeight: 600, cursor: 'pointer' }}
        >
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      {/* ── Error Banner ───────────────────────────────────── */}
      {error && (
        <div style={{ padding: '12px 16px', background: 'var(--accent-crimson-subtle)', border: '1px solid #fca5a5', borderRadius: 'var(--radius-sm)', color: 'var(--accent-crimson)', marginBottom: 20, fontSize: '0.9rem' }}>
          ⚠ {error}
        </div>
      )}

      {/* ── Stats Row ──────────────────────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 12, marginBottom: 24 }}>
        {[
          { label: 'Total Tasks',  value: stats.total,      icon: <Layers size={18} />,       color: 'var(--primary)',       bg: 'var(--primary-subtle)',         border: 'var(--primary-border)' },
          { label: 'Done',         value: stats.done,       icon: <CheckSquare size={18} />,  color: '#059669',              bg: '#ecfdf5',                       border: '#6ee7b7' },
          { label: 'In Progress',  value: stats.inProgress, icon: <RefreshCw size={18} />,    color: '#0284c7',              bg: '#f0f9ff',                       border: '#bae6fd' },
          { label: 'Blocked',      value: stats.blocked,    icon: <Ban size={18} />,           color: '#dc2626',              bg: '#fef2f2',                       border: '#fca5a5' },
          { label: 'Critical',     value: stats.critical,   icon: <AlertTriangle size={18} />, color: '#be123c',             bg: '#fff1f2',                       border: '#fda4af' },
        ].map((s) => (
          <div key={s.label} style={{ background: s.bg, border: `1px solid ${s.border}`, borderRadius: 'var(--radius-sm)', padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ color: s.color, display: 'flex' }}>{s.icon}</div>
            <div>
              <div style={{ fontSize: '1.5rem', fontWeight: 800, color: s.color, lineHeight: 1 }}>{s.value}</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600, marginTop: 2 }}>{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* ── Filters ────────────────────────────────────────── */}
      <div style={{ background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', padding: '14px 16px', marginBottom: 20, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
        <Filter size={15} style={{ color: 'var(--text-muted)', flexShrink: 0 }} />

        <input
          placeholder="Search tasks or projects…"
          value={searchQ}
          onChange={(e) => setSearchQ(e.target.value)}
          style={{ flex: '1 1 200px', padding: '7px 12px', fontSize: '0.88rem', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
        />

        <select
          value={filterStatus}
          onChange={(e) => setFilterStatus(e.target.value as TaskStatus | 'ALL')}
          style={{ padding: '7px 12px', fontSize: '0.88rem', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)', background: 'var(--bg-card)', color: 'var(--text-primary)', cursor: 'pointer' }}
        >
          <option value="ALL">All Statuses</option>
          <option value="TODO">To Do</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="BLOCKED">Blocked</option>
          <option value="DONE">Done</option>
        </select>

        <select
          value={filterPriority}
          onChange={(e) => setFilterPriority(e.target.value as TaskPriority | 'ALL')}
          style={{ padding: '7px 12px', fontSize: '0.88rem', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)', background: 'var(--bg-card)', color: 'var(--text-primary)', cursor: 'pointer' }}
        >
          <option value="ALL">All Priorities</option>
          <option value="CRITICAL">Critical</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>

        <select
          value={filterProject}
          onChange={(e) => setFilterProject(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
          style={{ padding: '7px 12px', fontSize: '0.88rem', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)', background: 'var(--bg-card)', color: 'var(--text-primary)', cursor: 'pointer', maxWidth: 200 }}
        >
          <option value="ALL">All Projects</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>

        {/* View Mode Toggle: List vs Kanban */}
        <div style={{ display: 'flex', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-medium)', overflow: 'hidden', marginLeft: 'auto' }}>
          <button
            onClick={() => setViewMode('list')}
            style={{
              padding: '6px 14px', border: 'none', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer',
              background: viewMode === 'list' ? 'var(--primary)' : 'var(--bg-card)',
              color: viewMode === 'list' ? '#fff' : 'var(--text-secondary)',
            }}
          >
            ☰ List View
          </button>
          <button
            onClick={() => setViewMode('kanban')}
            style={{
              padding: '6px 14px', border: 'none', fontSize: '0.84rem', fontWeight: 600, cursor: 'pointer',
              background: viewMode === 'kanban' ? 'var(--primary)' : 'var(--bg-card)',
              color: viewMode === 'kanban' ? '#fff' : 'var(--text-secondary)',
            }}
          >
            📋 Kanban Board
          </button>
        </div>

        {(filterStatus !== 'ALL' || filterPriority !== 'ALL' || filterProject !== 'ALL' || searchQ) && (
          <button
            onClick={() => { setFilterStatus('ALL'); setFilterPriority('ALL'); setFilterProject('ALL'); setSearchQ(''); }}
            style={{ padding: '7px 12px', fontSize: '0.82rem', fontWeight: 600, border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-xs)', background: 'var(--bg-card)', color: 'var(--accent-crimson)', cursor: 'pointer' }}
          >
            Clear Filters
          </button>
        )}
      </div>

      {/* ── Kanban Board View ───────────────────────────────── */}
      {viewMode === 'kanban' ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16, alignItems: 'start' }}>
          {(['TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'] as TaskStatus[]).map((colStatus) => {
            const colConfig = STATUS_CONFIG[colStatus];
            const colTasks = filtered.filter(t => t.status === colStatus);

            return (
              <div key={colStatus} style={{
                background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)',
                padding: '14px', display: 'flex', flexDirection: 'column', gap: 12, minHeight: 380,
              }}>
                {/* Column Header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: 8, borderBottom: '1px solid var(--border-light)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ color: colConfig.color }}>{colConfig.icon}</span>
                    <h4 style={{ margin: 0, fontSize: '0.92rem', fontWeight: 700, color: 'var(--text-primary)' }}>{colConfig.label}</h4>
                  </div>
                  <span style={{ fontSize: '0.76rem', fontWeight: 700, padding: '2px 8px', borderRadius: 99, background: colConfig.bg, color: colConfig.color }}>
                    {colTasks.length}
                  </span>
                </div>

                {/* Column Tasks */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {colTasks.length === 0 ? (
                    <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.8rem', border: '1px dashed var(--border-light)', borderRadius: 6 }}>
                      No tasks in {colConfig.label}
                    </div>
                  ) : (
                    colTasks.map(task => {
                      const pc = PRIORITY_CONFIG[task.priority];
                      const overdue = isOverdue(task);

                      return (
                        <div key={task.id} style={{
                          background: 'var(--bg-card)', borderRadius: 6, border: `1px solid ${overdue ? '#fca5a5' : 'var(--border-light)'}`,
                          borderLeft: `3px solid ${pc.dot}`, padding: 12, display: 'flex', flexDirection: 'column', gap: 8,
                          boxShadow: 'var(--shadow-sm)',
                        }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 6 }}>
                            <span style={{ fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)' }}>{task.title}</span>
                            <span style={{ fontSize: '0.68rem', fontWeight: 700, color: pc.color, padding: '1px 5px', borderRadius: 4, background: 'rgba(255,255,255,0.06)' }}>
                              {task.priority}
                            </span>
                          </div>

                          <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>
                            📁 {task.projectName}
                          </div>

                          {task.dueDate && (
                            <div style={{ fontSize: '0.74rem', color: overdue ? '#dc2626' : 'var(--text-secondary)' }}>
                              📅 Due: {task.dueDate} {overdue && '(Overdue)'}
                            </div>
                          )}

                          {/* Quick advance status buttons */}
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4, paddingTop: 6, borderTop: '1px solid var(--border-light)' }}>
                            <select
                              value={task.status}
                              onChange={(e) => handleStatus(task, e.target.value as TaskStatus)}
                              style={{ padding: '3px 6px', fontSize: '0.74rem', borderRadius: 4, border: '1px solid var(--border-light)', background: 'var(--bg-secondary)', color: 'var(--text-secondary)', cursor: 'pointer' }}
                            >
                              <option value="TODO">To Do</option>
                              <option value="IN_PROGRESS">In Progress</option>
                              <option value="BLOCKED">Blocked</option>
                              <option value="DONE">Done</option>
                            </select>

                            <Link to={`/projects/${task.projectId}`} style={{ fontSize: '0.72rem', color: 'var(--primary-light)', textDecoration: 'none', fontWeight: 600 }}>
                              Open →
                            </Link>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
      <>
      {allTasks.length === 0 && !error ? (
        <div style={{ padding: '3rem 2rem', textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)' }}>
          <CheckSquare size={40} style={{ color: 'var(--text-muted)', margin: '0 auto 1rem' }} />
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>No Tasks Yet</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
            Go to <strong>Projects</strong>, open a project and add tasks there.
          </p>
          <Link to="/projects" className="btn btn-primary" style={{ textDecoration: 'none', padding: '8px 20px', fontSize: '0.9rem', display: 'inline-block' }}>
            View Projects →
          </Link>
        </div>
      ) : filtered.length === 0 ? (
        <div style={{ padding: '2.5rem', textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', color: 'var(--text-secondary)' }}>
          No tasks match the current filters.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: 4 }}>
            Showing {filtered.length} of {allTasks.length} tasks
          </div>
          {filtered.map((task) => {
            const sc = STATUS_CONFIG[task.status];
            const pc = PRIORITY_CONFIG[task.priority];
            const overdue = isOverdue(task);
            const isUpdating = updatingId === task.id;

            return (
              <div
                key={task.id}
                style={{
                  background: 'var(--bg-card)',
                  border: `1px solid ${overdue ? '#fca5a5' : 'var(--border-light)'}`,
                  borderLeft: `4px solid ${pc.dot}`,
                  borderRadius: 'var(--radius-sm)',
                  padding: '14px 18px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 14,
                  flexWrap: 'wrap',
                  opacity: isUpdating ? 0.6 : 1,
                  transition: 'all 0.18s ease',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                {/* Progress ring / checkbox area */}
                <div style={{ flexShrink: 0, width: 38, height: 38, borderRadius: '50%', background: sc.bg, border: `2px solid ${sc.color}`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: sc.color }}>
                  {sc.icon}
                </div>

                {/* Title + project */}
                <div style={{ flex: '1 1 220px', minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <span style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>{task.title}</span>
                    {overdue && (
                      <span style={{ fontSize: '0.72rem', fontWeight: 700, color: '#dc2626', background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 4, padding: '1px 6px' }}>OVERDUE</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3 }}>
                    <Link
                      to={`/projects/${task.projectId}`}
                      style={{ fontSize: '0.78rem', color: 'var(--primary)', fontWeight: 600, textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 3 }}
                    >
                      {task.projectName} <ExternalLink size={10} />
                    </Link>
                    {task.dueDate && (
                      <span style={{ fontSize: '0.75rem', color: overdue ? '#dc2626' : 'var(--text-muted)', fontWeight: 500 }}>
                        · Due {task.dueDate}
                      </span>
                    )}
                  </div>
                </div>

                {/* Progress bar + input */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                  <div style={{ width: 70, height: 6, background: 'var(--border-light)', borderRadius: 99, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${task.progressPercentage}%`, background: task.progressPercentage === 100 ? '#34d399' : task.progressPercentage >= 50 ? '#38bdf8' : '#fb923c', borderRadius: 99, transition: 'width 0.3s ease' }} />
                  </div>
                  <input
                    type="number"
                    min={0} max={100}
                    value={task.progressPercentage}
                    disabled={isUpdating}
                    onChange={(e) => handleProgress(task, Number(e.target.value))}
                    style={{ width: 46, padding: '3px 5px', fontSize: '0.8rem', border: '1px solid var(--border-medium)', borderRadius: 5, background: 'var(--bg-secondary)', color: 'var(--text-primary)', textAlign: 'center' }}
                  />
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>%</span>
                </div>

                {/* Status select */}
                <select
                  value={task.status}
                  disabled={isUpdating}
                  onChange={(e) => handleStatus(task, e.target.value as TaskStatus)}
                  style={{ padding: '5px 10px', fontSize: '0.8rem', border: `1px solid ${sc.color}`, borderRadius: 'var(--radius-xs)', background: sc.bg, color: sc.color, fontWeight: 600, cursor: 'pointer', flexShrink: 0 }}
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="BLOCKED">Blocked</option>
                  <option value="DONE">Done</option>
                </select>

                {/* Priority select */}
                <select
                  value={task.priority}
                  disabled={isUpdating}
                  onChange={(e) => handlePriority(task, e.target.value as TaskPriority)}
                  style={{ padding: '5px 10px', fontSize: '0.8rem', border: `1px solid ${pc.dot}`, borderRadius: 'var(--radius-xs)', background: 'var(--bg-secondary)', color: pc.color, fontWeight: 700, cursor: 'pointer', flexShrink: 0 }}
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </select>
              </div>
            );
          })}
        </div>
      )}
      </>
      )}
    </div>
  );
}
