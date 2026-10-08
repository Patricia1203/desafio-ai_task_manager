import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getSubtasks, getSummary, listTasks } from '../api/tasks';
import type { Task, TaskSummary } from '../types/task';
import DashboardPage from './DashboardPage';

vi.mock('../api/tasks', () => ({
  getSubtasks: vi.fn(),
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
    areaId: null,
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
    areaId: null,
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
    areaId: null,
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

function OndeEsta() {
  const { search } = useLocation();
  return <div data-testid="tela-tasks">{search}</div>;
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

  it('expande a raiz e lista as subtarefas recuadas com o tempo para realizar', async () => {
    const pai: Task = {
      id: '4',
      title: 'Mover casa',
      description: null,
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: '2026-10-15',
      parentId: null,
      areaId: null,
      createdAt: '2026-10-01T10:00:00Z',
      updatedAt: '2026-10-01T10:00:00Z',
      subtaskCount: 1,
      estimatedTime: 1,
      estimatedUnit: 'HOURS',
    };
    const filha: Task = {
      ...pai,
      id: '4a',
      title: 'Contratar empresa',
      parentId: '4',
      subtaskCount: 0,
      estimatedTime: 2,
      estimatedUnit: 'DAYS',
    };
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 4, pending: 3, inProgress: 1, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina([...TAREFAS, pai]));
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    const bloco = await screen.findByLabelText('Próximos prazos');

    expect(within(bloco).getByText('15/10/2026')).toBeInTheDocument();
    expect(within(bloco).queryByText('Contratar empresa')).not.toBeInTheDocument();

    await userEvent.click(
      within(bloco).getByRole('button', { name: 'Ver subtarefas de Mover casa' }),
    );

    expect(getSubtasks).toHaveBeenCalledWith(pai.id);
    expect(await within(bloco).findByText('Contratar empresa')).toBeInTheDocument();
    expect(within(bloco).getByText('2 dias para realizar')).toBeInTheDocument();
    expect(within(bloco).getByText('Contratar empresa').closest('ul')).toHaveClass(
      'dashboard__prazos--nivel',
    );
    expect(
      within(bloco).getByRole('button', { name: 'Recolher subtarefas de Mover casa' }),
    ).toHaveAttribute('aria-expanded', 'true');

    await userEvent.click(
      within(bloco).getByRole('button', { name: 'Recolher subtarefas de Mover casa' }),
    );
    expect(within(bloco).queryByText('Contratar empresa')).not.toBeInTheDocument();
  });

  it('o clique no titulo da raiz tambem expande as subtarefas', async () => {
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 4, pending: 3, inProgress: 1, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(
      pagina([
        {
          ...TAREFAS[0],
          id: '4',
          title: 'Mover casa',
          dueDate: '2026-10-15',
          subtaskCount: 1,
          estimatedTime: null,
          estimatedUnit: null,
        },
      ]),
    );
    vi.mocked(getSubtasks).mockResolvedValue([{ ...TAREFAS[0], id: '4a', parentId: '4' }]);
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    const bloco = await screen.findByLabelText('Próximos prazos');
    await userEvent.click(within(bloco).getByRole('button', { name: 'Mover casa' }));

    expect(getSubtasks).toHaveBeenCalledWith('4');
    expect(await within(bloco).findByText('Definir roadmap')).toBeInTheDocument();
  });

  it('clicar na subtarefa navega para a tela dela', async () => {
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 4, pending: 3, inProgress: 1, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(
      pagina([
        {
          ...TAREFAS[0],
          id: '4',
          title: 'Mover casa',
          dueDate: '2026-10-15',
          subtaskCount: 1,
        },
      ]),
    );
    vi.mocked(getSubtasks).mockResolvedValue([
      { ...TAREFAS[0], id: '4a', title: 'Contratar empresa', parentId: '4', subtaskCount: 0 },
    ]);
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/tasks" element={<OndeEsta />} />
        </Routes>
      </MemoryRouter>,
    );

    const bloco = await screen.findByLabelText('Próximos prazos');
    await userEvent.click(within(bloco).getByRole('button', { name: 'Mover casa' }));
    await userEvent.click(await within(bloco).findByRole('button', { name: 'Contratar empresa' }));

    expect(await screen.findByTestId('tela-tasks')).toHaveTextContent('?tarefa=4a');
  });

  it('rola as subtarefas no espaco de 3 somente quando passa de 3', async () => {
    const pai: Task = {
      id: '5',
      title: 'Montar apresentacao',
      description: null,
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: '2026-10-18',
      parentId: null,
      areaId: null,
      createdAt: '2026-10-01T10:00:00Z',
      updatedAt: '2026-10-01T10:00:00Z',
      subtaskCount: 4,
      estimatedTime: null,
      estimatedUnit: null,
    };
    const filhas: Task[] = Array.from({ length: 4 }, (_, i) => ({
      ...pai,
      id: `5-${i}`,
      title: `Slide ${i + 1}`,
      parentId: pai.id,
      subtaskCount: 0,
    }));
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 4, pending: 4, inProgress: 0, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina([pai]));
    vi.mocked(getSubtasks).mockResolvedValue(filhas);
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    const bloco = await screen.findByLabelText('Próximos prazos');
    await userEvent.click(
      within(bloco).getByRole('button', { name: 'Ver subtarefas de Montar apresentacao' }),
    );

    const nivel = (await within(bloco).findByText('Slide 1')).closest('ul');
    expect(nivel).toHaveClass('dashboard__prazos--nivel');
    expect(nivel).toHaveClass('dashboard__prazos--rolagem');
  });

  it('sem passar de 3 subtarefas o bloco nao ganha a classe de rolagem', async () => {
    const pai: Task = {
      id: '5',
      title: 'Montar apresentacao',
      description: null,
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: '2026-10-18',
      parentId: null,
      areaId: null,
      createdAt: '2026-10-01T10:00:00Z',
      updatedAt: '2026-10-01T10:00:00Z',
      subtaskCount: 3,
      estimatedTime: null,
      estimatedUnit: null,
    };
    const filhas: Task[] = Array.from({ length: 3 }, (_, i) => ({
      ...pai,
      id: `5-${i}`,
      title: `Slide ${i + 1}`,
      parentId: pai.id,
      subtaskCount: 0,
    }));
    vi.mocked(getSummary).mockResolvedValue(
      resumo({ total: 3, pending: 3, inProgress: 0, done: 0, highPriority: 0 }),
    );
    vi.mocked(listTasks).mockResolvedValue(pagina([pai]));
    vi.mocked(getSubtasks).mockResolvedValue(filhas);
    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    const bloco = await screen.findByLabelText('Próximos prazos');
    await userEvent.click(
      within(bloco).getByRole('button', { name: 'Ver subtarefas de Montar apresentacao' }),
    );

    const nivel = (await within(bloco).findByText('Slide 1')).closest('ul');
    expect(nivel).toHaveClass('dashboard__prazos--nivel');
    expect(nivel).not.toHaveClass('dashboard__prazos--rolagem');
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