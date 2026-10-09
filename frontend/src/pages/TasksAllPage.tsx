import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import type { Task, TaskStatus } from '../types/task';
import { listTasks } from '../api/tasks';
import TaskList from '../components/task/TaskList';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

/**
 * Lista completa de tarefas (F15). Fica fora de /tasks para o painel principal
 * manter a agenda/calendário; aqui ficam só os filtros e a lista, com navegação
 * para o detalhe reaproveitando o TaskDetail de /tasks?tarefa=<id>.
 */
export default function TasksAllPage() {
  const navigate = useNavigate();
  const [tarefas, setTarefas] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFiltro, setStatusFiltro] = useState<TaskStatus | ''>('');

  useEffect(() => {
    let ativo = true;
    listTasks({ size: 100, status: statusFiltro || undefined })
      .then((pagina) => {
        if (!ativo) return;
        setTarefas(pagina.content);
        setError(null);
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
  }, [statusFiltro]);

  const carregar = () => {
    setLoading(true);
    listTasks({ size: 100, status: statusFiltro || undefined })
      .then((pagina) => {
        setTarefas(pagina.content);
        setError(null);
      })
      .catch((caught) => setError(messageOf(caught)))
      .finally(() => setLoading(false));
  };

  return (
    <section aria-labelledby="tasks-all-heading">
      <nav className="breadcrumb" aria-label="Trilha de navegação">
        <Link to="/tasks">Tarefas</Link>
        <span className="breadcrumb__sep" aria-hidden="true">
          /
        </span>
        <span aria-current="page">Todas as tarefas</span>
      </nav>

      <header className="tasks__header">
        <h2 id="tasks-all-heading">Todas as tarefas</h2>
        <Link to="/tasks">Voltar ao painel</Link>
      </header>

      <TaskList
        tasks={tarefas}
        loading={loading}
        error={error}
        status={statusFiltro}
        onStatusChange={(status) => setStatusFiltro(status)}
        onRetry={carregar}
        onSelect={(task) => navigate(`/tasks?tarefa=${task.id}`)}
      />
    </section>
  );
}
