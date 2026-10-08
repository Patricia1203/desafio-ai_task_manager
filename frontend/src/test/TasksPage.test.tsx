import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import { getSubtasks, getTask, listTasks } from '../api/tasks';
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

const raiz: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Mover casa',
  description: null,
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: '2026-11-01',
  parentId: null,
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
});