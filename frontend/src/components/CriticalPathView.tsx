import { CriticalPath } from '../services/api';
import LogoLoader from './LogoLoader';
interface CriticalPathViewProps {
  criticalPath: CriticalPath | null;
  loading: boolean;
}

export default function CriticalPathView({ criticalPath, loading }: CriticalPathViewProps) {
  if (loading) {
    return <LogoLoader message="Calculating critical path..." />;
  }

  if (!criticalPath || criticalPath.tasks.length === 0) {
    return (
      <div style={{
        textAlign: 'center',
        padding: '3rem',
        color: 'var(--text-secondary)',
      }}>
        <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>🎯</div>
        <p>No critical path found.</p>
        <p style={{ marginTop: 6, fontSize: '0.85rem' }}>
          Add task dependencies and click <strong>Calculate Schedule</strong> to identify the critical path.
        </p>
      </div>
    );
  }

  const { tasks, totalDurationDays } = criticalPath;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Summary card */}
      <div
        className="glass-panel"
        style={{
          padding: '24px 28px',
          borderColor: 'var(--secondary-border)',
          background: 'var(--secondary-subtle)',
          display: 'flex',
          alignItems: 'center',
          gap: 24,
          flexWrap: 'wrap',
          boxShadow: '0 8px 30px rgba(0, 0, 0, 0.3), 0 0 20px rgba(225, 112, 85, 0.15)'
        }}
      >
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--secondary-light)', fontWeight: 600, letterSpacing: '0.5px', marginBottom: 4 }}>
            CRITICAL PATH LENGTH
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--secondary-light)', letterSpacing: '-0.5px' }}>
            {totalDurationDays} <span style={{ fontSize: '1.1rem', fontWeight: 500, color: 'var(--text-secondary)' }}>days</span>
          </div>
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--secondary-light)', fontWeight: 600, letterSpacing: '0.5px', marginBottom: 4 }}>
            TASKS ON CRITICAL CHAIN
          </div>
          <div style={{ fontSize: '2.2rem', fontWeight: 800, color: 'var(--secondary-light)', letterSpacing: '-0.5px' }}>
            {tasks.length}
          </div>
        </div>
        <div style={{
          padding: '10px 18px',
          borderRadius: 'var(--radius-sm)',
          background: 'rgba(225, 112, 85, 0.15)',
          border: '1px solid var(--secondary-border)',
          fontSize: '0.88rem',
          color: 'var(--secondary-light)',
          maxWidth: 300,
          fontWeight: 500
        }}>
          ⚡ Zero float: Any delay in these tasks pushes back the target completion date.
        </div>
      </div>

      {/* Task chain */}
      <div>
        <h4 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: 16, color: 'var(--text-primary)' }}>
          Critical Path Sequence (in execution order)
        </h4>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {tasks.map((task, idx) => {
            const pct = Math.round((task.durationDays / totalDurationDays) * 100);
            return (
              <div key={task.id}>
                <div
                  className="glass-panel"
                  style={{
                    padding: '16px 20px',
                    borderColor: 'var(--secondary-border)',
                    background: 'var(--bg-card)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 10 }}>
                    {/* Step number */}
                    <div style={{
                      width: 32, height: 32, borderRadius: '50%',
                      background: 'var(--secondary-subtle)',
                      border: '2px solid var(--secondary)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: '0.85rem', fontWeight: 800, color: 'var(--secondary-light)',
                      flexShrink: 0,
                      boxShadow: '0 0 10px var(--secondary-glow)'
                    }}>
                      {idx + 1}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>{task.name}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 2 }}>
                        {task.scheduledStart} → {task.scheduledEnd} ({task.durationDays}d)
                      </div>
                    </div>
                    <span className="badge badge-secondary">
                      {task.durationDays} day(s) · {pct}%
                    </span>
                  </div>

                  {/* Progress bar representing proportion of project */}
                  <div style={{
                    height: 5, borderRadius: 3,
                    background: 'rgba(255,255,255,0.06)',
                    overflow: 'hidden',
                  }}>
                    <div style={{
                      width: `${pct}%`,
                      height: '100%',
                      background: 'linear-gradient(90deg, var(--secondary) 0%, var(--secondary-light) 100%)',
                      borderRadius: 3,
                      boxShadow: '0 0 8px var(--secondary-glow)'
                    }} />
                  </div>
                </div>

                {/* Connector arrow between tasks */}
                {idx < tasks.length - 1 && (
                  <div style={{
                    textAlign: 'center',
                    color: 'var(--secondary)',
                    fontSize: '1.2rem',
                    lineHeight: '22px',
                  }}>
                    ↓
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
