import { useMemo } from 'react';
import { Gantt, Task as GanttLibTask, ViewMode } from 'gantt-task-react';
import 'gantt-task-react/dist/index.css';
import { GanttTask } from '../services/api';

interface GanttChartProps {
  tasks: GanttTask[];
  viewMode: ViewMode;
  onTaskClick?: (task: GanttTask) => void;
}

export default function GanttChart({ tasks, viewMode, onTaskClick }: GanttChartProps) {
  const libTasks: GanttLibTask[] = useMemo(() => {
    if (!tasks || tasks.length === 0) return [];

    return tasks.map((t) => {
      const start = new Date(t.scheduledStart);
      const end = new Date(t.scheduledEnd);
      // Ensure end is always after start (library requires it)
      if (end <= start) {
        end.setDate(start.getDate() + 1);
      }

      return {
        id: String(t.id),
        name: t.name,
        start,
        end,
        progress: t.progressPercentage,
        type: 'task' as const,
        isDisabled: false,
        styles: {
          backgroundColor: t.isCritical
            ? 'rgba(225, 112, 85, 0.88)'
            : 'rgba(9, 132, 227, 0.85)',
          backgroundSelectedColor: t.isCritical
            ? '#e17055'
            : '#0984e3',
          progressColor: t.isCritical
            ? '#d65a3d'
            : '#076ec0',
          progressSelectedColor: t.isCritical
            ? '#c0392b'
            : '#06528d',
        },
        dependencies: t.dependencies.map(String),
      };
    });
  }, [tasks]);

  if (libTasks.length === 0) {
    return (
      <div style={{
        textAlign: 'center',
        padding: '3rem',
        color: 'var(--text-secondary)',
        fontSize: '0.95rem',
      }}>
        <div style={{ fontSize: '2.5rem', marginBottom: 12 }}>📊</div>
        <p>No tasks with scheduled dates yet.</p>
        <p style={{ marginTop: 6, fontSize: '0.85rem' }}>
          Add tasks to your project and click <strong>Calculate Schedule</strong> to generate the Gantt chart.
        </p>
      </div>
    );
  }

  return (
    <div style={{ overflowX: 'auto' }}>
      <Gantt
        tasks={libTasks}
        viewMode={viewMode}
        onDoubleClick={(t) => {
          const original = tasks.find((gt) => String(gt.id) === t.id);
          if (original && onTaskClick) onTaskClick(original);
        }}
        listCellWidth="200px"
        columnWidth={viewMode === ViewMode.Day ? 60 : viewMode === ViewMode.Week ? 120 : 200}
        ganttHeight={Math.max(300, libTasks.length * 50 + 80)}
        todayColor="rgba(16, 185, 129, 0.15)"
        rowHeight={46}
        barCornerRadius={6}
        handleWidth={8}
        fontSize="13px"
      />
    </div>
  );
}
