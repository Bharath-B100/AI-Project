import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ProjectForm from '../components/ProjectForm';
import { AiPlannerModal } from '../components/AiPlannerModal';
import { projectApi, Project, ProjectInput } from '../services/api';
import { FolderKanban, Calendar, DollarSign, Layers, Sparkles } from 'lucide-react';
import LogoLoader from '../components/LogoLoader';

export default function Projects() {
  const [items, setItems] = useState<Project[]>([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);

  const load = () =>
    projectApi
      .list()
      .then(setItems)
      .catch(() => setError('Unable to load projects. Check that the backend is running.'))
      .finally(() => setLoading(false));

  useEffect(() => {
    void load();
  }, []);

  const create = async (v: ProjectInput) => {
    try {
      await projectApi.create(v);
      load();
    } catch {
      setError('Could not create project');
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="badge badge-success">Active</span>;
      case 'COMPLETED':
        return <span className="badge badge-primary">Completed</span>;
      case 'ON_HOLD':
        return <span className="badge badge-warning">On Hold</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.75rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px' }}>Projects</h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', marginTop: '0.2rem' }}>
            Manage and schedule your team's initiatives with CPM analytics
          </p>
        </div>

        {/* AI Planner Action Button */}
        <button
          onClick={() => setIsAiModalOpen(true)}
          style={{
            padding: '10px 20px', borderRadius: 'var(--radius-sm)', border: 'none',
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)', color: '#fff',
            fontWeight: 700, fontSize: '0.88rem', cursor: 'pointer',
            display: 'flex', alignItems: 'center', gap: 8,
            boxShadow: '0 4px 14px rgba(99, 102, 241, 0.35)',
            transition: 'transform 0.15s ease, box-shadow 0.15s ease',
          }}
          onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 6px 18px rgba(99, 102, 241, 0.45)'; }}
          onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(99, 102, 241, 0.35)'; }}
        >
          <Sparkles size={16} /> ✨ Create with AI Planner
        </button>
      </div>

      <AiPlannerModal
        isOpen={isAiModalOpen}
        onClose={() => setIsAiModalOpen(false)}
        onSuccess={() => load()}
      />

      <ProjectForm onSave={create} />

      {error && (
        <div className="glass-panel" style={{ padding: '1rem', borderColor: 'var(--accent-crimson)', color: '#ff7675', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {loading ? (
        <LogoLoader message="Loading projects..." />
      ) : items.length === 0 ? (
        <div className="glass-panel" style={{ padding: '3.5rem 2rem', textAlign: 'center' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📂</div>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>No projects created yet</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', maxWidth: '400px', margin: '0 auto' }}>
            Create your first project above to begin planning milestones, task dependencies, and critical path schedules.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))', gap: '1.25rem' }}>
          {items.map((p) => (
            <Link
              key={p.id}
              to={`/projects/${p.id}`}
              className="glass-card-interactive"
              style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.75rem', marginBottom: '0.75rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div style={{ background: 'var(--primary-subtle)', color: 'var(--primary-light)', padding: '8px', borderRadius: '8px', display: 'flex' }}>
                      <FolderKanban size={18} />
                    </div>
                    <strong style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)' }}>{p.name}</strong>
                  </div>
                  {getStatusBadge(p.status)}
                </div>

                <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, marginBottom: '1.25rem', minHeight: '2.6rem' }}>
                  {p.description || 'No description provided.'}
                </p>
              </div>

              <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: '0.85rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                  <Layers size={14} color="var(--primary-light)" />
                  <span>{p.methodology || 'CPM / Agile'}</span>
                </div>
                {p.budget !== undefined && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: 'var(--text-secondary)' }}>
                    <DollarSign size={14} color="var(--accent-emerald)" />
                    <span>${p.budget.toLocaleString()}</span>
                  </div>
                )}
                {(p.startDate || p.endDate) && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Calendar size={14} color="var(--secondary)" />
                    <span>{p.startDate || '?'}</span>
                  </div>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
