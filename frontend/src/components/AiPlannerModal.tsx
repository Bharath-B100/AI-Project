import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Sparkles, X, ArrowRight, ArrowLeft,
  Calendar, Users, DollarSign, Layers, AlertCircle,
  Play, RefreshCw,
} from 'lucide-react';
import { planningApi, GeneratedPlan } from '../services/api';

interface AiPlannerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (projectId: number) => void;
}

const TEMPLATE_PROMPTS = [
  {
    title: '📱 Cross-Platform Mobile App',
    prompt: 'Build a production-ready iOS and Android mobile app with user authentication, offline caching, push notifications, and App Store release.',
    months: 3,
    teamSize: 4,
    methodology: 'AGILE',
  },
  {
    title: '🛍️ E-Commerce Digital Storefront',
    prompt: 'Create a modern e-commerce platform with product catalog, persistent shopping cart, Stripe payment gateway, and merchant dashboard.',
    months: 4,
    teamSize: 5,
    methodology: 'AGILE',
  },
  {
    title: '🧠 AI / ML Analytics Engine',
    prompt: 'Develop an end-to-end Machine Learning pipeline with automated data ingestion, model training, ONNX quantization, and real-time dashboard.',
    months: 3,
    teamSize: 4,
    methodology: 'HYBRID',
  },
  {
    title: '☁️ Cloud Kubernetes Migration',
    prompt: 'Migrate legacy monolithic backend to AWS/GCP Kubernetes with Terraform IaC, zero-downtime database replication, and GitOps CI/CD.',
    months: 3,
    teamSize: 3,
    methodology: 'WATERFALL',
  },
];

