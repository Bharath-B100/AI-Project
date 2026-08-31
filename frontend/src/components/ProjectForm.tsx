import { FormEvent, useState } from 'react';
import { ProjectInput, ProjectStatus } from '../services/api';

export default function ProjectForm({ onSave }: { onSave: (value: ProjectInput) => Promise<void> }) {
  const [expanded, setExpanded] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [budget, setBudget] = useState('');
  const [methodology, setMethodology] = useState('');
  const [status, setStatus] = useState<ProjectStatus>('DRAFT');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setSaving(true);
    setError('');
    try {
      if (startDate && endDate && new Date(endDate) < new Date(startDate)) {
        throw new Error('End date must be on or after start date');
      }
      await onSave({
        name,
        description: description || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        budget: budget ? Number(budget) : undefined,
        methodology: methodology || undefined,
        status,
      });
      setName('');
      setDescription('');
      setStartDate('');
      setEndDate('');
      setBudget('');
      setMethodology('');
      setStatus('DRAFT');
      setExpanded(false);
    } catch (err: any) {
      setError(err.message || 'Could not save project');
    } finally {
      setSaving(false);
    }
  };

  if (!expanded) {
    return (
      <button 
        className="btn btn-primary" 
        onClick={() => setExpanded(true)} 
        style={{ marginBottom: '1.5rem' }}
      >
        + New Project
      </button>
    );
  }

  return (
    <div className="glass-panel" style={{ padding: '1.75rem', marginBottom: '2rem', borderColor: 'var(--primary-border)' }}>
      <h3 style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '1.25rem', color: 'var(--text-primary)' }}>Create New Project</h3>
      {error && <p style={{ color: 'var(--accent-crimson)', fontSize: '0.85rem', marginBottom: '1rem', background: 'var(--accent-crimson-subtle)', padding: '8px 12px', borderRadius: '6px' }}>{error}</p>}
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Project Name *</label>
            <input
              required
              maxLength={150}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Website Launch"
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Methodology</label>
            <input
              maxLength={80}
              value={methodology}
              onChange={(e) => setMethodology(e.target.value)}
              placeholder="e.g. Agile, Waterfall"
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Status</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value as ProjectStatus)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            >
              <option value="DRAFT">DRAFT</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="ON_HOLD">ON_HOLD</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
          <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Description</label>
          <textarea
            rows={3}
            maxLength={2000}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe project goals, deliverables, and scope..."
            style={{ padding: '8px 12px', fontSize: '0.9rem', resize: 'vertical' }}
          />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '1rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Budget ($)</label>
            <input
              type="number"
              min="0"
              value={budget}
              onChange={(e) => setBudget(e.target.value)}
              placeholder="e.g. 15000"
              style={{ padding: '8px 12px', fontSize: '0.9rem' }}
            />
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
          <button type="button" className="btn btn-secondary" onClick={() => setExpanded(false)} disabled={saving}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Creating...' : 'Create Project'}
          </button>
        </div>
      </form>
    </div>
  );
}
