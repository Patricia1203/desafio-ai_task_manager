import { useCallback, useEffect, useState } from 'react';
import type { TaskSummary } from '../types/task';
import { getSummary } from '../api/tasks';
import AsyncState from '../components/common/AsyncState';

const INDICADORES: { chave: keyof TaskSummary; rotulo: string }[] = [
  { chave: 'total', rotulo: 'Total de tarefas' },
  { chave: 'pending', rotulo: 'Pendentes' },
  { chave: 'inProgress', rotulo: 'Em andamento' },
  { chave: 'done', rotulo: 'Concluídas' },
  { chave: 'highPriority', rotulo: 'Alta prioridade' },
];

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function DashboardPage() {
  const [summary, setSummary] = useState<TaskSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    try {
      const resumo = await getSummary();
      setSummary(resumo);
      setError(null);
    } catch (caught) {
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getSummary()
      .then((resumo) => {
        setSummary(resumo);
        setError(null);
        setLoading(false);
      })
      .catch((caught) => {
        setError(messageOf(caught));
        setLoading(false);
      });
  }, []);

  return (
    <section aria-labelledby="dashboard-heading">
      <h2 id="dashboard-heading">Dashboard</h2>

      <AsyncState loading={loading} error={error} onRetry={() => { setLoading(true); void carregar(); }}>
        {summary && (
          <ul className="dashboard__indicadores">
            {INDICADORES.map(({ chave, rotulo }) => (
              <li key={chave} className="dashboard__card">
                <span className="dashboard__valor">{summary[chave]}</span>
                <span className="dashboard__rotulo">{rotulo}</span>
              </li>
            ))}
          </ul>
        )}
      </AsyncState>
    </section>
  );
}
