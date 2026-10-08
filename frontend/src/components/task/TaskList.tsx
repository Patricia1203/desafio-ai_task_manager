import { useState } from 'react';
import type { Task, TaskStatus } from '../../types/task';
import { PRIORITY_LABELS, STATUS_LABELS, STATUS_OPTIONS } from '../../types/task';
import { formatarDataBR } from '../../utils/date';
import AsyncState from '../common/AsyncState';

interface TaskListProps {
  tasks: Task[];
  loading: boolean;
  error: string | null;
  status?: TaskStatus | '';
  onStatusChange?: (status: TaskStatus) => void;
  onRetry: () => void;
  onSelect: (task: Task) => void;
  adicionaisPorTarefa?: (tarefa: Task) => React.ReactNode;
}

export default function TaskList({
  tasks,
  loading,
  error,
  status = '',
  onStatusChange,
  onRetry,
  onSelect,
  adicionaisPorTarefa,
}: TaskListProps) {
  const [filtro, setFiltro] = useState('');

  const filtradas = tasks.filter((task) =>
    task.title.toLowerCase().includes(filtro.trim().toLowerCase()),
  );

  return (
    <div className="task-list">
      <div className="task-list__filtros">
        {onStatusChange && (
          <label htmlFor="task-list-status" className="field">
            <span className="field__label">Filtrar por status</span>
            <select
              id="task-list-status"
              value={status}
              onChange={(event) => onStatusChange?.(event.target.value as TaskStatus)}
            >
              <option value="">Todos</option>
              {STATUS_OPTIONS.map((opcao) => (
                <option key={opcao} value={opcao}>
                  {STATUS_LABELS[opcao]}
                </option>
              ))}
            </select>
          </label>
        )}

        {tasks.length > 0 && (
          <label htmlFor="task-list-filtro" className="field">
            <span className="field__label">Filtrar por título</span>
            <input
              id="task-list-filtro"
              type="search"
              value={filtro}
              onChange={(event) => setFiltro(event.target.value)}
            />
          </label>
        )}
      </div>

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
                  <span className={`badge badge--prioridade badge--prioridade-${task.priority.toLowerCase()}`}>
                    {PRIORITY_LABELS[task.priority]}
                  </span>
                  {task.subtaskCount > 0 && (
                    <span className="badge badge--subtarefas">
                      {task.subtaskCount} {task.subtaskCount === 1 ? 'subtarefa' : 'subtarefas'}
                    </span>
                  )}
                  {task.dueDate && (
                    <span className="task-list__prazo">Prazo: {formatarDataBR(task.dueDate)}</span>
                  )}
                </span>
              </button>
              {adicionaisPorTarefa?.(task)}
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
