import { useState, useEffect, useCallback } from 'react';
import {
  Zap, X, TrendingDown, TrendingUp, DollarSign, Calendar,
  Users, Sparkles, Sliders, CheckCircle2, RotateCcw, ArrowRight,
  Scissors, Check
} from 'lucide-react';
import { simulationApi, SimulationResult, TaskSimulationOverride } from '../services/api';

interface WhatIfSimulatorModalProps {
  projectId: number;
  projectName: string;
  isOpen: boolean;
  onClose: () => void;
  onApplied?: () => void;
}

export default function WhatIfSimulatorModalComponent({
  projectId, projectName, isOpen, onClose, onApplied,
}: WhatIfSimulatorModalProps) {
  const [devDelta, setDevDelta]             = useState(1);
  const [prodMultiplier, setProdMult]       = useState(1.0);
  const [hourlyRate, setHourlyRate]         = useState(650);
  const [taskOverrides, setTaskOverrides]   = useState<Record<number, { duration?: number; excluded?: boolean }>>({});
  const [result, setResult]                 = useState<SimulationResult | null>(null);
  const [loading, setLoading]               = useState(false);
  const [applying, setApplying]             = useState(false);
  const [appliedMsg, setAppliedMsg]         = useState<string | null>(null);
  const [error, setError]                   = useState<string | null>(null);

  const runSimulation = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const overridesList: TaskSimulationOverride[] = Object.entries(taskOverrides).map(([idStr, val]) => ({
        taskId: Number(idStr),
        newDurationDays: val.duration,
        excludeFromScope: val.excluded,
      }));

      const data = await simulationApi.simulate(projectId, {
        developerDelta: devDelta,
        developerHourlyRate: hourlyRate,
        productivityMultiplier: prodMultiplier,
        taskOverrides: overridesList.length > 0 ? overridesList : undefined,
      });
      setResult(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Simulation failed.');
    } finally {
      setLoading(false);
    }
  }, [projectId, devDelta, prodMultiplier, hourlyRate, taskOverrides]);

  useEffect(() => {
    if (isOpen && projectId) {
      runSimulation();
    }
  }, [isOpen, projectId, devDelta, prodMultiplier, hourlyRate, taskOverrides, runSimulation]);

  const handleApplyScenario = async () => {
    if (!window.confirm('Apply this scenario to live project tasks? Task durations and critical schedules will be updated.')) {
      return;
    }
    setApplying(true);
    setError(null);
    try {
      const overridesList: TaskSimulationOverride[] = Object.entries(taskOverrides).map(([idStr, val]) => ({
        taskId: Number(idStr),
        newDurationDays: val.duration,
        excludeFromScope: val.excluded,
      }));

      const res = await simulationApi.apply(projectId, {
        developerDelta: devDelta,
        developerHourlyRate: hourlyRate,
        productivityMultiplier: prodMultiplier,
        taskOverrides: overridesList.length > 0 ? overridesList : undefined,
      });
      setAppliedMsg(res.message);
      if (onApplied) onApplied();
      setTimeout(() => {
        setAppliedMsg(null);
        onClose();
      }, 2500);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to apply scenario to project.');
    } finally {
      setApplying(false);
    }
  };

  const handleReset = () => {
    setDevDelta(0);
    setProdMult(1.0);
    setHourlyRate(650);
    setTaskOverrides({});
  };

  const toggleTaskExclude = (taskId: number) => {
    setTaskOverrides(prev => {
      const current = prev[taskId] || {};
      return {
        ...prev,
        [taskId]: { ...current, excluded: !current.excluded },
      };
    });
  };

  const updateTaskDuration = (taskId: number, newDur: number) => {
    setTaskOverrides(prev => {
      const current = prev[taskId] || {};
      return {
        ...prev,
        [taskId]: { ...current, duration: Math.max(1, newDur) },
      };
    });
  };

  if (!isOpen) return null;

  const getFeasibilityBadge = (status?: string) => {
    switch (status) {
      case 'OPTIMAL':
        return { bg: '#ecfdf5', text: '#059669', border: '#a7f3d0', label: 'Optimal Strategy' };
      case 'HIGH_RISK':
        return { bg: '#fef2f2', text: '#dc2626', border: '#fca5a5', label: 'High Schedule Risk' };
      case 'DIMINISHING_RETURNS':
        return { bg: '#fffbeb', text: '#d97706', border: '#fcd34d', label: "Brooks' Law Diminishing Returns" };
      default:
        return { bg: '#eff6ff', text: '#2563eb', border: '#bfdbfe', label: 'Feasible Scenario' };
    }
  };

  const badge = getFeasibilityBadge(result?.feasibilityAssessment);

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(15, 23, 42, 0.75)', backdropFilter: 'blur(8px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem',
    }}>
      <div style={{
        background: 'var(--bg-card)', width: '100%', maxWidth: '960px', maxHeight: '92vh',
        borderRadius: 'var(--radius-lg)', boxShadow: '0 25px 50px -12px rgba(0,0,0,0.35)',
        border: '1px solid var(--border-medium)', display: 'flex', flexDirection: 'column',
        overflow: 'hidden', animation: 'fadeInScale 0.25s ease',
      }}>

        {/* ── Header ── */}
        <div style={{
          padding: '1.25rem 1.75rem', borderBottom: '1px solid var(--border-light)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: 'linear-gradient(90deg, var(--bg-card), var(--bg-secondary))',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 38, height: 38, borderRadius: 'var(--radius-sm)',
              background: 'linear-gradient(135deg, #3b82f6, #06b6d4)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
              boxShadow: '0 4px 12px rgba(59, 130, 246, 0.35)',
            }}>
              <Zap size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.18rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}>
                What-If Scenario Simulator & Sandbox
                <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '2px 8px', borderRadius: 99, background: badge.bg, color: badge.text, border: `1px solid ${badge.border}` }}>
                  {badge.label}
                </span>
              </h3>
              <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                Simulating: <strong>{projectName}</strong> · In-Memory CPM Sandbox & Sensitivity Analysis
              </p>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button
              onClick={handleReset}
              title="Reset sliders and task overrides"
              style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '5px 10px', fontSize: '0.75rem', fontWeight: 600, background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-xs)', color: 'var(--text-secondary)', cursor: 'pointer' }}
            >
              <RotateCcw size={12} /> Reset
            </button>
            <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 4 }}>
              <X size={20} />
            </button>
          </div>
        </div>

        {/* ── Success / Applied Banner ── */}
        {appliedMsg && (
          <div style={{ padding: '12px 1.75rem', background: '#ecfdf5', borderBottom: '1px solid #6ee7b7', color: '#059669', fontSize: '0.88rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CheckCircle2 size={18} /> {appliedMsg}
          </div>
        )}

        {/* ── Error Banner ── */}
        {error && (
          <div style={{ padding: '10px 1.75rem', background: '#fef2f2', borderBottom: '1px solid #fca5a5', color: '#dc2626', fontSize: '0.84rem' }}>
            {error}
          </div>
        )}

        {/* ── Body ── */}
        <div style={{ padding: '1.5rem 1.75rem', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: 18 }}>

          {/* ── Interactive Scenario Controls ── */}
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14,
            padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)',
          }}>
            {/* Developer Headcount Delta */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>
                <span><Users size={13} style={{ display: 'inline', marginRight: 4 }} /> Team Delta</span>
                <span style={{ color: devDelta > 0 ? '#059669' : (devDelta < 0 ? '#dc2626' : 'var(--text-secondary)') }}>
                  {devDelta > 0 ? `+${devDelta} Developers` : (devDelta < 0 ? `${devDelta} Developers` : '0 (Baseline)')}
                </span>
              </div>
              <input
                type="range" min={-2} max={6} step={1} value={devDelta}
                onChange={e => setDevDelta(Number(e.target.value))}
                style={{ width: '100%', accentColor: '#3b82f6' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>
                <span>-2 (Downsize)</span>
                <span>0 (Baseline)</span>
                <span>+6 (Max Scaled)</span>
              </div>
            </div>

            {/* Velocity / Productivity Multiplier */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>
                <span><Sliders size={13} style={{ display: 'inline', marginRight: 4 }} /> Velocity Rate</span>
                <span style={{ color: '#3b82f6' }}>{prodMultiplier.toFixed(1)}x Speed</span>
              </div>
              <input
                type="range" min={0.7} max={1.4} step={0.1} value={prodMultiplier}
                onChange={e => setProdMult(Number(e.target.value))}
                style={{ width: '100%', accentColor: '#3b82f6' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>
                <span>0.7x (Friction)</span>
                <span>1.0x (Normal)</span>
                <span>1.4x (High Flow)</span>
              </div>
            </div>

            {/* Developer Hourly Rate */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>
                <span><DollarSign size={13} style={{ display: 'inline', marginRight: 4 }} /> Dev Hourly Rate</span>
                <span style={{ color: 'var(--text-primary)' }}>₹{hourlyRate}/hr</span>
              </div>
              <input
                type="range" min={300} max={1500} step={50} value={hourlyRate}
                onChange={e => setHourlyRate(Number(e.target.value))}
                style={{ width: '100%', accentColor: '#3b82f6' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>
                <span>₹300/hr</span>
                <span>₹650/hr</span>
                <span>₹1,500/hr</span>
              </div>
            </div>
          </div>

          {/* ── Key Impact Comparison Cards ── */}
          {result && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
              {/* Delivery Timeline Delta */}
              <div style={{ padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                  <Calendar size={12} style={{ display: 'inline', marginRight: 4 }} /> Critical Duration
                </div>
                <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                  {result.simulatedDurationDays} Days
                </div>
                <div style={{ fontSize: '0.78rem', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4, color: result.durationDeltaDays <= 0 ? '#059669' : '#dc2626', fontWeight: 700 }}>
                  {result.durationDeltaDays <= 0 ? <TrendingDown size={14} /> : <TrendingUp size={14} />}
                  {result.durationDeltaDays <= 0 ? `${Math.abs(result.durationDeltaDays)} days accelerated` : `+${result.durationDeltaDays} days delay`}
                  <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>vs baseline ({result.baselineDurationDays}d)</span>
                </div>
              </div>

              {/* Finish Date */}
              <div style={{ padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                  🏁 Target Finish Date
                </div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                  {result.simulatedFinishDate}
                </div>
                <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', marginTop: 4 }}>
                  Baseline Finish: <strong>{result.baselineFinishDate}</strong>
                </div>
              </div>

              {/* Financial Impact */}
              <div style={{ padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                  <DollarSign size={12} style={{ display: 'inline', marginRight: 4 }} /> Total Estimated Cost
                </div>
                <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                  ₹{Number(result.simulatedEstimatedCost).toLocaleString()}
                </div>
                <div style={{ fontSize: '0.78rem', marginTop: 4, color: result.costDelta >= 0 ? '#b91c1c' : '#059669', fontWeight: 700 }}>
                  {result.costDelta >= 0 ? `+₹${Math.abs(result.costDelta).toLocaleString()}` : `-₹${Math.abs(result.costDelta).toLocaleString()}`}
                  <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}> cost variance</span>
                </div>
              </div>
            </div>
          )}

          {/* ── Prescriptive AI Recommendations ── */}
          {result?.prescriptiveRecommendations && result.prescriptiveRecommendations.length > 0 && (
            <div style={{
              padding: '14px 16px', borderRadius: 'var(--radius-sm)',
              background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(6, 182, 212, 0.05))',
              border: '1px solid rgba(59, 130, 246, 0.25)',
            }}>
              <div style={{ fontSize: '0.78rem', fontWeight: 800, color: '#2563eb', display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
                <Sparkles size={14} /> AI Prescriptive Insights & Trade-Off Analysis
              </div>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: '0.82rem', color: 'var(--text-primary)', lineHeight: 1.6 }}>
                {result.prescriptiveRecommendations.map((r, i) => (
                  <li key={i}>{r}</li>
                ))}
              </ul>
            </div>
          )}

          {/* ── Interactive Task Overrides & Descoping Table ── */}
          {result?.simulatedTasks && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  📊 Sandbox Task Duration & Scope Adjustments ({result.simulatedTasks.length} Tasks)
                </div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>
                  Tip: Toggle <strong>✂️ Descope</strong> or edit duration to simulate immediate critical path shifts.
                </div>
              </div>
              <div style={{ border: '1px solid var(--border-light)', borderRadius: 6, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
                  <thead>
                    <tr style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border-light)', color: 'var(--text-secondary)', textAlign: 'left' }}>
                      <th style={{ padding: '8px 10px' }}>Task Name</th>
                      <th style={{ padding: '8px 10px', width: 130 }}>Simulated Duration</th>
                      <th style={{ padding: '8px 10px', width: 100 }}>Start</th>
                      <th style={{ padding: '8px 10px', width: 100 }}>End</th>
                      <th style={{ padding: '8px 10px', width: 95 }}>CPM Status</th>
                      <th style={{ padding: '8px 10px', width: 100, textAlign: 'center' }}>Scope Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.simulatedTasks.map(t => {
                      const isExcluded = taskOverrides[t.id]?.excluded;
                      return (
                        <tr key={t.id} style={{ borderBottom: '1px solid var(--border-light)', background: isExcluded ? 'rgba(148, 163, 184, 0.1)' : t.isCritical ? 'rgba(239, 68, 68, 0.04)' : 'transparent' }}>
                          <td style={{ padding: '8px 10px', fontWeight: t.isCritical ? 700 : 500, color: isExcluded ? 'var(--text-muted)' : 'var(--text-primary)', textDecoration: isExcluded ? 'line-through' : 'none' }}>
                            {t.name}
                          </td>
                          <td style={{ padding: '8px 10px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                              <input
                                type="number" min={1} max={90}
                                value={t.durationDays}
                                onChange={e => updateTaskDuration(t.id, Number(e.target.value))}
                                disabled={isExcluded}
                                style={{ width: 48, padding: '2px 4px', fontSize: '0.78rem', borderRadius: 4, border: '1px solid var(--border-medium)', background: 'var(--bg-card)' }}
                              />
                              <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)' }}>days</span>
                            </div>
                          </td>
                          <td style={{ padding: '8px 10px', color: 'var(--text-muted)' }}>{t.scheduledStart}</td>
                          <td style={{ padding: '8px 10px', color: 'var(--text-muted)' }}>{t.scheduledEnd}</td>
                          <td style={{ padding: '8px 10px' }}>
                            {isExcluded ? (
                              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', fontWeight: 600 }}>DESCOPED</span>
                            ) : t.isCritical ? (
                              <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 7px', borderRadius: 99, background: '#fee2e2', color: '#b91c1c' }}>
                                CRITICAL
                              </span>
                            ) : (
                              <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Float Slack</span>
                            )}
                          </td>
                          <td style={{ padding: '8px 10px', textAlign: 'center' }}>
                            <button
                              onClick={() => toggleTaskExclude(t.id)}
                              style={{
                                display: 'inline-flex', alignItems: 'center', gap: 3, padding: '2px 8px', fontSize: '0.72rem',
                                borderRadius: 4, border: '1px solid', cursor: 'pointer', fontWeight: 600,
                                background: isExcluded ? '#ecfdf5' : '#fef2f2',
                                borderColor: isExcluded ? '#a7f3d0' : '#fca5a5',
                                color: isExcluded ? '#059669' : '#dc2626',
                              }}
                            >
                              {isExcluded ? <Check size={11} /> : <Scissors size={11} />}
                              {isExcluded ? 'Include' : 'Descope'}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>

        {/* ── Footer ── */}
        <div style={{
          padding: '12px 1.75rem', borderTop: '1px solid var(--border-light)',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10, background: 'var(--bg-secondary)',
        }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            {loading ? 'Recalculating scenario sandbox...' : 'Changes here are sandboxed in-memory until applied.'}
          </div>
          <div style={{ display: 'flex', gap: 10 }}>
            <button
              onClick={onClose}
              style={{ padding: '8px 18px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-medium)', background: 'var(--bg-card)', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer' }}
            >
              Close Sandbox
            </button>
            <button
              onClick={handleApplyScenario}
              disabled={applying || loading || !result}
              style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '8px 20px', borderRadius: 'var(--radius-xs)', border: 'none',
                background: 'linear-gradient(135deg, #2563eb, #06b6d4)', color: '#fff',
                fontWeight: 700, fontSize: '0.85rem', cursor: (applying || loading) ? 'wait' : 'pointer',
                boxShadow: '0 4px 14px rgba(37, 99, 235, 0.35)',
              }}
            >
              {applying ? 'Applying...' : <><Zap size={14} /> Apply to Live Project <ArrowRight size={14} /></>}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export { WhatIfSimulatorModalComponent as WhatIfSimulatorModal };
