import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import type { Task, TaskInput, TaskStatus } from '../types/task';
import type { WorkArea } from '../types/area';
import { areasPorId, listAreas } from '../api/areas';
import { createTask, getTask, listTasks, updateTask } from '../api/tasks';
import TaskList from '../components/task/TaskList';
import TaskSchedule from '../components/task/TaskSchedule';
import TaskForm from '../components/task/TaskForm';
import TaskDetail from '../components/task/TaskDetail';
import AiPanel from '../components/task/AiPanel';

type Modo = 'lista' | 'criar' | 'editar' | 'detalhe';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function TasksPage() {
  const [searchParams] = useSearchParams();
  const [tarefas, setTarefas] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFiltro, setStatusFiltro] = useState<TaskStatus | ''>('');
  const [modo, setModo] = useState<Modo>('lista');
  const [selecionada, setSelecionada] = useState<Task | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [detalheVersao, setDetalheVersao] = useState(0);
  const [paiDaSelecionada, setPaiDaSelecionada] = useState<Task | null>(null);
  const [areas, setAreas] = useState<WorkArea[]>([]);

  useEffect(() => {
    let ativo = true;
    listAreas()
      .then((lista) => {
        if (ativo) setAreas(lista);
      })
      .catch(() => {
        if (ativo) setAreas([]);
      });
    return () => {
      ativo = false;
    };
  }, []);

  const mapaAreas = areasPorId(areas);

  useEffect(() => {
    let ativo = true;
    if (selecionada?.parentId) {
      getTask(selecionada.parentId)
        .then((pai) => {
          if (ativo) setPaiDaSelecionada(pai);
        })
        .catch(() => {
          if (ativo) setPaiDaSelecionada(null);
        });
    }
    return () => {
      ativo = false;
    };
  }, [selecionada?.id, selecionada?.parentId]);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const pagina = await listTasks({ size: 50, status: statusFiltro || undefined });
      setTarefas(pagina.content);
      setError(null);
    } catch (caught) {
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }, [statusFiltro]);

  useEffect(() => {
    listTasks({ size: 50, status: statusFiltro || undefined })
      .then((pagina) => {
        setTarefas(pagina.content);
        setError(null);
        setLoading(false);
      })
      .catch((caught) => {
        setError(messageOf(caught));
        setLoading(false);
      });
  }, [statusFiltro]);

useEffect(() => {
    const id = searchParams.get('tarefa');
    if (!id) {
      return;
    }
    let ativo = true;
    getTask(id)
      .then((tarefa) => {
        if (ativo) {
          setSelecionada(tarefa);
          setModo('detalhe');
        }
      })
      .catch(() => {
        if (ativo) setError(messageOf('Tarefa não encontrada.'));
      });
    return () => {
      ativo = false;
    };
  }, [searchParams]);

  function abrirDetalhe(tarefa: Task) {
  if (!tarefa.parentId) {
    setPaiDaSelecionada(null);
  }
  setSelecionada(tarefa);
  setModo('detalhe');
}

function voltar() {
  if (modo === 'editar' && selecionada) {
    setModo('detalhe');
    return;
  }
  if (modo === 'detalhe' && selecionada?.parentId && paiDaSelecionada) {
    setSelecionada(paiDaSelecionada);
    setModo('detalhe');
    return;
  }
  setModo('lista');
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

  const subModo =
    modo === 'criar'
      ? 'Nova tarefa'
      : modo === 'editar'
        ? 'Editar tarefa'
        : modo === 'detalhe' && selecionada
          ? selecionada.title
          : null;

  return (
    <section aria-labelledby="tasks-heading">
      <nav className="breadcrumb" aria-label="Trilha de navegação">
        <Link to="/">Dashboard</Link>
        <span className="breadcrumb__sep" aria-hidden="true">
          /
        </span>
        {subModo ? (
          <>
            <Link to="/tasks" onClick={() => setModo('lista')}>
              Tarefas
            </Link>
            {paiDaSelecionada && (
              <>
                <span className="breadcrumb__sep" aria-hidden="true">
                  /
                </span>
                <button
                  type="button"
                  className="breadcrumb__link"
                  onClick={() => abrirDetalhe(paiDaSelecionada)}
                >
                  {paiDaSelecionada.title}
                </button>
              </>
            )}
            <span className="breadcrumb__sep" aria-hidden="true">
              /
            </span>
            <span aria-current="page">{subModo}</span>
          </>
        ) : (
          <span aria-current="page">Tarefas</span>
        )}
      </nav>

      <header className="tasks__header">
        <div className="tasks__titulo">
          {subModo && (
            <button
              type="button"
              className="tasks__voltar--seta"
              aria-label="Voltar"
              onClick={voltar}
            >
              ←
            </button>
          )}
          <h2 id="tasks-heading">Tarefas</h2>
        </div>
        {modo === 'lista' && (
          <button type="button" onClick={() => setModo('criar')}>
            Nova tarefa
          </button>
        )}
      </header>

      {modo === 'lista' && (
        <TaskList
          tasks={tarefas}
          loading={loading}
          error={error}
          status={statusFiltro}
          onStatusChange={(status) => {
            setStatusFiltro(status);
            setLoading(true);
          }}
          onRetry={() => {
            setLoading(true);
            void carregar();
          }}
          onSelect={abrirDetalhe}
        />
      )}

      {modo === 'lista' && <TaskSchedule tarefas={tarefas} />}

      {(modo === 'criar' || modo === 'editar') && (
        <TaskForm
          key={modo === 'editar' && selecionada ? selecionada.id : 'novo'}
          task={modo === 'editar' ? selecionada : null}
          areas={areas}
          submitting={submitting}
          onSubmit={salvar}
          onCancel={voltar}
          error={formError}
        />
      )}

      {modo === 'detalhe' && selecionada && (
        <>
          <TaskDetail
            key={`${selecionada.id}-${detalheVersao}`}
            task={selecionada}
            areaNome={selecionada.areaId ? (mapaAreas.get(selecionada.areaId)?.title ?? null) : null}
            onChanged={aposTrocaStatus}
            onDeleted={aposExclusao}
            onEdit={() => setModo('editar')}
            onOpen={abrirDetalhe}
          />
          <AiPanel
            key={`ia-${selecionada.id}`}
            task={selecionada}
            onChanged={aposTrocaStatus}
            onSubtasksCreated={() => {
              void carregar();
              setDetalheVersao((versao) => versao + 1);
            }}
          />
        </>
      )}
    </section>
  );
}
