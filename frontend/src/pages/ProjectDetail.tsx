import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import TaskForm from '../components/TaskForm';
import TaskCard from '../components/TaskCard';
import { projectApi, taskApi, teamApi, Project, Task, TaskInput, TaskPriority, TaskStatus, TeamMember } from '../services/api';
import LogoLoader from '../components/LogoLoader';
import { WhatIfSimulatorModal } from '../components/WhatIfSimulatorModal';
import { Zap } from 'lucide-react';

export default function ProjectDetail() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  
  const [project, setProject] = useState<Project>();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);

  const load = () => 
    Promise.all([projectApi.get(projectId), taskApi.list(projectId), teamApi.list(projectId).catch(() => [])])
      .then(([p, t, team]) => {
        setProject(p);
        setTasks(t);
        setTeamMembers(team);
      })
      .catch(() => setError('Unable to load this project. Please check if it exists.'));

  useEffect(() => {
    load();
  }, [projectId]);

  const add = async (v: TaskInput) => {
    setActionError('');
    try {
      await taskApi.create(projectId, v);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not add task.');
    }
  };

  const changeStatus = async (taskId: number, s: TaskStatus) => {
    setActionError('');
    try {
      await taskApi.status(taskId, s);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not update task status.');
    }
  };

  const changePriority = async (taskId: number, p: TaskPriority) => {
    setActionError('');
    try {
      await taskApi.priority(taskId, p);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not update task priority.');
    }
  };

  const changeProgress = async (taskId: number, prog: number) => {
    setActionError('');
    try {
      await taskApi.progress(taskId, prog);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not update task progress.');
    }
  };

  const removeTask = async (taskId: number) => {
    setActionError('');
    try {
      await taskApi.remove(taskId);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not delete task.');
    }
  };

  const removeProject = async () => {
    if (window.confirm('Are you sure you want to delete this project? All associated tasks, dependencies, and risk logs will be permanently deleted.')) {
      try {
        await projectApi.remove(projectId);
        navigate('/projects');
      } catch (err: any) {
        setActionError(err.response?.data?.message || 'Could not delete project.');
      }
    }
  };

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {/* What-If Simulator Modal */}
      {project && (
        <WhatIfSimulatorModal
          projectId={projectId}
          projectName={project.name}
          isOpen={isSimulatorOpen}
          onClose={() => setIsSimulatorOpen(false)}
          onApplied={load}
        />
      )}

      <Link to="/projects" style={{ color: 'var(--primary-light)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: '0.9rem', marginBottom: 16, fontWeight: 500 }}>
        ← Back to Projects
      </Link>
      
      {error ? (
        <div className="glass-panel" style={{ padding: 20, borderColor: 'var(--accent-crimson)', color: '#ff7675' }}>{error}</div>
      ) : !project ? (
        <LogoLoader message="Loading project..." />
      ) : (
        <>
          {/* Project Details Header Panel */}
          <div className="glass-panel" style={{ padding: 28, marginBottom: 24, display: 'flex', flexDirection: 'column', gap: 16, borderColor: 'var(--primary-border)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 16 }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
                  <h2 style={{ fontSize: '1.85rem', fontWeight: 800, margin: 0, letterSpacing: '-0.5px' }}>{project.name}</h2>
                  <span className={`badge ${project.status === 'ACTIVE' ? 'badge-success' : 'badge-primary'}`}>{project.status}</span>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', maxWidth: '700px', lineHeight: 1.5 }}>
                  {project.description || 'No project description provided.'}
                </p>
              </div>
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                <button
                  onClick={() => setIsSimulatorOpen(true)}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 6,
                    padding: '10px 16px', fontSize: '0.9rem', fontWeight: 700,
                    borderRadius: 'var(--radius-xs)', border: '1px solid rgba(59, 130, 246, 0.4)',
                    background: 'linear-gradient(135deg, rgba(37, 99, 235, 0.15), rgba(6, 182, 212, 0.1))',
                    color: '#3b82f6', cursor: 'pointer',
                  }}
                >
                  <Zap size={15} /> 🔮 What-If Simulator
                </button>
                <Link
                  to={`/projects/${projectId}/gantt`}
                  className="btn btn-primary"
                  style={{ fontSize: '0.9rem', padding: '10px 18px', textDecoration: 'none' }}
                >
                  📊 Gantt / Schedule
                </Link>
                <Link
                  to={`/projects/${projectId}/team`}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.9rem', padding: '10px 18px', textDecoration: 'none', background: 'var(--secondary-subtle)', color: 'var(--secondary-light)', border: '1px solid var(--secondary-border)' }}
                >
                  👥 Team
                </Link>
                <Link
                  to={`/projects/${projectId}/risks`}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.9rem', padding: '10px 18px', textDecoration: 'none', background: 'rgba(250, 177, 160, 0.1)', color: '#fab1a0', border: '1px solid rgba(250, 177, 160, 0.2)' }}
                >
                  ⚠️ Risks
                </Link>
                <button 
                  className="btn btn-danger" 
                  onClick={removeProject}
                  style={{ padding: '10px 16px', fontSize: '0.9rem' }}
                >
                  Delete Project
                </button>
              </div>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, borderTop: '1px solid var(--border-light)', paddingTop: 16, fontSize: '0.88rem' }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Methodology:</span>{' '}
                <strong style={{ color: 'var(--primary-light)' }}>{project.methodology || 'CPM / Agile'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Start Date:</span>{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{project.startDate ? new Date(project.startDate).toLocaleDateString() : 'Immediate'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Target Deadline:</span>{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{project.endDate ? new Date(project.endDate).toLocaleDateString() : 'Open-ended'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Total Tasks:</span>{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{tasks.length}</strong>
              </div>
            </div>
          </div>

          {actionError && (
            <div className="glass-panel" style={{ padding: 12, marginBottom: 16, borderColor: 'var(--accent-crimson)', color: '#ff7675', fontSize: '0.9rem' }}>
              {actionError}
            </div>
          )}

          {/* Create Task Form */}
          <div style={{ marginBottom: 32 }}>
            <TaskForm onSave={add} />
          </div>

          {/* Tasks Grid */}
          <h3 style={{ fontSize: '1.35rem', fontWeight: 700, marginBottom: 16 }}>Project Deliverables &amp; Work Items</h3>
          {tasks.length === 0 ? (
            <div className="glass-panel" style={{ padding: '3rem 2rem', textAlign: 'center' }}>
              <p style={{ color: 'var(--text-muted)', fontSize: '1rem', margin: 0 }}>
                No tasks created yet for this project. Use the form above to add your first work item or use AI Project Planner!
              </p>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: 16 }}>
              {tasks.map(t => (
                <TaskCard
                  key={t.id}
                  task={t}
                  projectId={projectId}
                  teamMembers={teamMembers}
                  onStatus={(s) => changeStatus(t.id, s)}
                  onPriority={(p) => changePriority(t.id, p)}
                  onProgress={(prog) => changeProgress(t.id, prog)}
                  onDelete={() => removeTask(t.id)}
                />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
