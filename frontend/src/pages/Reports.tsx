import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  projectApi, taskApi, trackingApi, riskApi, costApi, dashboardApi, reportApi,
  Project, Task, ProjectHealth, BudgetHealth, ProjectWorkload, ProjectRisk,
  DashboardOverview, CostEntry, CreateCostEntryRequest, CostCategory, WeeklyReport,
} from '../services/api';
import {
  FileBarChart2, TrendingUp, TrendingDown, Activity,
  CheckCircle2, AlertTriangle, XCircle, Clock,
  Users, BarChart3, Shield,
  Download, RefreshCw, ChevronDown, ChevronUp,
  ExternalLink, Plus, X, Zap,
} from 'lucide-react';
import LogoLoader from '../components/LogoLoader';

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────
interface ProjectReport {
  project: Project;
  tasks: Task[];
  health: ProjectHealth | null;
  budget: BudgetHealth | null;
  workload: ProjectWorkload | null;
  risks: ProjectRisk[] | null;
  costs: CostEntry[] | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Pure helpers — no hooks, no JSX at top level
// ─────────────────────────────────────────────────────────────────────────────
function fmtMoney(n: number | null | undefined): string {
  if (n == null) return '—';
  return n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function fmtPct(n: number | null | undefined): string {
  return n != null ? `${Number(n).toFixed(1)}%` : '—';
}
function toNum(v: unknown): number {
  return typeof v === 'number' ? v : Number(v ?? 0);
}

const HEALTH_COLOR  = (h?: string) => h === 'ON_TRACK' ? '#059669' : h === 'AT_RISK' ? '#d97706' : h === 'OFF_TRACK' ? '#dc2626' : '#94a3b8';
const HEALTH_BG     = (h?: string) => h === 'ON_TRACK' ? '#ecfdf5' : h === 'AT_RISK' ? '#fffbeb' : h === 'OFF_TRACK' ? '#fef2f2' : '#f8fafc';
const HEALTH_LABEL  = (h?: string) => h === 'ON_TRACK' ? 'On Track' : h === 'AT_RISK' ? 'At Risk' : h === 'OFF_TRACK' ? 'Off Track' : 'No Data';
const BUDGET_COLOR  = (b?: string) => b === 'LOW' ? '#059669' : b === 'MEDIUM' ? '#d97706' : b === 'HIGH' ? '#ea580c' : b === 'CRITICAL' ? '#dc2626' : '#94a3b8';
const WORK_COLOR    = (w?: string) => w === 'AVAILABLE' ? '#059669' : w === 'NEAR_CAPACITY' ? '#d97706' : '#dc2626';
const SEV_COLOR     = (s?: string) => s === 'CRITICAL' ? '#dc2626' : s === 'HIGH' ? '#ea580c' : s === 'MEDIUM' ? '#d97706' : '#059669';
const RISK_BG       = (s?: string) => s === 'OPEN' ? 'rgba(220,38,38,0.08)' : s === 'MITIGATED' ? 'rgba(5,150,105,0.08)' : s === 'ACCEPTED' ? 'rgba(217,119,6,0.08)' : 'rgba(148,163,184,0.08)';

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────
function HealthIcon({ h, size = 14 }: { h?: string; size?: number }) {
  if (h === 'ON_TRACK')  return <CheckCircle2 size={size} />;
  if (h === 'AT_RISK')   return <AlertTriangle size={size} />;
  if (h === 'OFF_TRACK') return <XCircle size={size} />;
  return <Clock size={size} />;
}

function Bar({ value, max = 100, color }: { value: number; max?: number; color: string }) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return (
    <div style={{ height: 7, background: 'var(--border-light)', borderRadius: 99, overflow: 'hidden' }}>
      <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 99, transition: 'width 0.4s ease' }} />
    </div>
  );
}