export const AiPlannerModal: React.FC<AiPlannerModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const navigate = useNavigate();

  // Input state
  const [prompt, setPrompt]           = useState('');
  const [timelineMonths, setMonths]   = useState(3);
  const [teamSize, setTeamSize]       = useState(4);
  const [methodology, setMethodology] = useState('AGILE');
  const [budget, setBudget]           = useState<number | undefined>(undefined);

  // Flow & Plan state
  const [step, setStep]               = useState<'PROMPT' | 'REVIEW'>('PROMPT');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isCommitting, setIsCommitting] = useState(false);
  const [error, setError]             = useState<string | null>(null);
  const [plan, setPlan]               = useState<GeneratedPlan | null>(null);

  // Review editable fields
  const [editName, setEditName]       = useState('');
  const [editMethodology, setEditMethodology] = useState('AGILE');
  const [startDate, setStartDate]     = useState(new Date().toISOString().split('T')[0]);

  if (!isOpen) return null;

  const handleSelectTemplate = (tpl: typeof TEMPLATE_PROMPTS[0]) => {
    setPrompt(tpl.prompt);
    setMonths(tpl.months);
    setTeamSize(tpl.teamSize);
    setMethodology(tpl.methodology);
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) return;

    setIsGenerating(true);
    setError(null);

    try {
      const generated = await planningApi.generatePlan({
        prompt: prompt.trim(),
        timelineMonths,
        teamSize,
        methodology,
        budget: budget ? Number(budget) : undefined,
      });

      setPlan(generated);
      setEditName(generated.projectName);
      setEditMethodology(generated.suggestedMethodology || methodology);
      setStep('REVIEW');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate AI project plan. Please try again.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleCommit = async () => {
    if (!plan || !editName.trim()) return;

    setIsCommitting(true);
    setError(null);

    try {
      const res = await planningApi.commitPlan({
        projectName: editName.trim(),
        description: plan.description,
        methodology: editMethodology,
        budget: plan.recommendedBudget || 0,
        startDate,
        tasks: plan.tasks,
      });

      onSuccess(res.projectId);
      onClose();
      navigate(`/projects/${res.projectId}`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save and schedule project.');
    } finally {
      setIsCommitting(false);
    }
  };

  const getPriorityBadgeColor = (p: string) => {
    switch (p) {
      case 'CRITICAL': return { bg: '#fee2e2', text: '#b91c1c' };
      case 'HIGH':     return { bg: '#ffedd5', text: '#c2410c' };
      case 'MEDIUM':   return { bg: '#e0f2fe', text: '#0369a1' };
      default:         return { bg: '#f1f5f9', text: '#475569' };
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(15, 23, 42, 0.75)', backdropFilter: 'blur(8px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1.5rem',
    }}>
      <div style={{
        background: 'var(--bg-card)', width: '100%', maxWidth: '860px', maxHeight: '90vh',
        borderRadius: 'var(--radius-lg)', boxShadow: '0 25px 50px -12px rgba(0,0,0,0.35)',
        border: '1px solid var(--border-medium)', display: 'flex', flexDirection: 'column',
        overflow: 'hidden', animation: 'fadeInScale 0.25s ease',
      }}>

        {/* ── Modal Header ── */}
        <div style={{
          padding: '1.25rem 1.75rem', borderBottom: '1px solid var(--border-light)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          background: 'linear-gradient(90deg, var(--bg-card), var(--bg-secondary))',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{
              width: 36, height: 36, borderRadius: 'var(--radius-sm)',
              background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff',
              boxShadow: '0 4px 12px rgba(99, 102, 241, 0.4)',
            }}>
              <Sparkles size={20} />
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: 8 }}>
                AI Project Planner & Auto-Scheduler
                <span style={{ fontSize: '0.68rem', fontWeight: 700, padding: '2px 8px', borderRadius: 99, background: 'rgba(99, 102, 241, 0.12)', color: '#6366f1', border: '1px solid rgba(99, 102, 241, 0.3)' }}>
                  Intelligent CPM
                </span>
              </h3>
              <p style={{ margin: 0, fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                {step === 'PROMPT' ? 'Describe your project goal and constraints to generate a complete WBS and dependency schedule' : 'Review and fine-tune your AI-generated project structure before committing'}
              </p>
            </div>
          </div>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 4 }}>
            <X size={20} />
          </button>
        </div>

        {/* ── Error Banner ── */}
        {error && (
          <div style={{ padding: '10px 1.75rem', background: '#fef2f2', borderBottom: '1px solid #fca5a5', color: '#dc2626', fontSize: '0.84rem', display: 'flex', alignItems: 'center', gap: 8 }}>
            <AlertCircle size={16} /> {error}
          </div>
        )}

        {/* ── Modal Body ── */}
        <div style={{ padding: '1.75rem', overflowY: 'auto', flex: 1 }}>

          {step === 'PROMPT' ? (
            <form onSubmit={handleGenerate}>
              {/* Quick Template Prompts */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', letterSpacing: '0.5px', textTransform: 'uppercase', marginBottom: 8 }}>
                  ⚡ Quick-Start Project Templates
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 8 }}>
                  {TEMPLATE_PROMPTS.map((tpl, i) => (
                    <button
                      key={i} type="button" onClick={() => handleSelectTemplate(tpl)}
                      style={{
                        padding: '10px 12px', textAlign: 'left', background: 'var(--bg-secondary)',
                        border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)',
                        cursor: 'pointer', transition: 'all 0.15s',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.borderColor = '#6366f1'; e.currentTarget.style.background = 'rgba(99, 102, 241, 0.05)'; }}
                      onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border-light)'; e.currentTarget.style.background = 'var(--bg-secondary)'; }}
                    >
                      <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>{tpl.title}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 3 }}>{tpl.months} mos · {tpl.teamSize} devs · {tpl.methodology}</div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Natural Language Prompt */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
                  Project Description & Requirements <span style={{ color: '#dc2626' }}>*</span>
                </label>
                <textarea
                  rows={4}
                  value={prompt}
                  onChange={e => setPrompt(e.target.value)}
                  placeholder="e.g. Build an AI-assisted telemedicine consultation platform with video calling, prescription generation, patient health records, and HIPAA-compliant data encryption..."
                  style={{
                    width: '100%', padding: '12px 14px', borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)',
                    color: 'var(--text-primary)', fontSize: '0.88rem', resize: 'vertical',
                    boxSizing: 'border-box', outline: 'none', lineHeight: 1.5,
                  }}
                  required
                />
              </div>

              {/* Constraints Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14, marginBottom: '1.5rem' }}>
                {/* Timeline */}
                <div style={{ background: 'var(--bg-secondary)', padding: '12px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)' }}>
                  <label style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
                    <span><Calendar size={12} style={{ display: 'inline', marginRight: 4 }} /> Timeline</span>
                    <span style={{ color: '#6366f1' }}>{timelineMonths} Months</span>
                  </label>
                  <input
                    type="range" min={1} max={12} value={timelineMonths}
                    onChange={e => setMonths(Number(e.target.value))}
                    style={{ width: '100%', accentColor: '#6366f1' }}
                  />
                </div>

                {/* Team Size */}
                <div style={{ background: 'var(--bg-secondary)', padding: '12px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)' }}>
                  <label style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
                    <span><Users size={12} style={{ display: 'inline', marginRight: 4 }} /> Team Size</span>
                    <span style={{ color: '#6366f1' }}>{teamSize} Members</span>
                  </label>
                  <input
                    type="range" min={2} max={15} value={teamSize}
                    onChange={e => setTeamSize(Number(e.target.value))}
                    style={{ width: '100%', accentColor: '#6366f1' }}
                  />
                </div>

                {/* Methodology */}
                <div style={{ background: 'var(--bg-secondary)', padding: '12px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)' }}>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
                    <Layers size={12} style={{ display: 'inline', marginRight: 4 }} /> Methodology
                  </label>
                  <select
                    value={methodology}
                    onChange={e => setMethodology(e.target.value)}
                    style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid var(--border-medium)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.8rem' }}
                  >
                    <option value="AGILE">Agile (Scrum / Sprints)</option>
                    <option value="WATERFALL">Waterfall (Sequential CPM)</option>
                    <option value="HYBRID">Hybrid (Iterative Waterfall)</option>
                  </select>
                </div>

                {/* Optional Budget */}
                <div style={{ background: 'var(--bg-secondary)', padding: '12px 14px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-light)' }}>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 6 }}>
                    <DollarSign size={12} style={{ display: 'inline', marginRight: 4 }} /> Budget (Optional)
                  </label>
                  <input
                    type="number" placeholder="Auto-calculated if blank"
                    value={budget ?? ''}
                    onChange={e => setBudget(e.target.value ? Number(e.target.value) : undefined)}
                    style={{ width: '100%', padding: '6px 8px', borderRadius: 4, border: '1px solid var(--border-medium)', background: 'var(--bg-card)', color: 'var(--text-primary)', fontSize: '0.8rem' }}
                  />
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
                <button
                  type="button" onClick={onClose}
                  style={{ padding: '9px 18px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button
                  type="submit" disabled={isGenerating || !prompt.trim()}
                  style={{
                    padding: '9px 24px', borderRadius: 'var(--radius-xs)', border: 'none',
                    background: 'linear-gradient(135deg, #6366f1, #8b5cf6)', color: '#fff',
                    fontWeight: 700, fontSize: '0.88rem', cursor: isGenerating ? 'wait' : 'pointer',
                    display: 'flex', alignItems: 'center', gap: 8, boxShadow: '0 4px 14px rgba(99, 102, 241, 0.35)',
                    opacity: isGenerating || !prompt.trim() ? 0.6 : 1,
                  }}
                >
                  {isGenerating ? (
                    <><RefreshCw size={15} style={{ animation: 'spin 1s linear infinite' }} /> Generating Plan…</>
                  ) : (
                    <><Sparkles size={15} /> Generate AI Project Plan <ArrowRight size={15} /></>
                  )}
                </button>
              </div>
            </form>
          ) : (
            <div>
              {/* Plan Banner */}
              <div style={{
                padding: '16px', borderRadius: 'var(--radius-sm)', background: 'linear-gradient(135deg, rgba(99,102,241,0.08), rgba(139,92,246,0.05))',
                border: '1px solid rgba(99,102,241,0.25)', marginBottom: '1.25rem',
              }}>
                <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                  <div>
                    <div style={{ fontSize: '0.74rem', fontWeight: 700, color: '#6366f1', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Generated Plan Overview</div>
                    <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', marginTop: 2 }}>{plan?.projectName}</div>
                  </div>
                  <div style={{ display: 'flex', gap: 16, fontSize: '0.82rem' }}>
                    <div>
                      <span style={{ color: 'var(--text-muted)' }}>Estimated Duration: </span>
                      <strong style={{ color: 'var(--text-primary)' }}>{plan?.estimatedTotalDays} Days</strong>
                    </div>
                    <div>
                      <span style={{ color: 'var(--text-muted)' }}>Recommended Budget: </span>
                      <strong style={{ color: '#059669' }}>₹{(plan?.recommendedBudget ?? 0).toLocaleString()}</strong>
                    </div>
                  </div>
                </div>

                {/* Recommended Team Roles */}
                {plan?.recommendedRoles && plan.recommendedRoles.length > 0 && (
                  <div style={{ marginTop: 12, paddingTop: 10, borderTop: '1px solid rgba(99,102,241,0.15)', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)', fontWeight: 600 }}>Suggested Team:</span>
                    {plan.recommendedRoles.map((r, i) => (
                      <span key={i} style={{ fontSize: '0.73rem', fontWeight: 600, padding: '2px 8px', borderRadius: 99, background: 'var(--bg-card)', border: '1px solid var(--border-light)', color: 'var(--text-primary)' }}>
                        {r}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {/* Editable Fields */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12, marginBottom: '1.25rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 4 }}>Project Name</label>
                  <input
                    value={editName} onChange={e => setEditName(e.target.value)}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)', color: 'var(--text-primary)', fontSize: '0.85rem', boxSizing: 'border-box' }}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 4 }}>Methodology</label>
                  <select
                    value={editMethodology} onChange={e => setEditMethodology(e.target.value)}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)', color: 'var(--text-primary)', fontSize: '0.85rem' }}
                  >
                    <option value="AGILE">Agile (Scrum / Sprints)</option>
                    <option value="WATERFALL">Waterfall (Sequential CPM)</option>
                    <option value="HYBRID">Hybrid</option>
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 4 }}>Start Date</label>
                  <input
                    type="date" value={startDate} onChange={e => setStartDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 10px', borderRadius: 6, border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)', color: 'var(--text-primary)', fontSize: '0.85rem', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              {/* Milestones Roadmaps */}
              <div style={{ marginBottom: '1.25rem' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 8 }}>
                  🚩 Project Roadmap Milestones
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {plan?.milestones.map((m, i) => (
                    <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 6 }}>
                      <div>
                        <span style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-primary)' }}>{m.name}</span>
                        <span style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginLeft: 8 }}>{m.description}</span>
                      </div>
                      <span style={{ fontSize: '0.75rem', fontWeight: 600, color: '#6366f1', flexShrink: 0 }}>~Day +{m.targetDayOffset}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* WBS Task Breakdown */}
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 8 }}>
                  📝 Work Breakdown Structure ({plan?.tasks.length} Tasks Generated)
                </div>
                <div style={{ border: '1px solid var(--border-light)', borderRadius: 6, overflow: 'hidden' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
                    <thead>
                      <tr style={{ background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border-light)', textAlign: 'left', color: 'var(--text-secondary)' }}>
                        <th style={{ padding: '8px 10px', width: 45 }}>ID</th>
                        <th style={{ padding: '8px 10px' }}>Task Title</th>
                        <th style={{ padding: '8px 10px', width: 90 }}>Duration</th>
                        <th style={{ padding: '8px 10px', width: 85 }}>Priority</th>
                        <th style={{ padding: '8px 10px', width: 110 }}>Predecessors</th>
                      </tr>
                    </thead>
                    <tbody>
                      {plan?.tasks.map(t => {
                        const pColor = getPriorityBadgeColor(t.priority);
                        return (
                          <tr key={t.tempId} style={{ borderBottom: '1px solid var(--border-light)' }}>
                            <td style={{ padding: '8px 10px', fontWeight: 700, color: 'var(--text-muted)' }}>{t.tempId}</td>
                            <td style={{ padding: '8px 10px' }}>
                              <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{t.title}</div>
                              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 2 }}>{t.description}</div>
                            </td>
                            <td style={{ padding: '8px 10px', color: 'var(--text-secondary)', fontWeight: 600 }}>{t.durationDays}d · {t.estimatedHours}h</td>
                            <td style={{ padding: '8px 10px' }}>
                              <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 7px', borderRadius: 99, background: pColor.bg, color: pColor.text }}>
                                {t.priority}
                              </span>
                            </td>
                            <td style={{ padding: '8px 10px' }}>
                              {t.dependsOnTempIds && t.dependsOnTempIds.length > 0 ? (
                                <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                                  {t.dependsOnTempIds.map(dep => (
                                    <span key={dep} style={{ fontSize: '0.7rem', fontWeight: 700, padding: '1px 6px', borderRadius: 4, background: 'rgba(99, 102, 241, 0.12)', color: '#6366f1' }}>
                                      {dep}
                                    </span>
                                  ))}
                                </div>
                              ) : (
                                <span style={{ color: 'var(--text-muted)', fontSize: '0.72rem' }}>None (Start)</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Review Actions */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <button
                  type="button" onClick={() => setStep('PROMPT')}
                  style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '9px 16px', borderRadius: 'var(--radius-xs)', border: '1px solid var(--border-medium)', background: 'var(--bg-secondary)', color: 'var(--text-secondary)', fontWeight: 600, fontSize: '0.85rem', cursor: 'pointer' }}
                >
                  <ArrowLeft size={14} /> Back to Prompt
                </button>
                <button
                  type="button" onClick={handleCommit} disabled={isCommitting || !editName.trim()}
                  style={{
                    padding: '9px 24px', borderRadius: 'var(--radius-xs)', border: 'none',
                    background: 'linear-gradient(135deg, #059669, #10b981)', color: '#fff',
                    fontWeight: 700, fontSize: '0.88rem', cursor: isCommitting ? 'wait' : 'pointer',
                    display: 'flex', alignItems: 'center', gap: 8, boxShadow: '0 4px 14px rgba(16, 185, 129, 0.35)',
                    opacity: isCommitting || !editName.trim() ? 0.6 : 1,
                  }}
                >
                  {isCommitting ? (
                    <><RefreshCw size={15} style={{ animation: 'spin 1s linear infinite' }} /> Creating & Scheduling…</>
                  ) : (
                    <><Play size={15} /> Confirm & Build Project <ArrowRight size={15} /></>
                  )}
                </button>
              </div>
            </div>
          )}

        </div>
      </div>
    </div>
  );
};
