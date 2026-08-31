import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Plus, Trash2, Sparkles, CheckCircle2, ArrowRight } from 'lucide-react';
import { teamApi, TeamMember, trackingApi, TeamWorkload, levelingApi, LevelingReport } from '../services/api';
import LogoLoader from '../components/LogoLoader';

export default function Team() {
  const { id } = useParams();
  const projectId = Number(id);

  const [members, setMembers] = useState<TeamMember[]>([]);
  const [workloads, setWorkloads] = useState<TeamWorkload[]>([]);
  const [levelingReport, setLevelingReport] = useState<LevelingReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [applyingLeveling, setApplyingLeveling] = useState(false);
  const [levelingSuccessMsg, setLevelingSuccessMsg] = useState<string | null>(null);
  const [error, setError] = useState('');

  // Form state
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('');
  const [hourlyRate, setHourlyRate] = useState<number>(50);
  const [availabilityHours, setAvailabilityHours] = useState<number>(40);

  const loadData = async () => {
    setLoading(true);
    try {
      const [teamData, workloadData, levelingData] = await Promise.all([
        teamApi.list(projectId),
        trackingApi.getWorkload(projectId).catch(() => ({ teamWorkloads: [] })),
        levelingApi.getRecommendations(projectId).catch(() => null),
      ]);
      setMembers(teamData);
      setWorkloads((workloadData as any).teamWorkloads || []);
      setLevelingReport(levelingData);
    } catch (err) {
      setError('Failed to load team data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [projectId]);

  const handleApplyLeveling = async () => {
    setApplyingLeveling(true);
    try {
      const res = await levelingApi.apply(projectId);
      setLevelingSuccessMsg(res.message);
      await loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to apply leveling.');
    } finally {
      setApplyingLeveling(false);
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim()) return;
    try {
      await teamApi.add(projectId, {
        name,
        email,
        role,
        hourlyRate,
        availabilityHoursPerWeek: availabilityHours,
        active: true
      });
      setName('');
      setEmail('');
      setRole('');
      setHourlyRate(50);
      setAvailabilityHours(40);
      loadData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to add team member');
    }
  };

  const handleRemove = async (memberId: number) => {
    if (confirm('Are you sure you want to remove this team member?')) {
      try {
        await teamApi.remove(projectId, memberId);
        loadData();
      } catch (err: any) {
        alert(err.response?.data?.message || 'Failed to remove team member');
      }
    }
  };

  const getWorkload = (memberId: number) => {
    return workloads.find(w => w.teamMemberId === memberId);
  };

  if (loading) {
    return <LogoLoader message="Loading team & workload analyzer..." />;
  }

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <Link to={`/projects/${projectId}`} style={{ color: 'var(--primary-light)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.9rem', fontWeight: 500 }}>
        ← Back to Project
      </Link>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 14 }}>
        <div>
          <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--secondary-subtle)', border: '1px solid var(--secondary-border)', borderRadius: 'var(--radius-full)', color: 'var(--secondary-light)', fontSize: '0.78rem', fontWeight: 600, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
            RESOURCE MANAGEMENT & LEVELING
          </div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', margin: 0 }}>
            Team & Workload
          </h2>
        </div>
      </div>

      {/* ── Resource Leveling AI Recommendation Banner ── */}
      {levelingReport && levelingReport.recommendations.length > 0 && (
        <div style={{
          padding: '18px 20px', borderRadius: 'var(--radius-md)',
          background: 'linear-gradient(135deg, rgba(238, 90, 36, 0.08), rgba(253, 203, 110, 0.05))',
          border: '1px solid rgba(238, 90, 36, 0.35)', boxShadow: '0 4px 20px rgba(0,0,0,0.06)',
          display: 'flex', flexDirection: 'column', gap: 12,
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{
                width: 34, height: 34, borderRadius: 8, background: '#ee5a24',
                display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
              }}>
                <Sparkles size={18} />
              </div>
              <div>
                <h4 style={{ margin: 0, fontSize: '1rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  AI Resource Leveling & Bottleneck Optimization
                </h4>
                <p style={{ margin: 0, fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                  Detected {levelingReport.overloadedCount} overloaded member(s). Redistributing tasks with slack prevents milestone delays.
                </p>
              </div>
            </div>
            <button
              onClick={handleApplyLeveling}
              disabled={applyingLeveling}
              style={{
                padding: '9px 18px', borderRadius: 'var(--radius-xs)',
                background: 'linear-gradient(135deg, #ee5a24, #f39c12)', color: '#fff',
                border: 'none', fontWeight: 700, fontSize: '0.86rem', cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 6, boxShadow: '0 4px 12px rgba(238, 90, 36, 0.35)',
              }}
            >
              {applyingLeveling ? 'Rebalancing…' : '⚡ Auto-Level Workload'}
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 4 }}>
            {levelingReport.recommendations.map((rec, i) => (
              <div key={i} style={{
                padding: '10px 14px', borderRadius: 6, background: 'var(--bg-card)',
                border: '1px solid var(--border-light)', fontSize: '0.84rem', display: 'flex',
                alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{rec.taskTitle}</span>
                  <span style={{ color: 'var(--text-muted)' }}>({rec.plannedHours}h)</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.8rem' }}>
                  <span style={{ color: '#ff7675', fontWeight: 600 }}>{rec.sourceMemberName} ({rec.sourceCurrentWorkloadPct}%)</span>
                  <ArrowRight size={14} color="var(--text-muted)" />
                  <span style={{ color: '#00b894', fontWeight: 600 }}>{rec.targetMemberName} ({rec.targetCurrentWorkloadPct}%)</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {levelingSuccessMsg && (
        <div style={{
          padding: '12px 16px', borderRadius: 'var(--radius-sm)',
          background: 'var(--accent-emerald-subtle)', border: '1px solid rgba(0, 184, 148, 0.3)',
          color: 'var(--accent-emerald)', fontSize: '0.88rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8,
        }}>
          <CheckCircle2 size={16} /> {levelingSuccessMsg}
        </div>
      )}

      {error && <div className="glass-panel" style={{ padding: 16, color: '#ff7675', borderColor: '#ff7675' }}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '24px', alignItems: 'start' }}>
        {/* Team List */}
        <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.92rem' }}>
            <thead>
              <tr style={{ background: 'var(--bg-card-hover)', borderBottom: '1px solid var(--border-light)', textAlign: 'left' }}>
                <th style={{ padding: '16px 20px', fontWeight: 600, color: 'var(--text-secondary)' }}>Name</th>
                <th style={{ padding: '16px 20px', fontWeight: 600, color: 'var(--text-secondary)' }}>Role</th>
                <th style={{ padding: '16px 20px', fontWeight: 600, color: 'var(--text-secondary)' }}>Rate/Hr</th>
                <th style={{ padding: '16px 20px', fontWeight: 600, color: 'var(--text-secondary)' }}>Utilization</th>
                <th style={{ padding: '16px 20px', fontWeight: 600, color: 'var(--text-secondary)', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {members.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No team members added yet. Add one from the panel.
                  </td>
                </tr>
              ) : (
                members.map(member => {
                  const wl = getWorkload(member.id);
                  let utilColor = 'var(--text-secondary)';
                  if (wl) {
                    if (wl.workloadStatus === 'OVERLOADED') utilColor = '#ff7675';
                    else if (wl.workloadStatus === 'NEAR_CAPACITY') utilColor = '#fdcb6e';
                    else utilColor = '#55efc4';
                  }

                  return (
                    <tr key={member.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                      <td style={{ padding: '16px 20px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{member.name}</div>
                        <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>{member.email}</div>
                      </td>
                      <td style={{ padding: '16px 20px', color: 'var(--text-secondary)' }}>{member.role || '-'}</td>
                      <td style={{ padding: '16px 20px', color: 'var(--text-secondary)' }}>${member.hourlyRate}</td>
                      <td style={{ padding: '16px 20px' }}>
                        {wl ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{ width: 60, height: 6, background: 'rgba(255,255,255,0.1)', borderRadius: 3, overflow: 'hidden' }}>
                              <div style={{ height: '100%', width: `${Math.min(wl.utilizationPercentage, 100)}%`, background: utilColor }} />
                            </div>
                            <span style={{ fontSize: '0.85rem', color: utilColor, fontWeight: 600 }}>{wl.utilizationPercentage}%</span>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No tasks</span>
                        )}
                      </td>
                      <td style={{ padding: '16px 20px', textAlign: 'right' }}>
                        <button onClick={() => handleRemove(member.id)} style={{ background: 'transparent', border: 'none', color: '#ff7675', cursor: 'pointer', padding: 4 }}>
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Add Member Form */}
        <div className="glass-panel" style={{ padding: 24 }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: 20, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Plus size={18} color="var(--primary-light)" /> Add Member
          </h3>
          <form onSubmit={handleAddMember} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 6 }}>Name</label>
              <input type="text" className="input" value={name} onChange={e => setName(e.target.value)} required placeholder="Jane Doe" />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 6 }}>Email</label>
              <input type="email" className="input" value={email} onChange={e => setEmail(e.target.value)} required placeholder="jane@example.com" />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 6 }}>Role</label>
              <input type="text" className="input" value={role} onChange={e => setRole(e.target.value)} placeholder="e.g. Frontend Developer" />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 6 }}>Rate/Hr ($)</label>
                <input type="number" className="input" value={hourlyRate} onChange={e => setHourlyRate(Number(e.target.value))} min={0} />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: 6 }}>Hours/Week</label>
                <input type="number" className="input" value={availabilityHours} onChange={e => setAvailabilityHours(Number(e.target.value))} min={1} max={168} />
              </div>
            </div>
            <button type="submit" className="btn btn-primary" style={{ marginTop: 8 }}>Add to Project</button>
          </form>
        </div>
      </div>
    </div>
  );
}
