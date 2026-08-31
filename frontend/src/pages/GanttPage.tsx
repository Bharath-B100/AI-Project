import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ViewMode } from 'gantt-task-react';
import GanttChart from '../components/GanttChart';
import DependencyManager from '../components/DependencyManager';
import CriticalPathView from '../components/CriticalPathView';
import { WhatIfSimulatorModal } from '../components/WhatIfSimulatorModal';
import {
  projectApi, taskApi, dependencyApi, scheduleApi,
  Project, Task, Dependency, GanttData, CriticalPath,
} from '../services/api';
import LogoLoader from '../components/LogoLoader';
import { Zap } from 'lucide-react';

type Tab = 'gantt' | 'dependencies' | 'critical-path';

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: 'gantt',         label: 'Gantt Chart',    icon: '📊' },
  { id: 'dependencies',  label: 'Dependencies',   icon: '🔗' },
  { id: 'critical-path', label: 'Critical Path',  icon: '🎯' },
];

const VIEW_MODES: { mode: ViewMode; label: string }[] = [
  { mode: ViewMode.Day,   label: 'Day'   },
  { mode: ViewMode.Week,  label: 'Week'  },
  { mode: ViewMode.Month, label: 'Month' },
];

export default function GanttPage() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);

  const [activeTab, setActiveTab] = useState<Tab>('gantt');
  const [viewMode, setViewMode] = useState<ViewMode>(ViewMode.Week);

  const [project, setProject]         = useState<Project | null>(null);
  const [tasks, setTasks]             = useState<Task[]>([]);
  const [dependencies, setDependencies] = useState<Dependency[]>([]);
  const [ganttData, setGanttData]     = useState<GanttData | null>(null);
  const [criticalPath, setCriticalPath] = useState<CriticalPath | null>(null);

  const [loadingGantt, setLoadingGantt]   = useState(false);
  const [calculating, setCalculating]     = useState(false);
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);
  const [selectedTask, setSelectedTask]   = useState<string | null>(null);
  const [error, setError]                 = useState('');
  const [calcResult, setCalcResult]       = useState<string | null>(null);

  // ── Data loading ───────────────────────────────────────────────────────────
  const loadProject = useCallback(async () => {
    const p = await projectApi.get(projectId);
    setProject(p);
  }, [projectId]);

  const loadTasks = useCallback(async () => {
    const t = await taskApi.list(projectId);
    setTasks(t);
  }, [projectId]);

  const loadDependencies = useCallback(async () => {
    const d = await dependencyApi.list(projectId);
    setDependencies(d);
  }, [projectId]);

  const loadGantt = useCallback(async () => {
    setLoadingGantt(true);
    setError('');
    try {
      const [g, cp] = await Promise.all([
        scheduleApi.getGantt(projectId),
        scheduleApi.getCriticalPath(projectId),
      ]);
      setGanttData(g);
      setCriticalPath(cp);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Could not load schedule data.');
    } finally {
      setLoadingGantt(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadProject();
    loadTasks();
    loadDependencies();
    loadGantt();
  }, [projectId]);

  // ── Actions ────────────────────────────────────────────────────────────────
  const handleCalculate = async () => {
    setCalculating(true);
    setError('');
    setCalcResult(null);
    try {
      const result = await scheduleApi.calculate(projectId);
      setCalcResult(
        `✅ Schedule recalculated. Critical path: ${result.criticalPath.length} task(s), ${result.totalDurationDays} days total.`
      );
      await loadGantt();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Schedule calculation failed.');
    } finally {
      setCalculating(false);
    }
  };

  const handleDepsChanged = async () => {
    await loadDependencies();
    await loadGantt();
  };

  // ── Styles ─────────────────────────────────────────────────────────────────
  const tabBtnStyle = (active: boolean): React.CSSProperties => ({
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '10px 20px',
    borderRadius: 'var(--radius-sm)',
    border: active ? '1px solid var(--primary-border)' : '1px solid transparent',
    background: active ? 'var(--primary-subtle)' : 'transparent',
    color: active ? 'var(--primary-light)' : 'var(--text-secondary)',
    fontSize: '0.92rem',
    fontWeight: active ? 700 : 500,
    cursor: 'pointer',
    transition: 'var(--transition-smooth)',
    boxShadow: active ? '0 0 15px rgba(9, 132, 227, 0.15)' : 'none'
  });

  const viewModeBtn = (active: boolean): React.CSSProperties => ({
    padding: '6px 14px',
    borderRadius: 'var(--radius-xs)',
    border: active ? '1px solid var(--primary-border)' : '1px solid var(--border-light)',
    background: active ? 'var(--primary-subtle)' : 'transparent',
    color: active ? 'var(--primary-light)' : 'var(--text-secondary)',
    cursor: 'pointer',
    fontSize: '0.85rem',
    fontWeight: active ? 600 : 400,
    transition: 'var(--transition-smooth)',
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: '1280px', margin: '0 auto' }}>
      {/* What-If Simulator Modal */}
      {project && (
        <WhatIfSimulatorModal
          projectId={projectId}
          projectName={project.name}
          isOpen={isSimulatorOpen}
          onClose={() => setIsSimulatorOpen(false)}
        />
      )}

      {/* Breadcrumb */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.9rem' }}>
        <Link to="/projects" style={{ color: 'var(--primary-light)', textDecoration: 'none', fontWeight: 500 }}>
          Projects
        </Link>
        <span style={{ color: 'var(--text-muted)' }}>/</span>
        <Link to={`/projects/${projectId}`} style={{ color: 'var(--text-secondary)', textDecoration: 'none' }}>
          {project?.name || 'Project'}
        </Link>
        <span style={{ color: 'var(--text-muted)' }}>/</span>
        <span style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Gantt &amp; Schedule</span>
      </div>

      {/* Header */}
      <div className="glass-panel" style={{ padding: '24px 28px', borderColor: 'var(--primary-border)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
              <h2 style={{ fontSize: '1.65rem', fontWeight: 800, margin: 0, letterSpacing: '-0.5px' }}>
                {project?.name ?? 'Loading…'}
              </h2>
              <span className="badge badge-primary">Interactive CPM</span>
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', marginTop: 4 }}>
              {ganttData
                ? `${ganttData.tasks.length} task(s) · ${ganttData.criticalPath.length} on critical path · ${project?.startDate ?? 'No start date'} → ${ganttData.projectEnd}`
                : 'Calculate schedule to generate Gantt chart and critical path analysis.'}
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <button
              onClick={() => setIsSimulatorOpen(true)}
              style={{
                fontSize: '0.88rem', padding: '10px 18px', borderRadius: 'var(--radius-sm)',
                background: 'linear-gradient(135deg, #3b82f6, #06b6d4)', color: '#fff',
                border: 'none', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 7,
                boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)',
              }}
            >
              <Zap size={15} /> 🔮 What-If Simulator
            </button>
            <button
              id="calculate-schedule-btn"
              className="btn btn-coral"
              onClick={handleCalculate}
              disabled={calculating}
              style={{ fontSize: '0.92rem', padding: '10px 22px' }}
            >
              {calculating ? '⏳ Calculating CPM…' : '⚡ Calculate Schedule'}
            </button>
          </div>
        </div>

        {/* Result / error banners */}
        {calcResult && (
          <div style={{
            marginTop: 16, padding: '12px 16px', borderRadius: 'var(--radius-sm)',
            background: 'var(--accent-emerald-subtle)', border: '1px solid rgba(0, 184, 148, 0.3)',
            color: 'var(--accent-emerald)', fontSize: '0.9rem', fontWeight: 500
          }}>
            {calcResult}
          </div>
        )}
        {error && (
          <div style={{
            marginTop: 16, padding: '12px 16px', borderRadius: 'var(--radius-sm)',
            background: 'var(--accent-crimson-subtle)', border: '1px solid rgba(214, 48, 49, 0.3)',
            color: '#ff7675', fontSize: '0.9rem', fontWeight: 500
          }}>
            ⚠️ {error}
          </div>
        )}
      </div>

      {/* Legend */}
      <div style={{ display: 'flex', gap: 24, fontSize: '0.85rem', color: 'var(--text-secondary)', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 16, height: 10, borderRadius: 3, background: 'var(--primary)', boxShadow: '0 0 8px var(--primary-glow)' }} />
          <span>Regular task (Scheduled)</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 16, height: 10, borderRadius: 3, background: 'var(--secondary)', boxShadow: '0 0 8px var(--secondary-glow)' }} />
          <span>Critical path task (Zero float / Drives deadline)</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{ width: 16, height: 10, borderRadius: 3, background: 'rgba(9, 132, 227, 0.25)', border: '1px solid var(--primary-border)' }} />
          <span>Current date reference</span>
        </div>
      </div>

      {/* Tab bar */}
      <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--border-light)', paddingBottom: 0 }}>
        {TABS.map((tab) => (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            style={tabBtnStyle(activeTab === tab.id)}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.icon} {tab.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="glass-panel" style={{ padding: 24, minHeight: 400 }}>
        {/* ── GANTT TAB ── */}
        {activeTab === 'gantt' && (
          <>
            {/* Zoom controls */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 4, marginBottom: 16 }}>
              <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginRight: 8, alignSelf: 'center' }}>
                Zoom:
              </span>
              {VIEW_MODES.map(({ mode, label }) => (
                <button
                  key={mode}
                  style={viewModeBtn(viewMode === mode)}
                  onClick={() => setViewMode(mode)}
                >
                  {label}
                </button>
              ))}
            </div>

            {loadingGantt ? (
              <LogoLoader message="Loading Gantt chart..." />
            ) : (
              <>
                <GanttChart
                  tasks={ganttData?.tasks ?? []}
                  viewMode={viewMode}
                  onTaskClick={(t) => setSelectedTask(String(t.id))}
                />
                {selectedTask && (
                  <div style={{
                    marginTop: 16, padding: '10px 16px', borderRadius: 8,
                    background: 'rgba(99,102,241,0.08)', border: '1px solid var(--border-glow)',
                    fontSize: '0.88rem', color: 'var(--text-secondary)',
                  }}>
                    Selected task ID: <strong style={{ color: 'var(--text-primary)' }}>{selectedTask}</strong>
                    &nbsp;·&nbsp;
                    <button
                      onClick={() => setSelectedTask(null)}
                      style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '0.85rem' }}
                    >
                      Clear
                    </button>
                  </div>
                )}
              </>
            )}
          </>
        )}

        {/* ── DEPENDENCIES TAB ── */}
        {activeTab === 'dependencies' && (
          <DependencyManager
            projectId={projectId}
            tasks={tasks}
            dependencies={dependencies}
            onDependenciesChange={handleDepsChanged}
          />
        )}

        {/* ── CRITICAL PATH TAB ── */}
        {activeTab === 'critical-path' && (
          <CriticalPathView
            criticalPath={criticalPath}
            loading={loadingGantt}
          />
        )}
      </div>
    </div>
  );
}
