import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import { listTasks } from '../api/tasks';
import TasksAllPage from '../pages/TasksAllPage';

vi.mock('../api/tasks', () => ({
  listTasks: vi.fn(),
}));

const tarefa: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Pagar boleto',
  description: null,
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: null,
  parentId: null,
  areaId: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  subtaskCount: 0,
  estimatedTime: null,
  estimatedUnit: null,
};

function montar() {
  return render(
    <MemoryRouter initialEntries={['/tasks/todas']}>
      <Routes>
        <Route path="/tasks/todas" element={<TasksAllPage />} />
        <Route path="/tasks" element={<div>Painel de tarefas</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('TasksAllPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listTasks).mockResolvedValue({
      content: [tarefa],
      page: 0,
      size: 100,
      totalItems: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it('lista as tarefas sem a agenda', async () => {
    montar();

    expect(await screen.findByText('Pagar boleto')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Próximos 7 dias' })).not.toBeInTheDocument();
  });

  it('mostra o breadcrumb Tarefas / Todas as tarefas', async () => {
    montar();

    const trilha = await screen.findByLabelText('Trilha de navegação');
    expect(trilha).toHaveTextContent('Tarefas');
    expect(trilha).toHaveTextContent('Todas as tarefas');
    expect(trilha).not.toHaveTextContent('Dashboard');
  });

  it('filtra por status consultando o backend', async () => {
    montar();
    await screen.findByText('Pagar boleto');

    await userEvent.selectOptions(screen.getByLabelText('Filtrar por status'), 'IN_PROGRESS');

    expect(listTasks).toHaveBeenLastCalledWith(
      expect.objectContaining({ status: 'IN_PROGRESS' }),
    );
  });

  it('abre o detalhe da tarefa no painel principal', async () => {
    montar();

    await userEvent.click(await screen.findByRole('button', { name: /Pagar boleto/ }));

    expect(await screen.findByText('Painel de tarefas')).toBeInTheDocument();
  });
});
