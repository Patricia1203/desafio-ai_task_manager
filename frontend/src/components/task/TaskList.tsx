import { useState } from 'react';
import type { Task } from '../../types/task';
import { PRIORITY_LABELS, STATUS_LABELS } from '../../types/task';
import AsyncState from '../common/AsyncState';

interface TaskListProps {
  tasks: Task[];
  loading: boolean;
  error: string | null;
  onRetry: () => void;
  onSelect: (task: Task) => void;
}

export default function TaskList({ tasks, loading, error, onRetry, onSelect }: TaskListProps) {
  const [filtro, setFiltro] = useState('');

  const filtradas = tasks.filter((task) =>
    task.title.toLowerCase().includes(filtro.trim().toLowerCase()),
  );

  return (
    <div className="task-list">
      <label htmlFor="task-list-filtro" className="field">
        <span className="field__label">Filtrar por título</span>
        <input
          id="task-list-filtro"
          type="search"
          value={filtro}
          onChange={(event) => setFiltro(event.target.value)}
        />
      </label>

      <AsyncState
        loading={loading}
        error={error}
        onRetry={onRetry}
        isEmpty={tasks.length === 0}
        emptyMessage="Nenhuma tarefa cadastrada. Crie a primeira!"
      >
        <ul className="task-list__items">
          {filtradas.map((task) => (
            <li key={task.id}>
              <button
                type="button"
                className="task-list__item"
                onClick={() => onSelect(task)}
              >
                <span className="task-list__titulo">{task.title}</span>
                <span className="task-list__meta">
                  <span className={`badge badge--${task.status.toLowerCase()}`}>
                    {STATUS_LABELS[task.status]}
                  </span>
                  <span className="badge badge--prioridade">
                    {PRIORITY_LABELS[task.priority]}
                  </span>
                  {task.subtaskCount > 0 && (
                    <span className="badge badge--subtarefas">
                      {task.subtaskCount} {task.subtaskCount === 1 ? 'subtarefa' : 'subtarefas'}
                    </span>
                  )}
                  {task.dueDate && <span className="task-list__prazo">Prazo: {task.dueDate}</span>}
                </span>
              </button>
            </li>
          ))}
        </ul>

        {filtradas.length === 0 && tasks.length > 0 && (
          <p role="status" className="async-state__empty">
            Nenhuma tarefa corresponde ao filtro.
          </p>
        )}
      </AsyncState>
    </div>
  );
}
