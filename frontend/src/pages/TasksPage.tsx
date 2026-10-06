import { useCallback, useEffect, useState } from 'react';
import type { Task, TaskInput, TaskStatus } from '../types/task';
import { STATUS_LABELS, STATUS_OPTIONS } from '../types/task';
import { createTask, listTasks, updateTask } from '../api/tasks';
import TaskList from '../components/task/TaskList';
import TaskForm from '../components/task/TaskForm';
import TaskDetail from '../components/task/TaskDetail';
import AiPanel from '../components/task/AiPanel';

type Modo = 'lista' | 'criar' | 'editar' | 'detalhe';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function TasksPage() {
  const [tarefas, setTarefas] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFiltro, setStatusFiltro] = useState<TaskStatus | ''>('');
  const [modo, setModo] = useState<Modo>('lista');
  const [selecionada, setSelecionada] = useState<Task | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const pagina = await listTasks(
        statusFiltro ? { status: statusFiltro, size: 50 } : { size: 50 },
      );
      setTarefas(pagina.conteudo);
    } catch (caught) {
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }, [statusFiltro]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  function abrirDetalhe(tarefa: Task) {
    setSelecionada(tarefa);
    setModo('detalhe');
  }

  async function salvar(input: TaskInput) {
    setSubmitting(true);
    setFormError(null);
    try {
      if (modo === 'editar' && selecionada) {
        const atualizada = await updateTask(selecionada.id, input);
        setSelecionada(atualizada);
        setModo('detalhe');
      } else {
        await createTask(input);
        setModo('lista');
      }
      await carregar();
    } catch (caught) {
      setFormError(messageOf(caught));
    } finally {
      setSubmitting(false);
    }
  }

  function aposExclusao() {
    setSelecionada(null);
    setModo('lista');
    carregar();
  }

  function aposTrocaStatus(atualizada: Task) {
    setSelecionada(atualizada);
    setTarefas((anteriores) =>
      anteriores.map((tarefa) => (tarefa.id === atualizada.id ? atualizada : tarefa)),
    );
  }

  return (
    <section aria-labelledby="tasks-heading">
      <header className="tasks__header">
        <h2 id="tasks-heading">Tarefas</h2>
        {modo === 'lista' && (
          <button type="button" onClick={() => setModo('criar')}>
            Nova tarefa
          </button>
        )}
      </header>

      {modo === 'lista' && (
        <>
          <label htmlFor="tasks-filtro-status" className="field">
            <span className="field__label">Filtrar por status</span>
            <select
              id="tasks-filtro-status"
              value={statusFiltro}
              onChange={(event) => setStatusFiltro(event.target.value as TaskStatus | '')}
            >
              <option value="">Todos</option>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABELS[status]}
                </option>
              ))}
            </select>
          </label>

          <TaskList
            tasks={tarefas}
            loading={loading}
            error={error}
            onRetry={carregar}
            onSelect={abrirDetalhe}
          />
        </>
      )}

      {(modo === 'criar' || modo === 'editar') && (
        <TaskForm
          key={modo === 'editar' && selecionada ? selecionada.id : 'novo'}
          task={modo === 'editar' ? selecionada : null}
          submitting={submitting}
          onSubmit={salvar}
          onCancel={() => setModo(selecionada && modo === 'editar' ? 'detalhe' : 'lista')}
          error={formError}
        />
      )}

      {modo === 'detalhe' && selecionada && (
        <>
          <button type="button" className="tasks__voltar" onClick={() => setModo('lista')}>
            Voltar para a lista
          </button>
          <TaskDetail
            task={selecionada}
            onChanged={aposTrocaStatus}
            onDeleted={aposExclusao}
            onEdit={() => setModo('editar')}
          />
          <AiPanel
            key={`ia-${selecionada.id}`}
            task={selecionada}
            onChanged={aposTrocaStatus}
            onSubtasksCreated={carregar}
          />
        </>
      )}
    </section>
  );
}
