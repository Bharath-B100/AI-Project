import { FormEvent, useState } from 'react';
import { TaskInput, TaskPriority, TaskStatus } from '../services/api';

export default function TaskForm({ onSave }: { onSave: (value: TaskInput) => Promise<void> }) {
  const [expanded, setExpanded] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [startDate, setStartDate] = useState('');
  const [estimatedHours, setEstimatedHours] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    setSaving(true);
    setError('');
    try {
      if (startDate && dueDate && new Date(dueDate) < new Date(startDate)) {
        throw new Error('Due date must be on or after start date');
      }
      await onSave({
        title,
        description: description || undefined,
        status,
        priority,
        dueDate: dueDate || undefined,
        startDate: startDate || undefined,
        estimatedHours: estimatedHours ? Number(estimatedHours) : undefined,
        progressPercentage: status === 'DONE' ? 100 : 0,
      });
      setTitle('');
      setDescription('');
      setStatus('TODO');
      setPriority('MEDIUM');
      setDueDate('');
      setStartDate('');
      setEstimatedHours('');
      setExpanded(false);
    } catch (err: any) {
      setError(err.message || 'Could not save task');
    } finally {
      setSaving(false);
    }
  };

  if (!expanded) {
    return (
      <button 
        className="btn btn-secondary" 
        onClick={() => setExpanded(true)} 
        style={{ marginBottom: '1.5rem' }}
      >
        + Add Task
      </button>
    );
  }

  return (
    <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '1.5rem', borderColor: 'var(--primary-border)' }}>
      <h4 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-primary)' }}>Add New Task</h4>
      {error && <p style={{ color: 'var(--accent-crimson)', fontSize: '0.85rem', marginBottom: '0.75rem', background: 'var(--accent-crimson-subtle)', padding: '8px 12px', borderRadius: '6px' }}>{error}</p>}
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Task Title *</label>
            <input
              required
              maxLength={180}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Design REST endpoints"
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Priority</label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value as TaskPriority)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Status</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as TaskStatus)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            >
              <option value="TODO">TODO</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="BLOCKED">BLOCKED</option>
              <option value="DONE">DONE</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
          <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Description</label>
          <textarea
            rows={2}
            maxLength={2000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Details of the work required..."
            style={{ padding: '8px 12px', fontSize: '0.9rem', resize: 'vertical' }}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.75rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Due Date</label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Est. Hours</label>
            <input
              type="number"
              min="0"
              value={estimatedHours}
              onChange={(e) => setEstimatedHours(e.target.value)}
              placeholder="e.g. 8"
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
          <button type="button" className="btn btn-secondary" style={{ padding: '8px 16px', fontSize: '0.88rem' }} onClick={() => setExpanded(false)} disabled={saving}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" style={{ padding: '8px 18px', fontSize: '0.88rem' }} disabled={saving}>
            {saving ? 'Adding...' : 'Add Task'}
          </button>
        </div>
      </form>
    </div>
  );
}
