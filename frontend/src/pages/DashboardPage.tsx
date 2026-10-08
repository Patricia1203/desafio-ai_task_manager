import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import type { Task, TaskPriority, TaskSummary } from '../types/task';
import { getSummary, listTasks } from '../api/tasks';
import AsyncState from '../components/common/AsyncState';
import { dataHojeLocal, formatarDataBR } from '../utils/date';

const INDICADORES: { chave: keyof TaskSummary; rotulo: string }[] = [
  { chave: 'total', rotulo: 'Total de tarefas' },
  { chave: 'pending', rotulo: 'Pendentes' },
  { chave: 'inProgress', rotulo: 'Em andamento' },
  { chave: 'done', rotulo: 'Concluídas' },
  { chave: 'highPriority', rotulo: 'Alta prioridade' },
];

const PRIORIDADES: { chave: TaskPriority; rotulo: string }[] = [
  { chave: 'HIGH', rotulo: 'Alta' },
  { chave: 'MEDIUM', rotulo: 'Média' },
  { chave: 'LOW', rotulo: 'Baixa' },
];

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<TaskSummary | null>(null);
  const [tarefas, setTarefas] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const [resumo, pagina] = await Promise.all([getSummary(), listTasks({ size: 50 })]);
      setSummary(resumo);
      setTarefas(pagina.content);
      setError(null);
    } catch (caught) {
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.all([getSummary(), listTasks({ size: 50 })])
      .then(([resumo, pagina]) => {
        setSummary(resumo);
        setTarefas(pagina.content);
        setError(null);
        setLoading(false);
      })
      .catch((caught) => {
        setError(messageOf(caught));
        setLoading(false);
      });
  }, []);

  const hoje = dataHojeLocal();
  const abertasLista = tarefas.filter((tarefa) => tarefa.status !== 'DONE');
  const atrasadas = abertasLista.filter(
    (tarefa) => tarefa.dueDate !== null && tarefa.dueDate < hoje,
  ).length;
  const porPrioridade = PRIORIDADES.map(({ chave, rotulo }) => ({
    chave,
    rotulo,
    quantidade: abertasLista.filter((tarefa) => tarefa.priority === chave).length,
  }));
  const abertasCount = abertasLista.length;
  const proximosPrazos = abertasLista
    .filter((tarefa) => tarefa.dueDate !== null)
    .sort((a, b) => (a.dueDate as string).localeCompare(b.dueDate as string))
    .slice(0, 5);

  const total = summary?.total ?? 0;
  const abertas = summary ? summary.pending + summary.inProgress : 0;
  const concluidas = summary?.done ?? 0;
  const pct = total > 0 ? Math.round((concluidas / total) * 100) : 0;
  const pctDe = (parte: number, base: number) => (base > 0 ? Math.round((parte / base) * 100) : 0);
  const detalheDe = (valor: number, base: number) =>
    valor === 0 ? '0%' : `${valor} de ${base} · ${pctDe(valor, base)}%`;
  const detalhes: Record<keyof TaskSummary, string> = summary
    ? {
        total: `${pct}% concluídas`,
        pending: detalheDe(summary.pending, total),
        inProgress: detalheDe(summary.inProgress, total),
        done: detalheDe(summary.done, total),
        highPriority: detalheDe(summary.highPriority, total),
      }
    : { total: '', pending: '', inProgress: '', done: '', highPriority: '' };

  return (
    <section aria-labelledby="dashboard-heading">
      <AsyncState
        loading={loading}
        error={error}
        onRetry={() => {
          setLoading(true);
          void carregar();
        }}
      >
        {summary && (
          <>
            <header className="dashboard__hero">
              <div>
                <h2 id="dashboard-heading">Dashboard</h2>
                <p>
                  Você tem {abertas} tarefas em aberto, {summary.highPriority} com alta prioridade.{' '}
                  {atrasadas} {atrasadas === 1 ? 'está atrasada' : 'estão atrasadas'}.
                </p>
              </div>
              <div>
                <span className="dashboard__pct">{pct}%</span>
                <span className="dashboard__pct-rotulo">concluído</span>
                <div
                  className="dashboard__barra"
                  role="progressbar"
                  aria-label="Progresso geral"
                  aria-valuenow={pct}
                  aria-valuemin={0}
                  aria-valuemax={100}
                >
                  <i style={{ width: `${pct}%` }} />
                </div>
              </div>
              <div className="dashboard__atalhos">
                <Link to="/tasks">Ver tarefas</Link>
                <Link to="/assistente">Perguntar ao assistente</Link>
              </div>
            </header>

            <ul className="dashboard__indicadores">
              {INDICADORES.map(({ chave, rotulo }) => {
                const valor = summary[chave];
                const perigo = chave === 'highPriority' && valor >= 1;
                return (
                  <li key={chave} className={`dashboard__card${perigo ? ' dashboard__card--perigo' : ''}`}>
                    <span className="dashboard__valor">{valor}</span>
                    <span className="dashboard__rotulo">{rotulo}</span>
                    <span className="dashboard__detalhe">{detalhes[chave]}</span>
                  </li>
                );
              })}
            </ul>

            <div className="dashboard__colunas">
              <section className="dashboard__bloco" aria-label="Distribuição por status">
                <h3>Distribuição por status</h3>
                <div
                  className="dashboard__pilha"
                  role="img"
                  aria-label={`A fazer ${summary.pending}, em andamento ${summary.inProgress}, concluídas ${summary.done}`}
                >
                  <i
                    className="dashboard__seg--todo"
                    style={{ width: `${total > 0 ? (summary.pending / total) * 100 : 0}%` }}
                  />
                  <i
                    className="dashboard__seg--doing"
                    style={{ width: `${total > 0 ? (summary.inProgress / total) * 100 : 0}%` }}
                  />
                  <i
                    className="dashboard__seg--done"
                    style={{ width: `${total > 0 ? (summary.done / total) * 100 : 0}%` }}
                  />
                </div>
                <ul className="dashboard__legenda">
                  <li>
                    <i className="dashboard__seg--todo" /> A fazer · {summary.pending}
                  </li>
                  <li>
                    <i className="dashboard__seg--doing" /> Em andamento · {summary.inProgress}
                  </li>
                  <li>
                    <i className="dashboard__seg--done" /> Concluídas · {summary.done}
                  </li>
                </ul>
              </section>

              <section className="dashboard__bloco" aria-label="Abertas por prioridade">
                <h3>Abertas por prioridade</h3>
                {porPrioridade.map(({ chave, rotulo, quantidade }) => (
                  <div key={chave} className="dashboard__linha">
                    <span>{rotulo}</span>
                    <div className="dashboard__trilho">
                      <i
                        className={`dashboard__prio--${chave.toLowerCase()}`}
                        style={{ width: `${abertasCount > 0 ? (quantidade / abertasCount) * 100 : 0}%` }}
                      />
                    </div>
                    <span>{quantidade}</span>
                  </div>
                ))}
              </section>

              <section className="dashboard__bloco dashboard__bloco--largo" aria-label="Próximos prazos">
                <h3>Próximos prazos</h3>
                {proximosPrazos.length > 0 ? (
                  <ul className="dashboard__prazos">
                    {proximosPrazos.map((tarefa) => {
                      const atrasada = tarefa.dueDate !== null && tarefa.dueDate < hoje;
                      return (
                        <li key={tarefa.id}>
                          <strong>{tarefa.title}</strong>
                          <span className={`dashboard__data${atrasada ? ' dashboard__data--atrasada' : ''}`}>
                            {atrasada ? 'Atrasada · ' : ''}
                            {formatarDataBR(tarefa.dueDate)}
                          </span>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <p className="async-state__empty">Nenhum prazo próximo.</p>
                )}
              </section>
            </div>
          </>
        )}
      </AsyncState>
    </section>
  );
}