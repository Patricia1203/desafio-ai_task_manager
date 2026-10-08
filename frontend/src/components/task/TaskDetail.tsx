import { useEffect, useState } from 'react';
import type { Task, TaskStatus } from '../../types/task';
import { PRIORITY_LABELS, STATUS_LABELS } from '../../types/task';
import { changeStatus, deleteTask, getSubtasks } from '../../api/tasks';
import { formatarDataBR } from '../../utils/date';
import { formatarTempoEstimado } from '../../utils/tempo';
import StatusSelect from './StatusSelect';

interface TaskDetailProps {
  task: Task;
  onChanged: (task: Task) => void;
  onDeleted: () => void;
  onEdit: () => void;
  onOpen: (task: Task) => void;
}

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function TaskDetail({ task, onChanged, onDeleted, onEdit, onOpen }: TaskDetailProps) {
  const [statusBusy, setStatusBusy] = useState(false);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [subtasksEmExclusao, setSubtasksEmExclusao] = useState<Task[] | null>(null);
  const [pendentesParaConcluir, setPendentesParaConcluir] = useState<Task[] | null>(null);
  const [subtasks, setSubtasks] = useState<Task[] | null>(null);
  const [subtasksBusy, setSubtasksBusy] = useState(true);
  const [subtasksError, setSubtasksError] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    getSubtasks(task.id)
      .then((lista) => {
        if (ativo) setSubtasks(lista);
      })
      .catch((error) => {
        if (ativo) setSubtasksError(messageOf(error));
      })
      .finally(() => {
        if (ativo) setSubtasksBusy(false);
      });
    return () => {
      ativo = false;
    };
  }, [task.id]);

  async function handleStatusChange(status: TaskStatus) {
    if (status === 'DONE') {
      try {
        const filhas = subtasks ?? (await getSubtasks(task.id));
        const pendentes = filhas.filter((subtask) => subtask.status !== 'DONE');
        if (pendentes.length > 0) {
          setPendentesParaConcluir(pendentes);
          return;
        }
      } catch (error) {
        setStatusError(messageOf(error));
        return;
      }
    }
    await trocarStatus(status, false);
  }

  async function trocarStatus(status: TaskStatus, completeSubtasks: boolean) {
    setStatusBusy(true);
    setStatusError(null);
    try {
      const atualizada = await changeStatus(task.id, status, completeSubtasks);
      onChanged(atualizada);
    } catch (error) {
      setStatusError(messageOf(error));
    } finally {
      setStatusBusy(false);
    }
  }

  async function confirmConcluir() {
    setStatusBusy(true);
    setStatusError(null);
    try {
      const atualizada = await changeStatus(task.id, 'DONE', true);
      setSubtasks((anteriores) =>
        (anteriores ?? []).map((subtask) =>
          subtask.status === 'DONE' ? subtask : { ...subtask, status: 'DONE' },
        ),
      );
      setPendentesParaConcluir(null);
      onChanged(atualizada);
    } catch (error) {
      setStatusError(messageOf(error));
    } finally {
      setStatusBusy(false);
    }
  }

  function cancelConcluir() {
    setPendentesParaConcluir(null);
    setStatusBusy(false);
  }

  async function handleDelete() {
    setDeleteBusy(true);
    setDeleteError(null);
    try {
      const filhas = subtasks ?? (await getSubtasks(task.id));
      setSubtasksEmExclusao(filhas);
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
  const emConclusao = pendentesParaConcluir !== null;

  return (
    <article className="task-detail" aria-labelledby="task-detail-titulo">
      <header className="task-detail__header">
        <h3 id="task-detail-titulo">{task.title}</h3>
        <span className={`badge badge--${task.status.toLowerCase()}`}>
          {STATUS_LABELS[task.status]}
        </span>
      </header>

      {task.description && <p className="task-detail__descricao">{task.description}</p>}

      <dl className="task-detail__meta">
        <div>
          <dt>Prioridade</dt>
          <dd>{PRIORITY_LABELS[task.priority]}</dd>
        </div>
        {task.parentId ? (
          <div>
            <dt>Tempo estimado</dt>
            <dd>
              {formatarTempoEstimado(task.estimatedTime, task.estimatedUnit) || 'Sem tempo estimado'}
            </dd>
          </div>
        ) : null}
        <div>
          <dt>Criada em</dt>
          <dd>{new Date(task.createdAt).toLocaleDateString('pt-BR')}</dd>
        </div>
        {task.parentId ? null : (
          <div>
            <dt>Prazo</dt>
            <dd>{task.dueDate ? formatarDataBR(task.dueDate) : 'Sem prazo'}</dd>
          </div>
        )}
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

      <section className="task-detail__subtarefas" aria-labelledby="task-detail-subtitulo">
        <h4 id="task-detail-subtitulo">Subtarefas</h4>
        {subtasksBusy && <p className="task-detail__carregando">Carregando subtarefas...</p>}
        {subtasksError && (
          <p className="error-message" role="alert">
            {subtasksError}
          </p>
        )}
        {subtasks && (
          <>
            {subtasks.length > 0 ? (
              <>
                <p className="task-detail__contagem">
                  {subtasks.length} {subtasks.length === 1 ? 'subtarefa' : 'subtarefas'}
                </p>
                <ul className="task-list__items">
                  {subtasks.map((subtask) => (
                    <li key={subtask.id}>
                      <button
                        type="button"
                        className="task-list__item task-list__item--sub"
                        onClick={() => onOpen(subtask)}
                      >
                        <span className="task-list__titulo">{subtask.title}</span>
                        <span className="task-list__meta">
                          <span className={`badge badge--${subtask.status.toLowerCase()}`}>
                            {STATUS_LABELS[subtask.status]}
                          </span>
                          <span className={`badge badge--prioridade badge--prioridade-${subtask.priority.toLowerCase()}`}>
                            {PRIORITY_LABELS[subtask.priority]}
                          </span>
                          {subtask.dueDate && (
                            <span className="task-list__prazo">
                              Prazo: {formatarDataBR(subtask.dueDate)}
                            </span>
                          )}
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              </>
            ) : (
              <p className="task-detail__vazio">Nenhuma subtarefa ainda.</p>
            )}
          </>
        )}
      </section>

      {emExclusao && (
        <div
          className="dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="dialogo-exclusao-titulo"
        >
          <h4 id="dialogo-exclusao-titulo">Excluir tarefa?</h4>
          <p>
            A tarefa <strong>{task.title}</strong> será excluída
            {subtasksEmExclusao.length > 0 ? ' junto com as subtarefas:' : '.'}
          </p>
          {subtasksEmExclusao.length > 0 && (
            <ul className="dialog__lista">
              {subtasksEmExclusao.map((subtask) => (
                <li key={subtask.id}>{subtask.title}</li>
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
    {emConclusao && (
        <div
          className="dialog"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="dialogo-conclusao-titulo"
        >
          <h4 id="dialogo-conclusao-titulo">Concluir tarefa e subtarefas?</h4>
          <p>
            A tarefa <strong>{task.title}</strong> será concluída junto com as subtarefas
            pendentes:
          </p>
          <ul className="dialog__lista">
            {pendentesParaConcluir.map((subtask) => (
              <li key={subtask.id}>{subtask.title}</li>
            ))}
          </ul>
          {statusError && (
            <p className="error-message" role="alert">
              {statusError}
            </p>
          )}
          <div className="dialog__acoes">
            <button
              type="button"
              className="button--danger"
              onClick={confirmConcluir}
              disabled={statusBusy}
            >
              {statusBusy ? 'Concluindo...' : 'Concluir'}
            </button>
            <button type="button" onClick={cancelConcluir} disabled={statusBusy}>
              Cancelar
            </button>
          </div>
        </div>
      )}
    </article>
  );
}