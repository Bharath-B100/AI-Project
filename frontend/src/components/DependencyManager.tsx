import { useState } from 'react';
import { Dependency, DependencyType, Task, dependencyApi } from '../services/api';

interface DependencyManagerProps {
  projectId: number;
  tasks: Task[];
  dependencies: Dependency[];
  onDependenciesChange: () => void;
}

const DEP_TYPE_LABELS: Record<DependencyType, string> = {
  FINISH_TO_START: 'Finish → Start (FS)',
  START_TO_START: 'Start → Start (SS)',
  FINISH_TO_FINISH: 'Finish → Finish (FF)',
  START_TO_FINISH: 'Start → Finish (SF)',
};

export default function DependencyManager({
  projectId,
  tasks,
  dependencies,
  onDependenciesChange,
}: DependencyManagerProps) {
  const [predecessorId, setPredecessorId] = useState('');
  const [successorId, setSuccessorId] = useState('');
  const [depType, setDepType] = useState<DependencyType>('FINISH_TO_START');
  const [lagDays, setLagDays] = useState(0);
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState('');

  const taskMap = Object.fromEntries(tasks.map((t) => [t.id, t.title]));

  const handleAdd = async () => {
    setError('');
    if (!predecessorId || !successorId) {
      setError('Please select both a predecessor and a successor task.');
      return;
    }
    if (predecessorId === successorId) {
      setError('A task cannot depend on itself.');
      return;
    }
    setAdding(true);
    try {
      await dependencyApi.create(projectId, Number(successorId), {
        predecessorTaskId: Number(predecessorId),
        dependencyType: depType,
        lagDays,
      });
      setPredecessorId('');
      setSuccessorId('');
      setDepType('FINISH_TO_START');
      setLagDays(0);
      onDependenciesChange();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Could not create dependency.');
    } finally {
      setAdding(false);
    }
  };

  const handleRemove = async (depId: number) => {
    try {
      await dependencyApi.remove(projectId, depId);
      onDependenciesChange();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Could not remove dependency.');
    }
  };

  const selectStyle: React.CSSProperties = {
    background: 'rgba(255,255,255,0.05)',
    border: '1px solid var(--border-light)',
    borderRadius: 8,
    color: 'var(--text-primary)',
    padding: '8px 12px',
    fontSize: '0.9rem',
    width: '100%',
    cursor: 'pointer',
  };

  const inputStyle: React.CSSProperties = {
    ...selectStyle,
    width: 80,
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Add dependency form */}
      <div className="glass-panel" style={{ padding: 20 }}>
        <h4 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 16 }}>Add Dependency</h4>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr auto 1fr', gap: 12, alignItems: 'center', marginBottom: 12 }}>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
              Predecessor (runs first)
            </label>
            <select
              id="dep-predecessor"
              value={predecessorId}
              onChange={(e) => setPredecessorId(e.target.value)}
              style={selectStyle}
            >
              <option value="">Select task…</option>
              {tasks.map((t) => (
                <option key={t.id} value={t.id}>{t.title}</option>
              ))}
            </select>
          </div>
          <div style={{ textAlign: 'center', color: 'var(--primary)', fontSize: '1.4rem', marginTop: 20 }}>→</div>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
              Successor (depends on predecessor)
            </label>
            <select
              id="dep-successor"
              value={successorId}
              onChange={(e) => setSuccessorId(e.target.value)}
              style={selectStyle}
            >
              <option value="">Select task…</option>
              {tasks.map((t) => (
                <option key={t.id} value={t.id}>{t.title}</option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 120px', gap: 12, marginBottom: 16 }}>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
              Dependency Type
            </label>
            <select
              id="dep-type"
              value={depType}
              onChange={(e) => setDepType(e.target.value as DependencyType)}
              style={selectStyle}
            >
              {Object.entries(DEP_TYPE_LABELS).map(([k, v]) => (
                <option key={k} value={k}>{v}</option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>
              Lag (days)
            </label>
            <input
              id="dep-lag"
              type="number"
              min={0}
              value={lagDays}
              onChange={(e) => setLagDays(Math.max(0, Number(e.target.value)))}
              style={inputStyle}
            />
          </div>
        </div>

        {error && (
          <div style={{
            padding: '8px 12px', marginBottom: 12, borderRadius: 8,
            background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.3)',
            color: '#f87171', fontSize: '0.85rem',
          }}>
            {error}
          </div>
        )}

        <button
          id="dep-add-btn"
          className="btn btn-primary"
          onClick={handleAdd}
          disabled={adding}
          style={{ fontSize: '0.9rem', padding: '8px 18px' }}
        >
          {adding ? 'Adding…' : '+ Add Dependency'}
        </button>
      </div>

      {/* Existing dependencies table */}
      <div>
        <h4 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: 12 }}>
          Existing Dependencies ({dependencies.length})
        </h4>
        {dependencies.length === 0 ? (
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
            No dependencies defined yet. Add one above to define task order.
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {dependencies.map((dep) => (
              <div
                key={dep.id}
                className="glass-panel"
                style={{
                  padding: '12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 12,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, flex: 1, flexWrap: 'wrap' }}>
                  <span style={{
                    background: 'var(--primary-subtle)',
                    border: '1px solid var(--primary-border)',
                    color: 'var(--primary-light)',
                    borderRadius: 'var(--radius-xs)', padding: '4px 12px',
                    fontSize: '0.85rem', fontWeight: 600,
                  }}>
                    {taskMap[dep.predecessorTaskId] || `Task #${dep.predecessorTaskId}`}
                  </span>
                  <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                    {DEP_TYPE_LABELS[dep.dependencyType]}
                    {dep.lagDays > 0 && ` +${dep.lagDays}d lag`}
                  </span>
                  <span style={{ color: 'var(--secondary)', fontSize: '1rem', fontWeight: 700 }}>→</span>
                  <span style={{
                    background: 'var(--secondary-subtle)',
                    border: '1px solid var(--secondary-border)',
                    color: 'var(--secondary-light)',
                    borderRadius: 'var(--radius-xs)', padding: '4px 12px',
                    fontSize: '0.85rem', fontWeight: 600,
                  }}>
                    {taskMap[dep.successorTaskId] || `Task #${dep.successorTaskId}`}
                  </span>
                </div>
                <button
                  className="btn btn-danger"
                  onClick={() => handleRemove(dep.id)}
                  style={{
                    padding: '4px 12px',
                    fontSize: '0.8rem',
                    whiteSpace: 'nowrap',
                    flexShrink: 0,
                  }}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
