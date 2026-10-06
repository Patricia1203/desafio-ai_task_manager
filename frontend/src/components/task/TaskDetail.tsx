import { useState } from 'react';
import type { Task, TaskStatus } from '../../types/task';
import { PRIORITY_LABELS, STATUS_LABELS } from '../../types/task';
import { changeStatus, deleteTask, getSubtasks } from '../../api/tasks';
import StatusSelect from './StatusSelect';

interface TaskDetailProps {
  task: Task;
  onChanged: (task: Task) => void;
  onDeleted: () => void;
  onEdit: () => void;
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function TaskDetail({ task, onChanged, onDeleted, onEdit }: TaskDetailProps) {
  const [statusBusy, setStatusBusy] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [subtasksEmExclusao, setSubtasksEmExclusao] = useState<Task[] | null>(null);

  async function handleStatusChange(status: TaskStatus) {
    setStatusBusy(true);
    setStatusError(null);
    try {
      const atualizada = await changeStatus(task.id, status);
      onChanged(atualizada);
    } catch (error) {
      setStatusError(messageOf(error));
    } finally {
      setStatusBusy(false);
    }
  }

  async function handleDelete() {
    setDeleteBusy(true);
    setDeleteError(null);
    try {
      const subtasks = await getSubtasks(task.id);
      setSubtasksEmExclusao(subtasks);
      setDeleteBusy(false);
    } catch (error) {
      setDeleteError(messageOf(error));
      setDeleteBusy(false);
    }
  }

  async function confirmDelete() {
    setDeleteError(null);
    try {
      await deleteTask(task.id);
      onDeleted();
    } catch (error) {
      setDeleteError(messageOf(error));
      setDeleteBusy(false);
      setSubtasksEmExclusao(null);
    }
  }

  function cancelDelete() {
    setSubtasksEmExclusao(null);
    setDeleteBusy(false);
  }

  const emExclusao = subtasksEmExclusao !== null;

  return (
    <article className="task-detail" aria-labelledby="task-detail-titulo">
      <header className="task-detail__header">
        <h3 id="task-detail-titulo">{task.titulo}</h3>
        <span className={`badge badge--${task.status.toLowerCase()}`}>
          {STATUS_LABELS[task.status]}
        </span>
      </header>

      {task.descricao && <p className="task-detail__descricao">{task.descricao}</p>}

      <dl className="task-detail__meta">
        <div>
          <dt>Prioridade</dt>
          <dd>{PRIORITY_LABELS[task.prioridade]}</dd>
        </div>
        <div>
          <dt>Prazo</dt>
          <dd>{task.prazo ?? 'Sem prazo'}</dd>
        </div>
        <div>
          <dt>Criada em</dt>
          <dd>{new Date(task.criadoEm).toLocaleDateString('pt-BR')}</dd>
        </div>
      </dl>

      <div className="task-detail__acoes">
        <StatusSelect
          id="task-detail-status"
          value={task.status}
          onChange={handleStatusChange}
          disabled={statusBusy}
        />
        <button type="button" onClick={onEdit} disabled={statusBusy || deleteBusy}>
          Editar
        </button>
        <button
          type="button"
          className="button--danger"
          onClick={handleDelete}
          disabled={statusBusy || deleteBusy || emExclusao}
        >
          {deleteBusy ? 'Excluindo...' : 'Excluir'}
        </button>
      </div>

      {statusError && (
        <p className="error-message" role="alert">
          {statusError}
        </p>
      )}
      {deleteError && (
        <p className="error-message" role="alert">
          {deleteError}
        </p>
      )}

      {emExclusao && (
        <div
          className="dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="dialogo-exclusao-titulo"
        >
          <h4 id="dialogo-exclusao-titulo">Excluir tarefa?</h4>
          <p>
            A tarefa <strong>{task.titulo}</strong> será excluída
            {subtasksEmExclusao.length > 0 ? ' junto com as subtarefas:' : '.'}
          </p>
          {subtasksEmExclusao.length > 0 && (
            <ul className="dialog__lista">
              {subtasksEmExclusao.map((subtask) => (
                <li key={subtask.id}>{subtask.titulo}</li>
              ))}
            </ul>
          )}
          <div className="dialog__acoes">
            <button
              type="button"
              className="button--danger"
              onClick={confirmDelete}
              disabled={deleteBusy}
            >
              {deleteBusy ? 'Excluindo...' : 'Excluir'}
            </button>
            <button type="button" onClick={cancelDelete} disabled={deleteBusy}>
              Cancelar
            </button>
          </div>
        </div>
      )}
    </article>
  );
}
