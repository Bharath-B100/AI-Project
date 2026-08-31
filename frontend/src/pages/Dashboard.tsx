import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, DashboardOverview } from '../services/api';
import { FolderKanban, Target, Clock, Zap, ArrowUpRight } from 'lucide-react';
import LogoLoader from '../components/LogoLoader';

export default function Dashboard() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    dashboardApi.getOverview()
      .then(setOverview)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-full)', color: 'var(--primary-light)', fontSize: '0.78rem', fontWeight: 600, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
          PROJECT INTELLIGENCE
        </div>
        <h2 style={{ fontSize: '2rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)' }}>
          Executive Overview
        </h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', marginTop: '0.25rem' }}>
          Real-time CPM calculations, milestone schedules, and portfolio tracking
        </p>
      </div>

      {/* KPI Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.25rem', borderColor: 'var(--primary-border)' }}>
          <div style={{ background: 'var(--primary-subtle)', padding: '14px', borderRadius: '12px', color: 'var(--primary-light)', display: 'flex', boxShadow: '0 4px 15px var(--primary-glow)' }}>
            <FolderKanban size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>ACTIVE PROJECTS</div>
            <div style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--text-primary)' }}>{overview?.activeProjects || 0}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>out of {overview?.totalProjects || 0} total</div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.25rem', borderColor: 'var(--secondary-border)' }}>
          <div style={{ background: 'var(--secondary-subtle)', padding: '14px', borderRadius: '12px', color: 'var(--secondary-light)', display: 'flex', boxShadow: '0 4px 15px var(--secondary-glow)' }}>
            <Target size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>PROJECT HEALTH</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: overview?.globalHealthStatus === 'OFF_TRACK' ? 'var(--accent-rose)' : overview?.globalHealthStatus === 'AT_RISK' ? '#f39c12' : 'var(--secondary-light)' }}>
              {overview?.globalHealthStatus || 'IDLE'}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Aggregated status</div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{ background: 'var(--accent-emerald-subtle)', padding: '14px', borderRadius: '12px', color: 'var(--accent-emerald)', display: 'flex' }}>
            <Zap size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>TEAM WORKLOAD</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: overview?.globalWorkloadStatus === 'OVERLOADED' ? 'var(--accent-rose)' : overview?.globalWorkloadStatus === 'NEAR_CAPACITY' ? '#f39c12' : 'var(--accent-emerald)' }}>
              {overview?.globalWorkloadStatus || 'AVAILABLE'}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Resource allocation status</div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{ background: 'rgba(250, 177, 160, 0.1)', padding: '14px', borderRadius: '12px', color: '#fab1a0', display: 'flex' }}>
            <Clock size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontWeight: 600 }}>BUDGET BURN</div>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#fab1a0' }}>${overview?.totalSpent || 0} / ${overview?.totalBudget || 0}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Cost tracking enabled</div>
          </div>
        </div>
      </div>

      {/* Recent Projects Section */}
      <div className="glass-panel" style={{ padding: '1.75rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>Your Active Workspaces</h3>
          <Link to="/projects" className="btn btn-secondary" style={{ fontSize: '0.85rem', padding: '6px 14px' }}>
            View All Projects →
          </Link>
        </div>

        {loading ? (
          <LogoLoader message="Loading overview..." />
        ) : !overview || overview.recentProjects.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>No projects created yet.</p>
            <Link to="/projects" className="btn btn-primary">
              + Create First Project
            </Link>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {overview.recentProjects.map((p) => (
              <Link
                key={p.id}
                to={`/projects/${p.id}`}
                className="glass-panel"
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '14px 18px',
                  textDecoration: 'none',
                  color: 'inherit',
                  transition: 'var(--transition-smooth)'
                }}
              >
                <div>
                  <strong style={{ fontSize: '1rem', color: 'var(--text-primary)' }}>{p.name}</strong>
                  <div style={{ color: 'var(--text-muted)', fontSize: '0.82rem', marginTop: '2px' }}>
                    {p.description || 'No description provided.'}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span className={`badge ${p.status === 'ACTIVE' ? 'badge-success' : 'badge-primary'}`}>{p.status}</span>
                  <ArrowUpRight size={18} color="var(--primary-light)" />
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