function StatCard({ label, value, icon, color, bg, border }: {
  label: string; value: string | number;
  icon: React.ReactNode; color: string; bg: string; border: string;
}) {
  return (
    <div style={{ background: bg, border: `1px solid ${border}`, borderRadius: 'var(--radius-sm)', padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
      <div style={{ color, background: `${color}18`, borderRadius: 10, padding: 8, display: 'flex', flexShrink: 0 }}>{icon}</div>
      <div>
        <div style={{ fontSize: '1.55rem', fontWeight: 800, color, lineHeight: 1 }}>{value}</div>
        <div style={{ fontSize: '0.73rem', color: 'var(--text-secondary)', fontWeight: 600, marginTop: 3 }}>{label}</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// RFC 4180 Compliant CSV Helpers & Exporters (with UTF-8 BOM)
// ─────────────────────────────────────────────────────────────────────────────
function escapeCSV(val: unknown): string {
  if (val === null || val === undefined) return '';
  const str = String(val);
  if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
    return `"${str.replace(/"/g, '""')}"`;
  }
  return str;
}

function exportSummaryCSV(reports: ProjectReport[], onError?: (msg: string) => void) {
  if (reports.length === 0) {
    if (onError) onError('No projects found to export.');
    return;
  }
  const headers = [
    'Project ID',
    'Project Name',
    'Status',
    'Methodology',
    'Start Date',
    'End Date',
    'Total Tasks',
    'Completed (Done)',
    'In Progress',
    'To Do',
    'Blocked',
    'Overdue Tasks',
    'Actual Progress (%)',
    'Expected Progress (%)',
    'Progress Variance (%)',
    'Project Health Status',
    'Approved Budget (₹)',
    'Actual Cost (₹)',
    'Estimated Labor Cost (₹)',
    'Remaining Budget (₹)',
    'Budget Used (%)',
    'Budget Health Status',
    'Recorded Cost Entries Total (₹)',
    'Open Risks',
    'Critical Risks',
    'High Risks',
    'Team Members Count',
    'Avg Team Utilization (%)',
  ];

  const rows = reports.map(r => {
    const totalTasks = r.tasks.length;
    const doneTasks = r.tasks.filter(t => t.status === 'DONE').length;
    const inProgressTasks = r.tasks.filter(t => t.status === 'IN_PROGRESS').length;
    const todoTasks = r.tasks.filter(t => t.status === 'TODO').length;
    const blockedTasks = r.tasks.filter(t => t.status === 'BLOCKED').length;
    const overdueTasks = r.health?.overdueTasks ?? 0;

    const openRisks = r.risks ? r.risks.filter(x => x.status === 'OPEN').length : 0;
    const criticalRisks = r.risks ? r.risks.filter(x => x.status === 'OPEN' && x.severity === 'CRITICAL').length : 0;
    const highRisks = r.risks ? r.risks.filter(x => x.status === 'OPEN' && x.severity === 'HIGH').length : 0;

    const totalCost = r.costs ? r.costs.reduce((s, c) => s + toNum(c.amount), 0) : 0;

    const teamCount = r.workload?.teamWorkloads ? r.workload.teamWorkloads.length : 0;
    const avgUtil = (teamCount > 0 && r.workload?.teamWorkloads)
      ? (r.workload.teamWorkloads.reduce((s, m) => s + toNum(m.utilizationPercentage), 0) / teamCount).toFixed(1)
      : '';

    return [
      r.project.id,
      r.project.name,
      r.project.status,
      r.project.methodology || 'N/A',
      r.project.startDate || 'N/A',
      r.project.endDate || 'N/A',
      totalTasks,
      doneTasks,
      inProgressTasks,
      todoTasks,
      blockedTasks,
      overdueTasks,
      r.health ? toNum(r.health.actualProgress).toFixed(1) : '',
      r.health ? toNum(r.health.expectedProgress).toFixed(1) : '',
      r.health ? toNum(r.health.progressVariance).toFixed(1) : '',
      r.health?.projectHealth || 'N/A',
      r.budget ? toNum(r.budget.approvedBudget).toFixed(2) : (r.project.budget != null ? toNum(r.project.budget).toFixed(2) : ''),
      r.budget ? toNum(r.budget.actualCost).toFixed(2) : '',
      r.budget ? toNum(r.budget.estimatedLaborCost).toFixed(2) : '',
      r.budget ? toNum(r.budget.remainingBudget).toFixed(2) : '',
      r.budget ? toNum(r.budget.budgetUsedPercentage).toFixed(1) : '',
      r.budget?.budgetHealth || 'N/A',
      totalCost.toFixed(2),
      openRisks,
      criticalRisks,
      highRisks,
      teamCount,
      avgUtil,
    ].map(escapeCSV).join(',');
  });

  // UTF-8 BOM prefix (\uFEFF) ensures Excel and spreadsheet tools open the CSV with proper UTF-8 encoding
  const csvContent = '\uFEFF' + [headers.map(escapeCSV).join(','), ...rows].join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `Portfolio_Summary_Report_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function exportDetailedTasksCSV(reports: ProjectReport[], onError?: (msg: string) => void) {
  const headers = [
    'Project ID',
    'Project Name',
    'Project Status',
    'Task ID',
    'Task Title',
    'Description',
    'Status',
    'Priority',
    'Progress (%)',
    'Start Date',
    'Due Date',
    'Estimated Hours',
    'Actual Hours',
    'Duration (Days)',
  ];

  const rows: string[] = [];
  reports.forEach(r => {
    r.tasks.forEach(t => {
      rows.push([
        r.project.id,
        r.project.name,
        r.project.status,
        t.id,
        t.title,
        t.description || '',
        t.status,
        t.priority,
        t.progressPercentage ?? 0,
        t.startDate || '',
        t.dueDate || '',
        t.estimatedHours ?? '',
        t.actualHours ?? '',
        t.durationDays ?? '',
      ].map(escapeCSV).join(','));
    });
  });

  if (rows.length === 0) {
    if (onError) onError('No tasks found across projects to export.');
    return;
  }

  const csvContent = '\uFEFF' + [headers.map(escapeCSV).join(','), ...rows].join('\r\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `Detailed_Tasks_Report_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Cost Entry mini-form
// ─────────────────────────────────────────────────────────────────────────────
function AddCostForm({ projectId, onAdded }: { projectId: number; onAdded: () => void }) {
  const [open, setOpen]         = useState(false);
  const [cat, setCat]           = useState<CostCategory>('LABOR');
  const [desc, setDesc]         = useState('');
  const [amount, setAmount]     = useState('');
  const [date, setDate]         = useState(new Date().toISOString().slice(0, 10));
  const [saving, setSaving]     = useState(false);
  const [err, setErr]           = useState('');

  const submit = async () => {
    if (!amount || Number(amount) <= 0) { setErr('Amount must be > 0'); return; }
    setSaving(true); setErr('');
    try {
      const req: CreateCostEntryRequest = { category: cat, description: desc || undefined, amount: Number(amount), entryDate: date };
      await costApi.add(projectId, req);
      setOpen(false); setAmount(''); setDesc(''); onAdded();
    } catch {
      setErr('Failed to add cost entry.');
    } finally { setSaving(false); }
  };

  if (!open) {
    return (
      <button onClick={() => setOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: 5, background: 'none', border: '1px dashed var(--border-medium)', borderRadius: 6, padding: '5px 10px', fontSize: '0.78rem', color: 'var(--text-secondary)', cursor: 'pointer', marginTop: 8 }}>
        <Plus size={12} /> Add Cost Entry
      </button>
    );
  }

  return (
    <div style={{ marginTop: 10, padding: 12, background: 'var(--bg-card)', border: '1px solid var(--border-medium)', borderRadius: 8 }}>
      {err && <div style={{ color: '#dc2626', fontSize: '0.78rem', marginBottom: 6 }}>{err}</div>}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <select value={cat} onChange={e => setCat(e.target.value as CostCategory)}
          style={{ padding: '5px 8px', fontSize: '0.8rem', border: '1px solid var(--border-medium)', borderRadius: 5, background: 'var(--bg-secondary)' }}>
          {(['LABOR', 'MATERIAL', 'EQUIPMENT', 'OVERHEAD', 'OTHER'] as CostCategory[]).map(c => <option key={c}>{c}</option>)}
        </select>
        <input placeholder="Description" value={desc} onChange={e => setDesc(e.target.value)}
          style={{ flex: '1 1 120px', padding: '5px 8px', fontSize: '0.8rem', border: '1px solid var(--border-medium)', borderRadius: 5, background: 'var(--bg-secondary)' }} />
        <input type="number" placeholder="Amount ₹" value={amount} onChange={e => setAmount(e.target.value)} min={0}
          style={{ width: 100, padding: '5px 8px', fontSize: '0.8rem', border: '1px solid var(--border-medium)', borderRadius: 5, background: 'var(--bg-secondary)' }} />
        <input type="date" value={date} onChange={e => setDate(e.target.value)}
          style={{ padding: '5px 8px', fontSize: '0.8rem', border: '1px solid var(--border-medium)', borderRadius: 5, background: 'var(--bg-secondary)' }} />
        <button onClick={submit} disabled={saving}
          style={{ padding: '5px 12px', fontSize: '0.8rem', fontWeight: 600, background: '#0284c7', color: '#fff', border: 'none', borderRadius: 5, cursor: 'pointer' }}>
          {saving ? '…' : 'Save'}
        </button>
        <button onClick={() => setOpen(false)}
          style={{ padding: '5px 8px', background: 'none', border: '1px solid var(--border-medium)', borderRadius: 5, cursor: 'pointer', display: 'flex' }}>
          <X size={13} />
        </button>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-project collapsible report card
// ─────────────────────────────────────────────────────────────────────────────
function ProjectReportCard({
  report,
  isAnalysing,
  onRefreshCosts,
  onRunRiskAnalysis,
  onUpdateRiskStatus,
  onOpenWeeklyReport,
}: {
  report: ProjectReport;
  isAnalysing: boolean;
  onRefreshCosts: (projectId: number) => void;
  onRunRiskAnalysis: (projectId: number) => void;
  onUpdateRiskStatus: (projectId: number, riskId: number, status: string) => void;
  onOpenWeeklyReport: (projectId: number) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const { project, tasks, health, budget, workload, risks, costs } = report;

  const done       = tasks.filter(t => t.status === 'DONE').length;
  const blocked    = tasks.filter(t => t.status === 'BLOCKED').length;
  const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length;
  const todo       = tasks.filter(t => t.status === 'TODO').length;
  const openRisks  = risks ? risks.filter(r => r.status === 'OPEN').length : 0;
  const totalCost  = costs ? costs.reduce((s, c) => s + toNum(c.amount), 0) : null;

  const hp = health?.projectHealth;

  return (
    <div style={{ background: 'var(--bg-card)', border: '1px solid var(--border-light)', borderLeft: `4px solid ${HEALTH_COLOR(hp)}`, borderRadius: 'var(--radius-sm)', overflow: 'hidden', boxShadow: 'var(--shadow-sm)' }}>

      {/* ── Collapsed row ─────────────────────────────────── */}
      <div
        onClick={() => setExpanded(e => !e)}
        style={{ padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer', flexWrap: 'wrap' }}
      >
        {/* Name + subtitle */}
        <div style={{ flex: '1 1 220px', minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ fontWeight: 700, fontSize: '0.97rem', color: 'var(--text-primary)' }}>{project.name}</span>
            <span style={{ fontSize: '0.7rem', fontWeight: 700, padding: '2px 8px', borderRadius: 99,
              background: project.status === 'ACTIVE' ? '#ecfdf5' : project.status === 'COMPLETED' ? '#f0f9ff' : '#f8fafc',
              color: project.status === 'ACTIVE' ? '#059669' : project.status === 'COMPLETED' ? '#0284c7' : '#94a3b8',
              border: `1px solid ${project.status === 'ACTIVE' ? '#6ee7b7' : project.status === 'COMPLETED' ? '#bae6fd' : '#e2e8f0'}` }}>
              {project.status}
            </span>
          </div>
          <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', marginTop: 2 }}>
            {tasks.length} tasks · {done} done · {inProgress} active{blocked > 0 ? ` · ${blocked} blocked` : ''}
          </div>
        </div>

        {/* Health pill */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 5, padding: '4px 10px', borderRadius: 99,
          background: HEALTH_BG(hp), color: HEALTH_COLOR(hp), fontSize: '0.8rem', fontWeight: 700,
          border: `1px solid ${HEALTH_COLOR(hp)}40`, flexShrink: 0 }}>
          <HealthIcon h={hp} size={12} />
          {HEALTH_LABEL(hp)}
        </div>

        {/* Progress mini-bar */}
        <div style={{ flex: '0 0 140px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-secondary)', marginBottom: 3 }}>
            <span>Progress</span>
            <strong>{fmtPct(health?.actualProgress)}</strong>
          </div>
          <Bar value={toNum(health?.actualProgress)} color={HEALTH_COLOR(hp)} />
        </div>

        {/* Open risks */}
        {openRisks > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '3px 9px', borderRadius: 99,
            background: '#fef2f2', color: '#dc2626', fontSize: '0.76rem', fontWeight: 700,
            border: '1px solid #fca5a5', flexShrink: 0 }}>
            <AlertTriangle size={11} /> {openRisks} risk{openRisks !== 1 ? 's' : ''}
          </div>
        )}

        {/* Budget used */}
        {budget && (
          <div style={{ fontSize: '0.78rem', color: BUDGET_COLOR(budget.budgetHealth), fontWeight: 700, flexShrink: 0, padding: '3px 8px', borderRadius: 6, background: 'rgba(0,0,0,0.03)', border: '1px solid var(--border-light)' }}>
            Budget: {fmtPct(budget.budgetUsedPercentage)}
          </div>
        )}

        <div style={{ color: 'var(--text-muted)', flexShrink: 0 }}>
          {expanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
        </div>
      </div>

      {/* ── Expanded detail ─────────────────────────────────── */}
      {expanded && (
        <div style={{ borderTop: '1px solid var(--border-light)', padding: 18, display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 14 }}>

            {/* ── Task breakdown panel ── */}
            <div style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', padding: 14, border: '1px solid var(--border-light)' }}>
              <h4 style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 5 }}>
                <BarChart3 size={13} /> Task Breakdown
              </h4>
              {[
                { label: 'Done',        count: done,        color: '#059669' },
                { label: 'In Progress', count: inProgress,  color: '#0284c7' },
                { label: 'To Do',       count: todo,        color: '#94a3b8' },
                { label: 'Blocked',     count: blocked,     color: '#dc2626' },
              ].map(s => (
                <div key={s.label} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 7 }}>
                  <span style={{ width: 78, fontSize: '0.76rem', color: 'var(--text-secondary)', flexShrink: 0 }}>{s.label}</span>
                  <div style={{ flex: 1, height: 5, background: 'var(--border-light)', borderRadius: 99, overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: tasks.length ? `${(s.count / tasks.length) * 100}%` : '0%', background: s.color, borderRadius: 99 }} />
                  </div>
                  <span style={{ width: 22, textAlign: 'right', fontSize: '0.8rem', fontWeight: 700, color: s.color, flexShrink: 0 }}>{s.count}</span>
                </div>
              ))}
              {health && (
                <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid var(--border-light)', display: 'flex', flexWrap: 'wrap', gap: 12, fontSize: '0.76rem' }}>
                  <span><span style={{ color: 'var(--text-muted)' }}>Expected: </span><strong>{fmtPct(health.expectedProgress)}</strong></span>
                  <span>
                    <span style={{ color: 'var(--text-muted)' }}>Variance: </span>
                    <strong style={{ color: toNum(health.progressVariance) >= 0 ? '#059669' : '#dc2626' }}>
                      {toNum(health.progressVariance) >= 0 ? '+' : ''}{fmtPct(health.progressVariance)}
                    </strong>
                  </span>
                  {(health.overdueTasks ?? 0) > 0 && (
                    <span style={{ color: '#dc2626' }}>⚠ {health.overdueTasks} overdue</span>
                  )}
                </div>
              )}
            </div>

            {/* ── Budget panel ── */}
            <div style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', padding: 14, border: '1px solid var(--border-light)' }}>
              <h4 style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 5 }}>
                <TrendingUp size={13} /> Budget Health
              </h4>
              {budget ? (
                <>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: '0.8rem' }}>
                    {[
                      { label: 'Approved Budget', val: `₹${fmtMoney(budget.approvedBudget)}`, c: 'var(--text-primary)' },
                      { label: 'Actual Cost',      val: `₹${fmtMoney(budget.actualCost)}`,     c: 'var(--text-primary)' },
                      { label: 'Labor Estimate',   val: `₹${fmtMoney(budget.estimatedLaborCost)}`, c: '#7c3aed' },
                      {
                        label: 'Remaining',
                        val: `₹${fmtMoney(budget.remainingBudget)}`,
                        c: toNum(budget.remainingBudget) < 0 ? '#dc2626' : '#059669',
                      },
                    ].map(row => (
                      <div key={row.label} style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: 'var(--text-muted)' }}>{row.label}</span>
                        <strong style={{ color: row.c }}>{row.val}</strong>
                      </div>
                    ))}
                  </div>
                  <div style={{ marginTop: 10 }}>
                    <Bar value={toNum(budget.budgetUsedPercentage)} color={BUDGET_COLOR(budget.budgetHealth)} />
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, fontSize: '0.73rem' }}>
                      <span style={{ color: 'var(--text-muted)' }}>Used</span>
                      <span style={{ fontWeight: 700, color: BUDGET_COLOR(budget.budgetHealth) }}>
                        {fmtPct(budget.budgetUsedPercentage)} · {budget.budgetHealth}
                      </span>
                    </div>
                  </div>
                </>
              ) : (
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>No budget set for this project.</p>
              )}

              {/* Cost entries */}
              <div style={{ marginTop: 12, paddingTop: 10, borderTop: '1px solid var(--border-light)' }}>
                <div style={{ fontSize: '0.76rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: 6 }}>
                  Cost Entries {costs && costs.length > 0 && `(${costs.length})`}
                  {totalCost !== null && costs && costs.length > 0 && (
                    <span style={{ marginLeft: 8, color: '#dc2626' }}>Total: ₹{fmtMoney(totalCost)}</span>
                  )}
                </div>
                {costs && costs.length > 0 ? (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 4, maxHeight: 110, overflowY: 'auto' }}>
                    {costs.map(c => (
                      <div key={c.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.74rem', padding: '3px 6px', background: 'var(--bg-card)', borderRadius: 4 }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{c.category} {c.description ? `· ${c.description}` : ''}</span>
                        <strong style={{ color: 'var(--text-primary)' }}>₹{fmtMoney(c.amount)}</strong>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p style={{ fontSize: '0.76rem', color: 'var(--text-muted)' }}>No cost entries yet.</p>
                )}
                <AddCostForm projectId={project.id} onAdded={() => onRefreshCosts(project.id)} />
              </div>
            </div>

            {/* ── Workload panel ── */}
            {workload && workload.teamWorkloads.length > 0 && (
              <div style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', padding: 14, border: '1px solid var(--border-light)' }}>
                <h4 style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 5 }}>
                  <Users size={13} /> Team Workload
                </h4>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {workload.teamWorkloads.slice(0, 6).map(m => (
                    <div key={m.teamMemberId}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.76rem', marginBottom: 3 }}>
                        <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{m.teamMemberName}</span>
                        <span style={{ color: WORK_COLOR(m.workloadStatus), fontWeight: 700 }}>{fmtPct(m.utilizationPercentage)}</span>
                      </div>
                      <Bar value={toNum(m.utilizationPercentage)} color={WORK_COLOR(m.workloadStatus)} />
                      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginTop: 2 }}>
                        {m.assignedTaskCount} tasks · {toNum(m.plannedHours).toFixed(0)}h planned · {m.workloadStatus.replace('_', ' ')}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* ── Risks panel ── */}
            <div style={{ background: 'var(--bg-secondary)', borderRadius: 'var(--radius-sm)', padding: 14, border: '1px solid var(--border-light)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <h4 style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 5, margin: 0 }}>
                  <Shield size={13} /> AI Risks {risks && risks.length > 0 ? `(${risks.length})` : ''}
                </h4>
                <button
                  onClick={e => { e.stopPropagation(); onRunRiskAnalysis(project.id); }}
                  disabled={isAnalysing}
                  style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '3px 8px', fontSize: '0.72rem', fontWeight: 600,
                    background: '#fff7ed', border: '1px solid #fed7aa', borderRadius: 4, color: '#ea580c', cursor: isAnalysing ? 'wait' : 'pointer' }}
                  title="Run AI risk detection"
                >
                  <Zap size={11} className={isAnalysing ? 'spin' : ''} />
                  {isAnalysing ? 'Analysing…' : 'Analyse'}
                </button>
              </div>
              {risks && risks.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 240, overflowY: 'auto' }}>
                  {risks.map(r => (
                    <div key={r.id} style={{ padding: '8px 10px', background: RISK_BG(r.status), borderRadius: 6,
                      border: `1px solid ${SEV_COLOR(r.severity)}30` }}>
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 6, justifyContent: 'space-between' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flex: 1, minWidth: 0 }}>
                          <span style={{ width: 8, height: 8, borderRadius: '50%', background: SEV_COLOR(r.severity), flexShrink: 0, display: 'inline-block' }} />
                          <span style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.title}</span>
                        </div>
                        <select
                          value={r.status}
                          onClick={e => e.stopPropagation()}
                          onChange={e => onUpdateRiskStatus(project.id, r.id, e.target.value)}
                          style={{ fontSize: '0.68rem', fontWeight: 600, padding: '1px 4px', borderRadius: 4, border: '1px solid var(--border-medium)', background: 'var(--bg-card)', cursor: 'pointer' }}
                        >
                          <option value="OPEN">OPEN</option>
                          <option value="MITIGATED">MITIGATED</option>
                          <option value="ACCEPTED">ACCEPTED</option>
                          <option value="CLOSED">CLOSED</option>
                        </select>
                      </div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.4 }}>
                        {r.description}
                      </div>
                      {r.suggestedAction && (
                        <div style={{ fontSize: '0.7rem', color: '#047857', marginTop: 4, padding: '4px 6px', background: 'rgba(5,150,105,0.06)', borderRadius: 4 }}>
                          💡 <strong>Action:</strong> {r.suggestedAction}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  No risks detected. Click "Analyse" to run deterministic AI risk evaluation on schedules, budget, and resource workload.
                </p>
              )}
            </div>
          </div>

          {/* Open project link & Export Single Project CSV */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 10, paddingTop: 6, flexWrap: 'wrap' }}>
            <button
              onClick={(e) => { e.stopPropagation(); onOpenWeeklyReport(project.id); }}
              style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: '0.78rem', fontWeight: 700, color: '#7c3aed', background: '#f5f3ff', border: '1px solid #ddd6fe', borderRadius: 'var(--radius-xs)', padding: '5px 10px', cursor: 'pointer' }}
              title="Generate AI Executive Report for this project"
            >
              <Zap size={12} /> AI Weekly Report
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); exportSummaryCSV([report]); }}
              style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: '0.78rem', fontWeight: 600, color: '#059669', background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: 'var(--radius-xs)', padding: '5px 10px', cursor: 'pointer' }}
              title="Export this project report as CSV"
            >
              <Download size={12} /> Export Project CSV
            </button>
            <Link to={`/projects/${project.id}`} style={{ display: 'inline-flex', alignItems: 'center', gap: 5, fontSize: '0.8rem', fontWeight: 600, color: 'var(--primary)', textDecoration: 'none' }}>
              Open Project <ExternalLink size={11} />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Reports page
