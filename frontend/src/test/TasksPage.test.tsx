import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import { getSubtasks, getTask, listTasks } from '../api/tasks';
import { areasPorId, listAreas } from '../api/areas';
import TasksPage from '../pages/TasksPage';

vi.mock('../api/tasks', () => ({
  changeStatus: vi.fn(),
  createTask: vi.fn(),
  deleteTask: vi.fn(),
  getSubtasks: vi.fn(),
  getTask: vi.fn(),
  listTasks: vi.fn(),
  updateTask: vi.fn(),
}));

vi.mock('../api/areas', () => ({
  areasPorId: vi.fn(),
  listAreas: vi.fn(),
}));

const areas = [{ id: '3f1d3f6e-0000-4000-8000-000000000033', title: 'Moradia', imageType: null }];

const raiz: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Mover casa',
  description: null,
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '2026-11-01',
  parentId: null,
  areaId: areas[0].id,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  subtaskCount: 1,
  estimatedTime: null,
  estimatedUnit: null,
};

const filha: Task = {
  ...raiz,
  id: '3f1d3f6e-0000-4000-8000-000000000002',
  title: 'Contratar empresa',
  dueDate: null,
  parentId: raiz.id,
  subtaskCount: 0,
  estimatedTime: 2,
  estimatedUnit: 'HOURS',
};

function montar() {
  return render(
    <MemoryRouter>
      <TasksPage />
    </MemoryRouter>,
  );
}

describe('TasksPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listTasks).mockResolvedValue({
      content: [raiz, filha],
      page: 0,
      size: 50,
      totalItems: 2,
      totalPages: 1,
      first: true,
      last: true,
    });
    vi.mocked(getTask).mockResolvedValue(raiz);
    vi.mocked(getSubtasks).mockResolvedValue([]);
    vi.mocked(listAreas).mockResolvedValue(areas);
    vi.mocked(areasPorId).mockImplementation((lista) => new Map(lista.map((area) => [area.id, area])));
  });

  it('abre o detalhe da subtarefa com breadcrumb Tarefas / pai / subtarefa', async () => {
    montar();

    await userEvent.click(await screen.findByRole('button', { name: /Contratar empresa/ }));

    const trilha = screen.getByLabelText('Trilha de navegação');
    expect(within(trilha).getByText('Tarefas')).toBeInTheDocument();
    expect(within(trilha).getByRole('button', { name: 'Mover casa' })).toBeInTheDocument();
    expect(within(trilha).getByText('Contratar empresa')).toBeInTheDocument();
  });

  it('clicar no pai no breadcrumb volta para o detalhe do pai', async () => {
    montar();

    await userEvent.click(await screen.findByRole('button', { name: /Contratar empresa/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Mover casa' }));

    expect(await screen.findByRole('heading', { name: 'Mover casa' })).toBeInTheDocument();
  });

  it('clicar em voltar na subtarefa retorna ao detalhe da tarefa pai', async () => {
    montar();

    await userEvent.click(await screen.findByRole('button', { name: /Contratar empresa/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Voltar' }));

    expect(await screen.findByRole('heading', { name: 'Mover casa' })).toBeInTheDocument();
  });

  it('muda o status pelo filtro dentro do quadro e recarrega a lista', async () => {
    montar();

    await userEvent.selectOptions(screen.getByLabelText('Filtrar por status'), 'IN_PROGRESS');

    expect(listTasks).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'IN_PROGRESS' }),
    );
  });

  it('mostra o breadcrumb de Tarefas sem Dashboard e o link para todas', async () => {
    montar();

    const trilha = screen.getByLabelText('Trilha de navegação');
    expect(trilha).toHaveTextContent('Tarefas');
    expect(trilha).not.toHaveTextContent('Dashboard');
    expect(screen.getByRole('link', { name: 'Ver todas as tarefas' })).toHaveAttribute(
      'href',
      '/tasks/todas',
    );
  });

  it('exibe a linha do tempo com as proximas datas de vencimento', async () => {
    montar();

    expect(
      await screen.findByRole('heading', { name: 'Próximos 7 dias' }),
    ).toBeInTheDocument();
  });

  it('exibe o calendario do mes atual', async () => {
    montar();

    const hoje = new Date();
    const meses = [
      'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
      'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
    ];
    expect(
      await screen.findByRole('heading', {
        name: `${meses[hoje.getMonth()]} ${hoje.getFullYear()}`,
      }),
    ).toBeInTheDocument();
  });

  it('clicar em um dia do calendario abre o resumo com prioridade e descricao', async () => {
    function hojeISO() {
      const data = new Date();
      const mm = String(data.getMonth() + 1).padStart(2, '0');
      const dd = String(data.getDate()).padStart(2, '0');
      return `${data.getFullYear()}-${mm}-${dd}`;
    }

    vi.mocked(listTasks).mockResolvedValue({
      content: [
        {
          ...raiz,
          id: '3f1d3f6e-0000-4000-8000-000000000099',
          title: 'Revisar contrato',
          dueDate: hojeISO(),
          description: 'Conferir cláusulas antes de assinar.',
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    montar();

    expect(await screen.findByText('Revisar contrato')).toBeInTheDocument();

    const [ano, mes, diaNum] = hojeISO().split('-').map(Number);
    const rotulo = `Atividades de ${String(diaNum).padStart(2, '0')}/${String(mes).padStart(2, '0')}/${ano}`;

    const botoesDia = screen.getAllByRole('button', { name: rotulo });
    expect(botoesDia.length).toBeGreaterThanOrEqual(1);
    await userEvent.click(botoesDia[botoesDia.length - 1]);

    expect(
      await screen.findByRole('heading', { name: rotulo }),
    );

    const dialogo = screen.getByRole('dialog');
    expect(
      within(dialogo).getByText('Revisar contrato'),
    ).toBeInTheDocument();
    expect(within(dialogo).getByText('Conferir cláusulas antes de assinar.')).toBeInTheDocument();
    expect(within(dialogo).getByText('Média')).toBeInTheDocument();
    expect(within(dialogo).getByText('A fazer')).toBeInTheDocument();
  });
});