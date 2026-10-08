import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { WorkArea } from '../types/area';
import type { Task, TaskPriority } from '../types/task';
import { PRIORITY_LABELS, PRIORITY_OPTIONS } from '../types/task';
import { areaImageUrl, listAreas } from '../api/areas';
import { createTask, listTasks, updateTask } from '../api/tasks';
import TaskList from '../components/task/TaskList';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function AreaDetailPage() {
  const { areaId } = useParams();
  const navigate = useNavigate();
  const [areas, setAreas] = useState<WorkArea[]>([]);
  const [area, setArea] = useState<WorkArea | null>(null);
  const [areaErro, setAreaErro] = useState<string | null>(null);
  const [areaBusy, setAreaBusy] = useState(true);
  const [tarefas, setTarefas] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [mostrarCriar, setMostrarCriar] = useState(false);
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [prioridade, setPrioridade] = useState<TaskPriority>('MEDIUM');
  const [prazo, setPrazo] = useState('');
  const [criarBusy, setCriarBusy] = useState(false);
  const [criarError, setCriarError] = useState<string | null>(null);
  const [movendoId, setMovendoId] = useState<string | null>(null);
  const [moverErro, setMoverErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    if (!areaId) {
      return;
    }
    listAreas()
      .then((lista) => {
        if (ativo) {
          setAreas(lista);
          const encontrada = lista.find((item) => item.id === areaId);
          if (encontrada) {
            setArea(encontrada);
          } else {
            setAreaErro('Quadro não encontrado.');
          }
        }
      })
      .catch((caught) => {
        if (ativo) setAreaErro(messageOf(caught));
      })
      .finally(() => {
        if (ativo) setAreaBusy(false);
      });
    return () => {
      ativo = false;
    };
  }, [areaId]);

  useEffect(() => {
    let ativo = true;
    if (!areaId) {
      return;
    }
    listTasks({ areaId, size: 50 })
      .then((pagina) => {
        if (ativo) {
          setTarefas(pagina.content);
          setError(null);
        }
      })
      .catch((caught) => {
        if (ativo) setError(messageOf(caught));
      })
      .finally(() => {
        if (ativo) setLoading(false);
      });
    return () => {
      ativo = false;
    };
  }, [areaId]);

  const carregarTarefas = () => {
    if (!areaId) {
      return;
    }
    setLoading(true);
    listTasks({ areaId, size: 50 })
      .then((pagina) => {
        setTarefas(pagina.content);
        setError(null);
      })
      .catch((caught) => setError(messageOf(caught)))
      .finally(() => setLoading(false));
  };

  const outrosQuadros = areas.filter((item) => item.id !== areaId);

  async function criar(event: FormEvent) {
    event.preventDefault();
    const nome = titulo.trim();
    if (!nome) {
      setCriarError('Informe o título da tarefa.');
      return;
    }
    setCriarBusy(true);
    setCriarError(null);
    try {
      await createTask({
        title: nome,
        description: descricao.trim() || null,
        priority: prioridade,
        dueDate: prazo || null,
        estimatedTime: null,
        estimatedUnit: null,
        areaId: areaId ?? null,
      });
      setTitulo('');
      setDescricao('');
      setPrazo('');
      setMostrarCriar(false);
      carregarTarefas();
    } catch (caught) {
      setCriarError(messageOf(caught));
    } finally {
      setCriarBusy(false);
    }
  }

  async function mover(tarefa: Task, novoAreaId: string) {
    setMovendoId(tarefa.id);
    setMoverErro(null);
    try {
      await updateTask(tarefa.id, {
        title: tarefa.title,
        description: tarefa.description,
        priority: tarefa.priority,
        dueDate: tarefa.dueDate,
        estimatedTime: tarefa.estimatedTime,
        estimatedUnit: tarefa.estimatedUnit,
        areaId: novoAreaId,
      });
      carregarTarefas();
    } catch (caught) {
      setMoverErro(`Não foi possível mover "${tarefa.title}": ${messageOf(caught)}`);
    } finally {
      setMovendoId(null);
    }
  }

  return (
    <section aria-labelledby="area-heading">
      <nav className="breadcrumb" aria-label="Trilha de navegação">
        <Link to="/">Dashboard</Link>
        <span className="breadcrumb__sep" aria-hidden="true">
          /
        </span>
        <Link to="/areas">Quadros</Link>
        <span className="breadcrumb__sep" aria-hidden="true">
          /
        </span>
        <span aria-current="page">{area ? area.title : 'Detalhe'}</span>
      </nav>

      {areaBusy && <p className="async-state__empty">Carregando quadro...</p>}
      {areaErro && !area && (
        <>
          <p className="error-message" role="alert">
            {areaErro}
          </p>
          <Link to="/areas">Voltar para quadros</Link>
        </>
      )}

      {area && (
        <>
          <header className="area-detail__cabecalho">
            {area.imageType ? (
              <img className="area-detail__foto" src={areaImageUrl(area.id)} alt="" />
            ) : (
              <span
                className="area-detail__foto area-detail__foto--vazia"
                aria-hidden="true"
              />
            )}
            <h2 id="area-heading">{area.title}</h2>
            <p className="area-detail__contagem">
              {tarefas.length} {tarefas.length === 1 ? 'tarefa' : 'tarefas'} neste quadro
            </p>
            <button
              type="button"
              className="tasks__voltar--seta"
              aria-label="Voltar"
              onClick={() => navigate('/areas')}
            >
              ←
            </button>
          </header>

          <div className="area-detail__acoes">
            {!mostrarCriar ? (
              <button type="button" onClick={() => setMostrarCriar(true)}>
                Nova tarefa neste quadro
              </button>
            ) : (
              <form className="task-form" onSubmit={criar} noValidate>
                <h3 className="task-form__title">Nova tarefa em "{area.title}"</h3>

                <label htmlFor="area-task-titulo" className="field">
                  <span className="field__label">Título *</span>
                  <input
                    id="area-task-titulo"
                    value={titulo}
                    maxLength={200}
                    onChange={(event) => setTitulo(event.target.value)}
                  />
                </label>

                <label htmlFor="area-task-descricao" className="field">
                  <span className="field__label">Descrição</span>
                  <textarea
                    id="area-task-descricao"
                    rows={3}
                    value={descricao}
                    maxLength={5000}
                    onChange={(event) => setDescricao(event.target.value)}
                  />
                </label>

                <label htmlFor="area-task-prioridade" className="field">
                  <span className="field__label">Prioridade</span>
                  <select
                    id="area-task-prioridade"
                    value={prioridade}
                    onChange={(event) => setPrioridade(event.target.value as TaskPriority)}
                  >
                    {PRIORITY_OPTIONS.map((opcao) => (
                      <option key={opcao} value={opcao}>
                        {PRIORITY_LABELS[opcao]}
                      </option>
                    ))}
                  </select>
                </label>

                <label htmlFor="area-task-prazo" className="field">
                  <span className="field__label">Prazo</span>
                  <input
                    id="area-task-prazo"
                    type="date"
                    value={prazo}
                    onChange={(event) => setPrazo(event.target.value)}
                  />
                </label>

                {criarError && (
                  <p className="error-message" role="alert">
                    {criarError}
                  </p>
                )}

                <div className="task-form__actions">
                  <button type="submit" disabled={criarBusy}>
                    {criarBusy ? 'Criando...' : 'Criar tarefa'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setMostrarCriar(false)}
                    disabled={criarBusy}
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            )}
          </div>

          {moverErro && (
            <p className="error-message" role="alert">
              {moverErro}
            </p>
          )}

          <TaskList
            tasks={tarefas}
            loading={loading}
            error={error}
            onRetry={carregarTarefas}
            onSelect={(task) => navigate(`/tasks?tarefa=${task.id}`)}
            adicionaisPorTarefa={(tarefa) =>
              outrosQuadros.length > 0 ? (
                <label className="task-list__mover">
                  <span className="field__label">Mover para</span>
                  <select
                    aria-label={`Mover ${tarefa.title} para outro quadro`}
                    value=""
                    disabled={movendoId === tarefa.id}
                    onChange={(event) => {
                      const destino = event.target.value;
                      if (destino) {
                        void mover(tarefa, destino);
                      }
                    }}
                  >
                    <option value="">Nenhum</option>
                    {outrosQuadros.map((item) => (
                      <option key={item.id} value={item.id}>
                        {item.title}
                      </option>
                    ))}
                  </select>
                </label>
              ) : null
            }
          />
        </>
      )}
    </section>
  );
}