// ─────────────────────────────────────────────────────────────────────────────
type StatusFilter = 'ALL' | 'ACTIVE' | 'COMPLETED' | 'DRAFT' | 'ON_HOLD';

export default function Reports() {
  const [reports,  setReports]  = useState<ProjectReport[]>([]);
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState('');
  const [filter,   setFilter]   = useState<StatusFilter>('ALL');
  const [analysing, setAnalysing] = useState<number | null>(null);
  const [analysingAll, setAnalysingAll] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [weeklyReport, setWeeklyReport] = useState<WeeklyReport | null>(null);
  const [showWeeklyReport, setShowWeeklyReport] = useState(false);

  // ── load all data ──────────────────────────────────────────
  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [projects, ov] = await Promise.all([
        projectApi.list(),
        dashboardApi.getOverview().catch(() => null),
      ]);
      setOverview(ov);

      const rows = await Promise.all(
        projects.map(async (project): Promise<ProjectReport> => {
          const [tasks, health, budget, workload, risks, costs] = await Promise.all([
            taskApi.list(project.id).catch((): Task[] => []),
            trackingApi.getProgress(project.id).catch(() => null),
            trackingApi.getBudget(project.id).catch(() => null),
            trackingApi.getWorkload(project.id).catch(() => null),
            riskApi.list(project.id).catch((): ProjectRisk[] => []),
            costApi.list(project.id).catch((): CostEntry[] => []),
          ]);
          return { project, tasks, health, budget, workload, risks, costs };
        })
      );
      setReports(rows);
    } catch {
      setError('Failed to load reports. Make sure the backend is running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ── refresh only cost entries for one project ──────────────
  const refreshCosts = useCallback(async (projectId: number) => {
    try {
      const costs = await costApi.list(projectId);
      const budget = await trackingApi.getBudget(projectId).catch(() => null);
      setReports(prev => prev.map(r =>
        r.project.id === projectId ? { ...r, costs, budget } : r
      ));
    } catch { /* silent */ }
  }, []);

  // ── run risk analysis for one project ─────────────────────
  const runRiskAnalysis = useCallback(async (projectId: number) => {
    setAnalysing(projectId);
    try {
      await riskApi.analyze(projectId);
      const [risks, health, budget] = await Promise.all([
        riskApi.list(projectId),
        trackingApi.getProgress(projectId).catch(() => null),
        trackingApi.getBudget(projectId).catch(() => null),
      ]);
      setReports(prev => prev.map(r =>
        r.project.id === projectId ? { ...r, risks, health, budget } : r
      ));
      setToastMessage(`⚡ AI Risk Analysis completed for project #${projectId}`);
      setTimeout(() => setToastMessage(null), 3500);
    } catch {
      setError('Failed to run AI risk analysis for this project.');
    } finally {
      setAnalysing(null);
    }
  }, []);

  // ── run portfolio-wide AI risk analysis across all projects ──
  const runPortfolioRiskAnalysis = useCallback(async () => {
    if (reports.length === 0) return;
    setAnalysingAll(true);
    try {
      await Promise.all(
        reports.map(async r => {
          try {
            await riskApi.analyze(r.project.id);
          } catch { /* silent on single fail */ }
        })
      );
      // Reload fresh data and metrics
      await load();
      setToastMessage('⚡ Portfolio AI Risk Analysis completed across all projects!');
      setTimeout(() => setToastMessage(null), 4000);
    } catch {
      setError('Failed to run portfolio risk analysis.');
    } finally {
      setAnalysingAll(false);
    }
  }, [reports, load]);

  // ── open weekly report for specific project ────────────────
  const openWeeklyReport = useCallback(async (projectId: number) => {
    try {
      const report = await reportApi.getWeekly(projectId);
      setWeeklyReport(report);
      setShowWeeklyReport(true);
    } catch {
      setToastMessage('Failed to generate weekly report for this project.');
      setTimeout(() => setToastMessage(null), 4000);
    }
  }, []);

  // ── update risk status inline ─────────────────────────────
  const handleUpdateRiskStatus = useCallback(async (projectId: number, riskId: number, newStatus: string) => {
    try {
      const updated = await riskApi.updateStatus(projectId, riskId, newStatus);
      setReports(prev => prev.map(r => {
        if (r.project.id !== projectId || !r.risks) return r;
        return {
          ...r,
          risks: r.risks.map(x => x.id === riskId ? { ...x, status: updated.status } : x),
        };
      }));
      setToastMessage(`Risk status updated to ${newStatus}`);
      setTimeout(() => setToastMessage(null), 3000);
    } catch {
      setToastMessage('Failed to update risk status.');
      setTimeout(() => setToastMessage(null), 4000);
    }
  }, []);

  // ── derived stats ──────────────────────────────────────────
  const totalTasks     = reports.reduce((s, r) => s + r.tasks.length, 0);
  const totalDone      = reports.reduce((s, r) => s + r.tasks.filter(t => t.status === 'DONE').length, 0);
  const totalBlocked   = reports.reduce((s, r) => s + r.tasks.filter(t => t.status === 'BLOCKED').length, 0);
  const totalOpenRisks = reports.reduce((s, r) => s + (r.risks ? r.risks.filter(x => x.status === 'OPEN').length : 0), 0);
  const onTrack        = reports.filter(r => r.health?.projectHealth === 'ON_TRACK').length;
  const atRisk         = reports.filter(r => r.health?.projectHealth === 'AT_RISK').length;
  const offTrack       = reports.filter(r => r.health?.projectHealth === 'OFF_TRACK').length;
  const filtered       = filter === 'ALL' ? reports : reports.filter(r => r.project.status === filter);

  if (loading) return <LogoLoader message="Building reports…" />;

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto' }}>

      {/* ── Toast notification ───────────────────── */}
      {toastMessage && (
        <div style={{
          position: 'fixed', bottom: 24, right: 24, zIndex: 9999,
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '12px 18px', borderRadius: 'var(--radius-sm)',
          background: '#ecfdf5', border: '1px solid #6ee7b7', color: '#059669',
          boxShadow: 'var(--shadow-lg)', fontSize: '0.88rem', fontWeight: 600,
          animation: 'fadeInUp 0.2s ease',
        }}>
          <CheckCircle2 size={16} />
          {toastMessage}
        </div>
      )}

      {/* ── Page header ─────────────────────────── */}
      <div style={{ marginBottom: '1.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ display: 'inline-block', padding: '4px 12px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-full)', color: 'var(--primary)', fontSize: '0.76rem', fontWeight: 700, letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
            ANALYTICS &amp; EXPORTS
          </div>
          <h2 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.5px', color: 'var(--text-primary)', margin: 0 }}>
            Reports &amp; Performance
          </h2>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
            Portfolio-wide metrics across <strong>{reports.length}</strong> project{reports.length !== 1 ? 's' : ''}
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button
            onClick={runPortfolioRiskAnalysis}
            disabled={analysingAll}
            style={{
              display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px',
              background: '#fff7ed', border: '1px solid #fed7aa', borderRadius: 'var(--radius-sm)',
              color: '#ea580c', fontSize: '0.83rem', fontWeight: 700, cursor: analysingAll ? 'wait' : 'pointer',
            }}
            title="Trigger deterministic AI risk analysis across all projects in the portfolio"
          >
            <Zap size={14} className={analysingAll ? 'spin' : ''} />
            {analysingAll ? 'Analyzing Portfolio AI Risks…' : '⚡ Run AI Risk Analysis'}
          </button>
          <button onClick={() => exportSummaryCSV(filtered, (msg) => { setToastMessage(msg); setTimeout(() => setToastMessage(null), 3500); })}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: '#ecfdf5', border: '1px solid #6ee7b7', borderRadius: 'var(--radius-sm)', color: '#059669', fontSize: '0.83rem', fontWeight: 600, cursor: 'pointer' }}>
            <Download size={13} /> Export Summary CSV
          </button>
          <button onClick={() => exportDetailedTasksCSV(filtered, (msg) => { setToastMessage(msg); setTimeout(() => setToastMessage(null), 3500); })}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-medium)', borderRadius: 'var(--radius-sm)', color: 'var(--text-primary)', fontSize: '0.83rem', fontWeight: 600, cursor: 'pointer' }}>
            <Download size={13} /> Export Tasks CSV
          </button>
          <button
            onClick={() => {
              if (reports.length === 0) {
                setToastMessage('No projects found to generate report.');
                setTimeout(() => setToastMessage(null), 3000);
                return;
              }
              const targetProject = reports.find(r => r.project.status === 'ACTIVE' && r.tasks.length > 0) || reports.find(r => r.tasks.length > 0) || reports[0];
              reportApi.getWeekly(targetProject.project.id).then(report => {
                setWeeklyReport(report);
                setShowWeeklyReport(true);
              }).catch(() => {
                setToastMessage('Failed to generate weekly report. Ensure project has tasks.');
                setTimeout(() => setToastMessage(null), 4000);
              });
            }}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: 'linear-gradient(135deg,#7c3aed,#a855f7)', border: 'none', borderRadius: 'var(--radius-sm)', color: '#fff', fontSize: '0.83rem', fontWeight: 700, cursor: 'pointer', boxShadow: '0 2px 8px rgba(124,58,237,0.3)' }}
            title="Generate AI Executive Status Report for your project"
          >
            <Zap size={13} /> AI Weekly Report
          </button>
          <button onClick={load}
            style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: 'var(--primary-subtle)', border: '1px solid var(--primary-border)', borderRadius: 'var(--radius-sm)', color: 'var(--primary)', fontSize: '0.83rem', fontWeight: 600, cursor: 'pointer' }}>
            <RefreshCw size={13} /> Refresh
          </button>
        </div>
      </div>

      {/* ── Error ───────────────────────────────── */}
      {error && (
        <div style={{ padding: '12px 16px', background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 'var(--radius-sm)', color: '#dc2626', marginBottom: 20, fontSize: '0.88rem' }}>
          ⚠ {error}
        </div>
      )}

      {/* ── KPI stat cards ──────────────────────── */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 10, marginBottom: 20 }}>
        <StatCard label="Total Projects"  value={reports.length}  icon={<FileBarChart2 size={17} />} color="var(--primary)" bg="var(--primary-subtle)"  border="var(--primary-border)" />
        <StatCard label="On Track"        value={onTrack}         icon={<CheckCircle2 size={17} />}  color="#059669"         bg="#ecfdf5"                 border="#6ee7b7" />
        <StatCard label="At Risk"         value={atRisk}          icon={<AlertTriangle size={17} />} color="#d97706"         bg="#fffbeb"                 border="#fcd34d" />
        <StatCard label="Off Track"       value={offTrack}        icon={<XCircle size={17} />}       color="#dc2626"         bg="#fef2f2"                 border="#fca5a5" />
        <StatCard label="Total Tasks"     value={totalTasks}      icon={<Activity size={17} />}      color="#7c3aed"         bg="#f5f3ff"                 border="#c4b5fd" />
        <StatCard label="Completed"       value={totalDone}       icon={<TrendingUp size={17} />}    color="#059669"         bg="#ecfdf5"                 border="#6ee7b7" />
        <StatCard label="Blocked"         value={totalBlocked}    icon={<TrendingDown size={17} />}  color="#dc2626"         bg="#fef2f2"                 border="#fca5a5" />
        <StatCard label="Open Risks"      value={totalOpenRisks}  icon={<Shield size={17} />}        color="#ea580c"         bg="#fff7ed"                 border="#fed7aa" />
      </div>

      {/* ── Portfolio completion bar ─────────────── */}
      {totalTasks > 0 && (
        <div style={{ background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', padding: '14px 18px', marginBottom: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', fontWeight: 600, marginBottom: 6 }}>
            <span style={{ color: 'var(--text-secondary)' }}>Portfolio Completion</span>
            <span style={{ color: 'var(--text-primary)' }}>
              {totalDone} / {totalTasks} tasks &nbsp;({((totalDone / totalTasks) * 100).toFixed(1)}%)
            </span>
          </div>
          <div style={{ height: 10, background: 'var(--border-light)', borderRadius: 99, overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${(totalDone / totalTasks) * 100}%`, background: 'linear-gradient(90deg, #059669, #34d399)', borderRadius: 99, transition: 'width 0.5s ease' }} />
          </div>
          <div style={{ display: 'flex', gap: 18, marginTop: 8, flexWrap: 'wrap' }}>
            {[
              { label: 'Done',        count: totalDone,    color: '#059669' },
              { label: 'In Progress', count: reports.reduce((s, r) => s + r.tasks.filter(t => t.status === 'IN_PROGRESS').length, 0), color: '#0284c7' },
              { label: 'To Do',       count: reports.reduce((s, r) => s + r.tasks.filter(t => t.status === 'TODO').length, 0),        color: '#94a3b8' },
              { label: 'Blocked',     count: totalBlocked, color: '#dc2626' },
            ].map(s => (
              <div key={s.label} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: '0.73rem', color: 'var(--text-secondary)' }}>
                <span style={{ width: 7, height: 7, borderRadius: '50%', background: s.color, display: 'inline-block' }} />
                {s.label}: <strong style={{ color: 'var(--text-primary)', marginLeft: 3 }}>{s.count}</strong>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Filter tabs ──────────────────────────── */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 14, flexWrap: 'wrap' }}>
        {(['ALL', 'ACTIVE', 'DRAFT', 'COMPLETED', 'ON_HOLD'] as StatusFilter[]).map(f => (
          <button key={f} onClick={() => setFilter(f)}
            style={{ padding: '5px 12px', fontSize: '0.78rem', fontWeight: 600, borderRadius: 'var(--radius-full)',
              border: `1px solid ${filter === f ? 'var(--primary)' : 'var(--border-medium)'}`,
              background: filter === f ? 'var(--primary)' : 'var(--bg-card)',
              color: filter === f ? '#fff' : 'var(--text-secondary)',
              cursor: 'pointer', transition: 'all 0.15s' }}>
            {f === 'ALL' ? `All (${reports.length})` : `${f} (${reports.filter(r => r.project.status === f).length})`}
          </button>
        ))}
      </div>

      {/* ── Project report cards ─────────────────── */}
      {filtered.length === 0 ? (
        <div style={{ padding: '3rem', textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)', color: 'var(--text-secondary)' }}>
          {reports.length === 0 ? (
            <>
              <FileBarChart2 size={36} style={{ color: 'var(--text-muted)', margin: '0 auto 1rem', display: 'block' }} />
              <p style={{ fontWeight: 600, marginBottom: 6 }}>No projects found.</p>
              <Link to="/projects" style={{ color: 'var(--primary)', fontSize: '0.88rem', fontWeight: 600 }}>Create a project →</Link>
            </>
          ) : 'No projects match this filter.'}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600, marginBottom: 2 }}>
            Click any row to expand · {filtered.length} project{filtered.length !== 1 ? 's' : ''}
            {analysing && <span style={{ marginLeft: 10, color: '#ea580c' }}>⚡ Running risk analysis for project #{analysing}…</span>}
          </div>
          {filtered.map(r => (
            <ProjectReportCard
              key={r.project.id}
              report={r}
              isAnalysing={analysing === r.project.id || analysingAll}
              onRefreshCosts={refreshCosts}
              onRunRiskAnalysis={runRiskAnalysis}
              onUpdateRiskStatus={handleUpdateRiskStatus}
              onOpenWeeklyReport={openWeeklyReport}
            />
          ))}
        </div>
      )}

      {/* ── Dashboard summary strip ──────────────── */}
      {overview && (
        <div style={{ marginTop: 24, padding: 18, background: 'var(--bg-secondary)', border: '1px solid var(--border-light)', borderRadius: 'var(--radius-sm)' }}>
          <h4 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
            <Activity size={13} /> Portfolio Dashboard Summary
          </h4>
          <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
            {[
              { label: 'Total Budget',    value: `₹${fmtMoney(overview.totalBudget)}` },
              { label: 'Total Spent',     value: `₹${fmtMoney(overview.totalSpent)}` },
              { label: 'Active Projects', value: overview.activeProjects },
              { label: 'Global Health',   value: (overview.globalHealthStatus ?? 'IDLE').replace(/_/g, ' '), color: HEALTH_COLOR(overview.globalHealthStatus) },
              { label: 'Team Workload',   value: (overview.globalWorkloadStatus ?? 'IDLE').replace(/_/g, ' '), color: WORK_COLOR(overview.globalWorkloadStatus) },
            ].map(s => (
              <div key={s.label}>
                <div style={{ color: 'var(--text-muted)', fontSize: '0.72rem', fontWeight: 600, marginBottom: 2 }}>{s.label}</div>
                <div style={{ fontWeight: 800, fontSize: '0.95rem', color: (s as { color?: string }).color || 'var(--text-primary)' }}>{s.value}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── AI Weekly Report Modal ────────────────── */}
      {showWeeklyReport && weeklyReport && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 }}>
          <div style={{ background: 'var(--bg-card)', borderRadius: 14, width: '100%', maxWidth: 780, maxHeight: '90vh', overflow: 'auto', boxShadow: '0 24px 64px rgba(0,0,0,0.4)' }}>
            {/* Header */}
            <div style={{ padding: '20px 24px', borderBottom: '1px solid var(--border-light)', display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              background: weeklyReport.overallStatusColor === 'RED' ? 'linear-gradient(135deg,#dc2626,#ef4444)' : weeklyReport.overallStatusColor === 'AMBER' ? 'linear-gradient(135deg,#d97706,#f59e0b)' : 'linear-gradient(135deg,#059669,#10b981)', borderRadius: '14px 14px 0 0' }}>
              <div>
                <div style={{ color: 'rgba(255,255,255,0.85)', fontSize: '0.72rem', fontWeight: 700, letterSpacing: 1 }}>AI EXECUTIVE WEEKLY REPORT</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 4 }}>
                  <h3 style={{ color: '#fff', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>{weeklyReport.projectName}</h3>
                  {reports.length > 1 && (
                    <select
                      value={weeklyReport.projectId}
                      onChange={(e) => openWeeklyReport(Number(e.target.value))}
                      style={{ background: 'rgba(255,255,255,0.25)', border: '1px solid rgba(255,255,255,0.4)', borderRadius: 6, color: '#fff', padding: '3px 8px', fontSize: '0.76rem', fontWeight: 700, outline: 'none', cursor: 'pointer' }}
                    >
                      {reports.map(r => (
                        <option key={r.project.id} value={r.project.id} style={{ color: '#1e293b' }}>
                          Switch: {r.project.name}
                        </option>
                      ))}
                    </select>
                  )}
                </div>
                <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: '0.78rem', marginTop: 2 }}>{weeklyReport.methodology} · {weeklyReport.reportDate}</div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <a href={reportApi.downloadPdf(weeklyReport.projectId)} download
                  style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', background: 'rgba(255,255,255,0.2)', border: '1px solid rgba(255,255,255,0.3)', borderRadius: 8, color: '#fff', fontSize: '0.8rem', fontWeight: 700, textDecoration: 'none', cursor: 'pointer' }}>
                  <Download size={14} /> Download PDF
                </a>
                <button onClick={() => setShowWeeklyReport(false)}
                  style={{ padding: '8px 12px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.3)', borderRadius: 8, color: '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
                  <X size={16} />
                </button>
              </div>
            </div>
            <div style={{ padding: '20px 24px' }}>
              {/* Executive summary */}
              <p style={{ fontSize: '0.9rem', lineHeight: 1.7, color: 'var(--text-secondary)', background: 'var(--bg-secondary)', padding: '12px 16px', borderRadius: 8, border: '1px solid var(--border-light)', marginBottom: 20 }}>
                {weeklyReport.executiveSummary}
              </p>
              {/* KPI grid */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(140px,1fr))', gap: 10, marginBottom: 20 }}>
                {[
                  { label: 'Schedule', value: weeklyReport.scheduleHealth.replace(/_/g,' '), color: weeklyReport.scheduleHealth === 'ON_TRACK' ? '#059669' : weeklyReport.scheduleHealth === 'AT_RISK' ? '#d97706' : '#dc2626' },
                  { label: 'Progress', value: `${weeklyReport.actualProgressPct.toFixed(1)}%` , color: 'var(--primary)' },
                  { label: 'Budget Used', value: `${weeklyReport.budgetUsedPct.toFixed(1)}%`, color: weeklyReport.budgetHealth === 'CRITICAL' ? '#dc2626' : weeklyReport.budgetHealth === 'HIGH' ? '#ea580c' : '#059669' },
                  { label: 'Delay Risk', value: `${weeklyReport.delayProbabilityPct.toFixed(0)}%`, color: weeklyReport.delayProbabilityPct > 50 ? '#dc2626' : weeklyReport.delayProbabilityPct > 25 ? '#d97706' : '#059669' },
                  { label: 'Open Risks', value: weeklyReport.openRisks, color: weeklyReport.openRisks > 3 ? '#dc2626' : '#d97706' },
                  { label: 'Team Load', value: `${weeklyReport.avgUtilizationPct.toFixed(0)}%`, color: weeklyReport.overloadedMembers > 0 ? '#ea580c' : '#059669' },
                ].map(k => (
                  <div key={k.label} style={{ background: 'var(--bg-secondary)', border: `1px solid ${k.color}30`, borderRadius: 8, padding: '12px 14px', textAlign: 'center' }}>
                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: k.color }}>{k.value}</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, marginTop: 2 }}>{k.label}</div>
                  </div>
                ))}
              </div>
              {/* Narrative sections */}
              {[
                { title: '✅ Key Accomplishments', items: weeklyReport.keyAccomplishments, color: '#059669' },
                { title: '🚧 Active Blockers', items: weeklyReport.activeBlockers, color: '#dc2626' },
                { title: '🎯 Next Steps', items: weeklyReport.nextStepRecommendations, color: 'var(--primary)' },
              ].map(s => (
                <div key={s.title} style={{ marginBottom: 16 }}>
                  <div style={{ fontSize: '0.82rem', fontWeight: 700, color: s.color, marginBottom: 6 }}>{s.title}</div>
                  <ul style={{ margin: 0, paddingLeft: 18 }}>
                    {s.items.map((item, i) => (
                      <li key={i} style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', marginBottom: 4, lineHeight: 1.5 }}>{item}</li>
                    ))}
                  </ul>
                </div>
              ))}
              {/* Milestone table */}
              {weeklyReport.milestones?.length > 0 && (
                <div style={{ marginTop: 8 }}>
                  <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 8 }}>📅 Milestone Snapshot</div>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem' }}>
                    <thead>
                      <tr style={{ background: 'var(--bg-secondary)' }}>
                        {['Milestone','Target','Progress','Status'].map(h => <th key={h} style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 700, color: 'var(--text-muted)', borderBottom: '1px solid var(--border-light)' }}>{h}</th>)}
                      </tr>
                    </thead>
                    <tbody>
                      {weeklyReport.milestones.map((m, i) => (
                        <tr key={i} style={{ borderBottom: '1px solid var(--border-light)' }}>
                          <td style={{ padding: '7px 10px', fontWeight: 600 }}>{m.name}</td>
                          <td style={{ padding: '7px 10px', color: 'var(--text-muted)' }}>{m.targetDate}</td>
                          <td style={{ padding: '7px 10px' }}>{m.completedTaskCount}/{m.totalTaskCount} ({m.completionPct.toFixed(0)}%)</td>
                          <td style={{ padding: '7px 10px' }}>
                            <span style={{ padding: '2px 8px', borderRadius: 99, fontSize: '0.72rem', fontWeight: 700,
                              background: m.status === 'COMPLETED' ? '#ecfdf5' : m.status === 'MISSED' ? '#fef2f2' : '#fffbeb',
                              color: m.status === 'COMPLETED' ? '#059669' : m.status === 'MISSED' ? '#dc2626' : '#d97706' }}>
                              {m.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
