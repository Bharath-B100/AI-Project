import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ViewMode } from 'gantt-task-react';
import GanttChart from '../components/GanttChart';
import DependencyManager from '../components/DependencyManager';
import CriticalPathView from '../components/CriticalPathView';
import { WhatIfSimulatorModal } from '../components/WhatIfSimulatorModal';
import {
  projectApi, taskApi, dependencyApi, scheduleApi, planningApi,
  Project, Task, Dependency, GanttData, CriticalPath, SuggestedDependency, AutoLevelResponse,
} from '../services/api';
import LogoLoader from '../components/LogoLoader';
import { Zap, X } from 'lucide-react';

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
  const [isLeveling, setIsLeveling]       = useState(false);
  const [autoLevelResult, setAutoLevelResult] = useState<AutoLevelResponse | null>(null);
  const [suggestions, setSuggestions]     = useState<SuggestedDependency[] | null>(null);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);
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

  const handleAutoLevel = async () => {
    setIsLeveling(true);
    setError('');
    try {
      const leveled = await scheduleApi.autoLevel(projectId);
      setAutoLevelResult(leveled);
      setCalcResult(
        `⚡ Auto-Leveling complete: ${leveled.resolvedResourceConflicts} bottleneck conflict(s) resolved. Project completion: ${leveled.leveledProjectEnd}.`
      );
      await loadGantt();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Auto-leveling schedule failed.');
    } finally {
      setIsLeveling(false);
    }
  };

  const handleSuggestDependencies = async () => {
    setLoadingSuggestions(true);
    setError('');
    try {
      const suggs = await planningApi.suggestDependencies(projectId);
      setSuggestions(suggs);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to analyze task dependencies.');
    } finally {
      setLoadingSuggestions(false);
    }
  };

  const handleApplySuggestedDep = async (s: SuggestedDependency) => {
    try {
      await dependencyApi.create(projectId, s.successorTaskId, {
        predecessorTaskId: s.predecessorTaskId,
        dependencyType: 'FINISH_TO_START',
        lagDays: 0,
      });
      setSuggestions(prev => prev ? prev.filter(x => !(x.predecessorTaskId === s.predecessorTaskId && x.successorTaskId === s.successorTaskId)) : null);
      await handleDepsChanged();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create dependency.');
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
          onApplied={() => { loadGantt(); loadTasks(); }}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
            <button
              onClick={handleAutoLevel}
              disabled={isLeveling}
              style={{
                fontSize: '0.88rem', padding: '10px 18px', borderRadius: 'var(--radius-sm)',
                background: 'linear-gradient(135deg, #10b981, #059669)', color: '#fff',
                border: 'none', fontWeight: 700, cursor: isLeveling ? 'wait' : 'pointer', display: 'flex', alignItems: 'center', gap: 7,
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
              }}
              title="Resolve resource bottlenecks and auto-stagger concurrent tasks"
            >
              <Zap size={15} /> {isLeveling ? 'Leveling…' : '⚡ AI Auto-Level'}
            </button>
            <button
              onClick={handleSuggestDependencies}
              disabled={loadingSuggestions}
              style={{
                fontSize: '0.88rem', padding: '10px 18px', borderRadius: 'var(--radius-sm)',
                background: 'linear-gradient(135deg, #8b5cf6, #6366f1)', color: '#fff',
                border: 'none', fontWeight: 700, cursor: loadingSuggestions ? 'wait' : 'pointer', display: 'flex', alignItems: 'center', gap: 7,
                boxShadow: '0 4px 12px rgba(139, 92, 246, 0.3)',
              }}
              title="Infer missing dependencies using AI heuristics"
            >
              <span>🤖</span> {loadingSuggestions ? 'Analyzing…' : 'AI Suggest Links'}
            </button>
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

      {/* ── AI Suggested Dependencies Modal ── */}
      {suggestions && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
          <div style={{ background: 'var(--bg-card)', borderRadius: 14, width: '100%', maxWidth: 680, maxHeight: '85vh', overflow: 'auto', boxShadow: '0 24px 64px rgba(0,0,0,0.4)', border: '1px solid var(--border-medium)' }}>
            <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'linear-gradient(135deg, #8b5cf6, #6366f1)', color: '#fff', borderRadius: '14px 14px 0 0' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800 }}>🤖 AI Recommended Task Dependencies</h3>
                <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: 'rgba(255,255,255,0.8)' }}>
                  Inferred from task titles, phase sequences, and architectural prerequisites
                </p>
              </div>
              <button onClick={() => setSuggestions(null)} style={{ background: 'rgba(255,255,255,0.2)', border: 'none', borderRadius: 6, color: '#fff', padding: 6, cursor: 'pointer' }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ padding: '16px 20px' }}>
              {suggestions.length === 0 ? (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
                  ✅ No new missing dependencies detected. Your project network is already well linked!
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {suggestions.map((s, i) => (
                    <div key={i} style={{ padding: '12px 14px', borderRadius: 8, background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span>{s.predecessorTitle}</span>
                          <span style={{ color: '#6366f1', fontSize: '0.75rem', fontWeight: 800 }}>➔ Finish-to-Start ➔</span>
                          <span>{s.successorTitle}</span>
                        </div>
                        <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', marginTop: 4 }}>{s.rationale}</div>
                        <div style={{ fontSize: '0.7rem', color: '#059669', fontWeight: 700, marginTop: 2 }}>Confidence: {(s.confidenceScore * 100).toFixed(0)}%</div>
                      </div>
                      <button
                        onClick={() => handleApplySuggestedDep(s)}
                        style={{
                          padding: '6px 14px', borderRadius: 6, border: 'none',
                          background: 'linear-gradient(135deg, #10b981, #059669)', color: '#fff',
                          fontWeight: 700, fontSize: '0.78rem', cursor: 'pointer', flexShrink: 0,
                        }}
                      >
                        + Add Link
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Auto-Leveling Results Dialog ── */}
      {autoLevelResult && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
          <div style={{ background: 'var(--bg-card)', borderRadius: 14, width: '100%', maxWidth: 640, maxHeight: '85vh', overflow: 'auto', boxShadow: '0 24px 64px rgba(0,0,0,0.4)', border: '1px solid var(--border-medium)' }}>
            <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: 'linear-gradient(135deg, #10b981, #059669)', color: '#fff', borderRadius: '14px 14px 0 0' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 800 }}>⚡ Resource-Constrained Auto-Leveling</h3>
                <p style={{ margin: '2px 0 0', fontSize: '0.78rem', color: 'rgba(255,255,255,0.8)' }}>
                  Optimized timeline with concurrent bottleneck resolution
                </p>
              </div>
              <button onClick={() => setAutoLevelResult(null)} style={{ background: 'rgba(255,255,255,0.2)', border: 'none', borderRadius: 6, color: '#fff', padding: 6, cursor: 'pointer' }}>
                <X size={16} />
              </button>
            </div>
            <div style={{ padding: '18px 20px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 10, marginBottom: 16 }}>
                <div style={{ background: 'var(--bg-secondary)', padding: '10px', borderRadius: 8, textAlign: 'center' }}>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#6366f1' }}>{autoLevelResult.resolvedResourceConflicts}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Bottlenecks Resolved</div>
                </div>
                <div style={{ background: 'var(--bg-secondary)', padding: '10px', borderRadius: 8, textAlign: 'center' }}>
                  <div style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-primary)' }}>{autoLevelResult.leveledProjectEnd}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Leveled Completion</div>
                </div>
                <div style={{ background: 'var(--bg-secondary)', padding: '10px', borderRadius: 8, textAlign: 'center' }}>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: autoLevelResult.delayOrSavedDays <= 0 ? '#059669' : '#ea580c' }}>
                    {autoLevelResult.delayOrSavedDays > 0 ? `+${autoLevelResult.delayOrSavedDays}d` : `${autoLevelResult.delayOrSavedDays}d`}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Schedule Shift</div>
                </div>
              </div>
              <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>📋 Leveling Adjustment Log:</div>
              <div style={{ background: 'var(--bg-secondary)', padding: '12px 14px', borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {autoLevelResult.levelingLog.map((log, i) => (
                  <div key={i} style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                    {log}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
