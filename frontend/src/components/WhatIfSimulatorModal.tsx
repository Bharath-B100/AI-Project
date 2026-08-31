import { useState, useEffect } from 'react';
import {
  Zap, X, TrendingDown, TrendingUp, DollarSign, Calendar,
  Users, Sparkles, Sliders,
} from 'lucide-react';
import { simulationApi, SimulationResult } from '../services/api';

interface WhatIfSimulatorModalProps {
  projectId: number;
  projectName: string;
  isOpen: boolean;
  onClose: () => void;
}

export default function WhatIfSimulatorModalComponent({
  projectId, projectName, isOpen, onClose,
}: WhatIfSimulatorModalProps) {
  const [devDelta, setDevDelta]         = useState(1);
  const [prodMultiplier, setProdMult]   = useState(1.0);
  const [result, setResult]             = useState<SimulationResult | null>(null);
  const [error, setError]               = useState<string | null>(null);

  const runSimulation = async () => {
    setError(null);
    try {
      const data = await simulationApi.simulate(projectId, {
        developerDelta: devDelta,
        developerHourlyRate: 500,
        productivityMultiplier: prodMultiplier,
      });
      setResult(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Simulation failed.');
    }
  };

  useEffect(() => {
    if (isOpen && projectId) {
      runSimulation();
    }
  }, [isOpen, projectId, devDelta, prodMultiplier]);

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
        background: 'var(--bg-card)', width: '100%', maxWidth: '900px', maxHeight: '92vh',
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
              width: 36, height: 36, borderRadius: 'var(--radius-sm)',
              background: 'linear-gradient(135deg, #3b82f6, #06b6d4)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
              boxShadow: '0 4px 12px rgba(59, 130, 246, 0.35)',
            }}>
              <Zap size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}>
                What-If Scenario Simulator & Sandbox
                <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '2px 8px', borderRadius: 99, background: badge.bg, color: badge.text, border: `1px solid ${badge.border}` }}>
                  {badge.label}
                </span>
              </h3>
              <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                Simulating: <strong>{projectName}</strong> · In-Memory CPM Sandbox
              </p>
            </div>
          </div>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 4 }}>
            <X size={20} />
          </button>
        </div>

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
            display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 14,
            padding: 16, background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)',
          }}>
            {/* Developer Headcount Delta */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>
                <span><Users size={13} style={{ display: 'inline', marginRight: 4 }} /> Team Headcount Delta</span>
                <span style={{ color: devDelta > 0 ? '#059669' : (devDelta < 0 ? '#dc2626' : 'var(--text-secondary)') }}>
                  {devDelta > 0 ? `+${devDelta} Developers` : (devDelta < 0 ? `${devDelta} Developers` : 'No Change (Baseline)')}
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
                <span><Sliders size={13} style={{ display: 'inline', marginRight: 4 }} /> Velocity Multiplier</span>
                <span style={{ color: '#3b82f6' }}>{prodMultiplier.toFixed(1)}x Velocity</span>
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
          </div>

          {/* ── Key Impact Comparison Cards ── */}
          {result && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12 }}>
              {/* Delivery Timeline Delta */}
              <div style={{ padding: 14, borderRadius: 'var(--radius-sm)', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 700 }}>
                  <Calendar size={12} style={{ display: 'inline', marginRight: 4 }} /> Scheduled Lead Time
                </div>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                  {result.simulatedDurationDays} Days
                </div>
                <div style={{ fontSize: '0.78rem', marginTop: 4, display: 'flex', alignItems: 'center', gap: 4, color: result.durationDeltaDays <= 0 ? '#059669' : '#dc2626', fontWeight: 700 }}>
                  {result.durationDeltaDays <= 0 ? <TrendingDown size={14} /> : <TrendingUp size={14} />}
                  {result.durationDeltaDays <= 0 ? `${Math.abs(result.durationDeltaDays)} days faster` : `+${result.durationDeltaDays} days delay`}
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
                  <DollarSign size={12} style={{ display: 'inline', marginRight: 4 }} /> Estimated Project Cost
                </div>
                <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 4 }}>
                  ₹{Number(result.simulatedEstimatedCost).toLocaleString()}
                </div>
                <div style={{ fontSize: '0.78rem', marginTop: 4, color: result.costDelta >= 0 ? '#b91c1c' : '#059669', fontWeight: 700 }}>
                  {result.costDelta >= 0 ? `+₹${Math.abs(result.costDelta).toLocaleString()}` : `-₹${Math.abs(result.costDelta).toLocaleString()}`}
                  <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}> financial variance</span>
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

          {/* ── Mini Simulated Gantt Schedule ── */}
          {result?.simulatedTasks && (
            <div>
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 8 }}>
                📊 Simulated Task Schedule & Critical Path ({result.simulatedTasks.length} Tasks)
              </div>
              <div style={{ border: '1px solid var(--border-light)', borderRadius: 6, overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
                  <thead>
                    <tr style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border-light)', color: 'var(--text-secondary)', textAlign: 'left' }}>
                      <th style={{ padding: '8px 10px' }}>Task Name</th>
                      <th style={{ padding: '8px 10px', width: 90 }}>Duration</th>
                      <th style={{ padding: '8px 10px', width: 100 }}>Start</th>
                      <th style={{ padding: '8px 10px', width: 100 }}>End</th>
                      <th style={{ padding: '8px 10px', width: 95 }}>CPM Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.simulatedTasks.map(t => (
                      <tr key={t.id} style={{ borderBottom: '1px solid var(--border-light)', background: t.isCritical ? 'rgba(239, 68, 68, 0.04)' : 'transparent' }}>
                        <td style={{ padding: '8px 10px', fontWeight: t.isCritical ? 700 : 500, color: 'var(--text-primary)' }}>
                          {t.name}
                        </td>
                        <td style={{ padding: '8px 10px', color: 'var(--text-secondary)' }}>{t.durationDays}d</td>
                        <td style={{ padding: '8px 10px', color: 'var(--text-muted)' }}>{t.scheduledStart}</td>
                        <td style={{ padding: '8px 10px', color: 'var(--text-muted)' }}>{t.scheduledEnd}</td>
                        <td style={{ padding: '8px 10px' }}>
                          {t.isCritical ? (
                            <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 7px', borderRadius: 99, background: '#fee2e2', color: '#b91c1c' }}>
                              CRITICAL
                            </span>
                          ) : (
                            <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Float Slack</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>

        {/* ── Footer ── */}
        <div style={{
          padding: '12px 1.75rem', borderTop: '1px solid var(--border-light)',
          display: 'flex', justifyContent: 'flex-end', gap: 10, background: 'var(--bg-secondary)',
        }}>
          <button
            onClick={onClose}
            style={{ padding: '8px 18px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-medium)', background: 'var(--bg-card)', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer' }}
          >
            Close Sandbox
          </button>
        </div>
      </div>
    </div>
  );
}

export { WhatIfSimulatorModalComponent as WhatIfSimulatorModal };
