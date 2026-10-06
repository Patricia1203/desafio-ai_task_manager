import { useCallback, useEffect, useState } from 'react';
import type { TaskSummary } from '../types/task';
import { getSummary } from '../api/tasks';
import AsyncState from '../components/common/AsyncState';

const INDICADORES: { chave: keyof TaskSummary; rotulo: string }[] = [
  { chave: 'total', rotulo: 'Total de tarefas' },
  { chave: 'pendentes', rotulo: 'Pendentes' },
  { chave: 'emAndamento', rotulo: 'Em andamento' },
  { chave: 'concluidas', rotulo: 'Concluídas' },
  { chave: 'altaPrioridade', rotulo: 'Alta prioridade' },
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
    setError(null);
    try {
      setSummary(await getSummary());
    } catch (caught) {
      setError(messageOf(caught));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  return (
    <section aria-labelledby="dashboard-heading">
      <h2 id="dashboard-heading">Dashboard</h2>

      <AsyncState loading={loading} error={error} onRetry={carregar}>
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
