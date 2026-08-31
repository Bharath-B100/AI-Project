import { Task, TaskPriority, TaskStatus, TaskAssignment, assignmentApi, TeamMember } from '../services/api';
import { Trash2, Users, Plus, X } from 'lucide-react';
import { useState, useEffect } from 'react';

export default function TaskCard({
  task,
  projectId,
  teamMembers,
  onStatus,
  onPriority,
  onProgress,
  onDelete
}: {
  task: Task;
  projectId: number;
  teamMembers: TeamMember[];
  onStatus: (s: TaskStatus) => void;
  onPriority: (p: TaskPriority) => void;
  onProgress: (prog: number) => void;  // ← was missing from the interface; added to fix TS error
  onDelete?: () => void;
}) {
  const [assignments, setAssignments] = useState<TaskAssignment[]>([]);
  const [showAssignForm, setShowAssignForm] = useState(false);
  const [selectedMember, setSelectedMember] = useState<number | ''>('');
  const [plannedHours, setPlannedHours] = useState<number>(0);
  const [allocation, setAllocation] = useState<number>(100);

  useEffect(() => {
    if (projectId && task.id) {
      loadAssignments();
    }
  }, [projectId, task.id]);

  const loadAssignments = async () => {
    try {
      const data = await assignmentApi.list(projectId, task.id);
      setAssignments(data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleAssign = async () => {
    if (!selectedMember || plannedHours <= 0 || allocation <= 0) return;
    try {
      await assignmentApi.assign(projectId, task.id, {
        teamMemberId: Number(selectedMember),
        plannedHours,
        allocationPercentage: allocation
      });
      setShowAssignForm(false);
      setSelectedMember('');
      setPlannedHours(0);
      setAllocation(100);
      loadAssignments();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to assign task');
    }
  };

  const handleRemoveAssignment = async (assignmentId: number) => {
    try {
      await assignmentApi.remove(projectId, task.id, assignmentId);
      loadAssignments();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to remove assignment');
    }
  };

  const getPriorityBadge = (p: TaskPriority) => {
    switch (p) {
      case 'CRITICAL': return <span className="badge badge-critical">Critical</span>;
      case 'HIGH': return <span className="badge badge-secondary">High</span>;
      case 'MEDIUM': return <span className="badge badge-primary">Medium</span>;
      case 'LOW': return <span className="badge" style={{ background: 'rgba(255,255,255,0.06)', color: 'var(--text-secondary)', border: '1px solid var(--border-light)' }}>Low</span>;
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '16px 20px', marginBottom: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: '240px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <strong style={{ fontSize: '1.05rem', color: 'var(--text-primary)' }}>{task.title}</strong>
            {getPriorityBadge(task.priority)}
          </div>
          {task.description && <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: 8 }}>{task.description}</div>}
          <div style={{ color: 'var(--text-muted)', fontSize: '0.8rem', display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {task.startDate && <span>Start: <strong style={{ color: 'var(--text-secondary)' }}>{task.startDate}</strong></span>}
            <span>Due: <strong style={{ color: 'var(--text-secondary)' }}>{task.dueDate || 'Not set'}</strong></span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              Progress: 
              <input 
                type="number" 
                min={0} max={100} 
                value={task.progressPercentage} 
                onChange={(e) => onProgress(Number(e.target.value))}
                style={{ width: '50px', padding: '2px 4px', fontSize: '0.8rem', background: 'var(--bg-dark)', color: task.progressPercentage === 100 ? 'var(--accent-emerald)' : 'var(--primary-light)', border: '1px solid var(--border-light)', borderRadius: '4px' }}
              />%
            </span>
          </div>
        </div>
        
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <select value={task.status} onChange={(e) => onStatus(e.target.value as TaskStatus)} style={{ padding: '7px 12px', fontSize: '0.85rem' }}>
            <option value="TODO">TODO</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="BLOCKED">BLOCKED</option>
            <option value="DONE">DONE</option>
          </select>
          <select value={task.priority} onChange={(e) => onPriority(e.target.value as TaskPriority)} style={{ padding: '7px 12px', fontSize: '0.85rem' }}>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
          {onDelete && (
            <button onClick={onDelete} title="Delete Task" style={{ background: 'none', border: 'none', color: '#ff7675', cursor: 'pointer', padding: 8 }}>
              <Trash2 size={18} />
            </button>
          )}
        </div>
      </div>

      <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Users size={14} /> Assignees ({assignments.length})
          </h4>
          {!showAssignForm && (
            <button onClick={() => setShowAssignForm(true)} style={{ background: 'transparent', border: 'none', color: 'var(--primary-light)', fontSize: '0.85rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}>
              <Plus size={14} /> Add Assignee
            </button>
          )}
        </div>

        {assignments.length > 0 && (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {assignments.map(a => (
              <div key={a.id} style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '4px 8px 4px 10px', background: 'rgba(255,255,255,0.05)', borderRadius: 'var(--radius-full)', border: '1px solid var(--border-light)', fontSize: '0.8rem' }}>
                <span style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{a.teamMemberName}</span>
                <span style={{ color: 'var(--text-muted)' }}>{a.allocationPercentage}% ({a.plannedHours}h)</span>
                <button onClick={() => handleRemoveAssignment(a.id)} style={{ background: 'none', border: 'none', color: '#ff7675', cursor: 'pointer', padding: 2, display: 'flex' }}><X size={12} /></button>
              </div>
            ))}
          </div>
        )}

        {showAssignForm && (
          <div style={{ display: 'flex', gap: 10, alignItems: 'center', background: 'var(--bg-dark)', padding: '10px 12px', borderRadius: 8, border: '1px solid var(--border-light)', marginTop: 4 }}>
            <select className="input" value={selectedMember} onChange={e => setSelectedMember(e.target.value ? Number(e.target.value) : '')} style={{ flex: 1, padding: '6px 10px', minWidth: '140px' }}>
              <option value="">Select Member...</option>
              {teamMembers.map(m => (
                <option key={m.id} value={m.id}>{m.name} ({m.role || 'Member'})</option>
              ))}
            </select>
            <input type="number" className="input" placeholder="Planned Hrs" value={plannedHours || ''} onChange={e => setPlannedHours(Number(e.target.value))} style={{ width: '100px', padding: '6px 10px' }} min={1} />
            <input type="number" className="input" placeholder="Alloc %" value={allocation} onChange={e => setAllocation(Number(e.target.value))} style={{ width: '80px', padding: '6px 10px' }} min={1} max={100} />
            <button onClick={handleAssign} className="btn btn-primary" style={{ padding: '6px 12px' }}>Save</button>
            <button onClick={() => setShowAssignForm(false)} className="btn btn-secondary" style={{ padding: '6px' }}><X size={16} /></button>
          </div>
        )}
      </div>
    </div>
  );
}
