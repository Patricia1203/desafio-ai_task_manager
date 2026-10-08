import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getSummary, listTasks } from '../api/tasks';
import type { Task, TaskSummary } from '../types/task';
import DashboardPage from './DashboardPage';

vi.mock('../api/tasks', () => ({
  getSummary: vi.fn(),
  listTasks: vi.fn(),
}));

const TAREFAS: Task[] = [
  {
    id: '1',
    title: 'Definir roadmap',
    description: null,
    status: 'IN_PROGRESS',
    priority: 'HIGH',
    dueDate: '2026-10-01',
    parentId: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    subtaskCount: 0,
    estimatedTime: null,
    estimatedUnit: null,
  },
  {
    id: '2',
    title: 'Revisar contrato',
    description: null,
    status: 'TODO',
    priority: 'MEDIUM',
    dueDate: '2026-10-20',
    parentId: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    subtaskCount: 0,
    estimatedTime: null,
    estimatedUnit: null,
  },
  {
    id: '3',
    title: 'Publicar notas',
    description: null,
    status: 'DONE',
    priority: 'MEDIUM',
    dueDate: null,
    parentId: null,
    createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z',
    subtaskCount: 0,
    estimatedTime: null,
    estimatedUnit: null,
  },
];

function resumo(valores: TaskSummary) {
  return valores;
}

function pagina(tarefas: Task[]) {
  return {
    content: tarefas,
    page: 0,
    size: 50,
    totalItems: tarefas.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renderiza o titulo da pagina', async () => {
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 0, pending: 0, inProgress: 0, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina([]));
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );
    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });

  it('mostra o hero, os indicadores e os blocos com dados reais', async () => {
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 12, pending: 5, inProgress: 3, done: 4, highPriority: 2 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina(TAREFAS));
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Total de tarefas')).toBeInTheDocument();
    expect(screen.getByText('Total de tarefas').closest('li')).toHaveTextContent('12');
    expect(screen.getByText('Pendentes').closest('li')).toHaveTextContent('5');
    expect(screen.getByText('Em andamento').closest('li')).toHaveTextContent('3');
    expect(screen.getByText('Concluídas').closest('li')).toHaveTextContent('4');
    expect(screen.getByText('Alta prioridade').closest('li')).toHaveTextContent('2');

    expect(screen.getByText('33% concluídas')).toBeInTheDocument();
    expect(screen.getByText('5 de 12 pendentes · 58% não pendentes')).toBeInTheDocument();
    expect(screen.getByText('3 de 12 em andamento · 75% fora de andamento')).toBeInTheDocument();
    expect(screen.getByText('4 de 12 concluídas · 67% não concluídas')).toBeInTheDocument();
    expect(screen.getByText('2 de 12 alta prioridade · 83% sem alta prioridade')).toBeInTheDocument();

    expect(screen.getByText('Alta prioridade').closest('li')).toHaveClass('dashboard__card--perigo');
    expect(screen.getByText('Total de tarefas').closest('li')).not.toHaveClass('dashboard__card--perigo');

    expect(screen.getByText(/Você tem 8 tarefas em aberto, 2 com alta prioridade\. 1 está atrasada\./)).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Progresso geral' })).toHaveAttribute(
      'aria-valuenow',
      '33',
    );
    expect(screen.getByRole('link', { name: 'Ver tarefas' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Perguntar ao assistente' })).toBeInTheDocument();

    expect(screen.getByRole('heading', { name: 'Distribuição por status' })).toBeInTheDocument();
    expect(screen.getByText('A fazer · 5')).toBeInTheDocument();
    expect(screen.getByText('Em andamento · 3')).toBeInTheDocument();
    expect(screen.getByText('Concluídas · 4')).toBeInTheDocument();

    const prioridades = screen.getByLabelText('Abertas por prioridade');
    expect(within(prioridades).getByText('Alta')).toBeInTheDocument();
    expect(within(prioridades).getByText('Média')).toBeInTheDocument();
    expect(within(prioridades).getByText('Baixa')).toBeInTheDocument();
    expect(within(prioridades).getByText('0')).toBeInTheDocument();

    expect(screen.getByRole('heading', { name: 'Próximos prazos' })).toBeInTheDocument();
    expect(screen.getByText('Definir roadmap')).toBeInTheDocument();
    expect(screen.getByText('Atrasada · 01/10/2026')).toBeInTheDocument();
    expect(screen.getByText('Revisar contrato')).toBeInTheDocument();
  });

  it('alta prioridade zerada nao fica vermelha nos KPIs', async () => {
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 4, pending: 2, inProgress: 1, done: 1, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina(TAREFAS));
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByText('Alta prioridade')).toBeInTheDocument();
    expect(screen.getByText('Alta prioridade').closest('li')).not.toHaveClass(
      'dashboard__card--perigo',
    );
    expect(screen.getByText('Alta prioridade').closest('li')).toHaveTextContent('Nenhuma alta prioridade');
    expect(screen.getByText('Alta prioridade').closest('li')).not.toHaveTextContent('de 4');
    expect(screen.getByText('2 de 4 pendentes · 50% não pendentes')).toBeInTheDocument();
    expect(screen.getByText('1 de 4 em andamento · 75% fora de andamento')).toBeInTheDocument();
    expect(screen.getByText('1 de 4 concluídas · 75% não concluídas')).toBeInTheDocument();
  });

  it('mostra o erro quando o resumo falha e permite tentar de novo', async () => {
    vi.mocked(getSummary)
      .mockRejectedValueOnce(new Error('Falhou a rede'))
      .mockResolvedValueOnce(
        resumo({ total: 1, pending: 1, inProgress: 0, done: 0, highPriority: 0 }),
      );
    vi.mocked(listTasks).mockResolvedValue(pagina([]));
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Falhou a rede');
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByText('Total de tarefas')).toBeInTheDocument();
    expect(getSummary).toHaveBeenCalledTimes(2);
  });
});