import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import TaskForm from '../components/TaskForm';
import TaskCard from '../components/TaskCard';
import { projectApi, taskApi, teamApi, Project, Task, TaskInput, TaskPriority, TaskStatus, TeamMember } from '../services/api';
import LogoLoader from '../components/LogoLoader';
export default function ProjectDetail() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  
  const [project, setProject] = useState<Project>();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');

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

  const changeProgress = async (taskId: number, progress: number) => {
    setActionError('');
    try {
      await taskApi.progress(taskId, progress);
      load();
    } catch (err: any) {
      setActionError(err.response?.data?.message || 'Could not update task progress.');
    }
  };

  const removeTask = async (taskId: number) => {
    if (confirm('Are you sure you want to delete this task?')) {
      setActionError('');
      try {
        await taskApi.remove(taskId);
        load();
      } catch (err: any) {
        setActionError(err.response?.data?.message || 'Could not delete task.');
      }
    }
  };

  const removeProject = async () => {
    if (confirm('Are you sure you want to delete this project and all its tasks?')) {
      setActionError('');
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
              <div style={{ display: 'flex', gap: 10 }}>
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
              {project.budget !== undefined && (
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>Budget:</span>{' '}
                  <strong style={{ color: 'var(--accent-emerald)' }}>${project.budget.toLocaleString(undefined, { minimumFractionDigits: 2 })}</strong>
                </div>
              )}
              {(project.startDate || project.endDate) && (
                <div>
                  <span style={{ color: 'var(--text-muted)' }}>Timeline:</span>{' '}
                  <strong style={{ color: 'var(--secondary)' }}>{project.startDate || '?'} to {project.endDate || '?'}</strong>
                </div>
              )}
            </div>
          </div>

          {/* Action Errors Banner */}
          {actionError && (
            <div className="glass-panel" style={{ padding: '12px 16px', marginBottom: 16, borderColor: '#f87171', backgroundColor: 'rgba(248, 113, 113, 0.05)', color: '#f87171', fontSize: '0.9rem', fontWeight: 500 }}>
              Error: {actionError}
            </div>
          )}

          {/* Tasks List Panel */}
          <div className="glass-panel" style={{ padding: 24 }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: 16 }}>Tasks ({tasks.length})</h3>
            <TaskForm onSave={add} />
            
            <div style={{ marginTop: 20 }}>
              {tasks.length === 0 ? (
                <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '2rem 0' }}>No tasks found in this project. Add one to get started.</p>
              ) : (
                tasks.map((t) => (
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
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
