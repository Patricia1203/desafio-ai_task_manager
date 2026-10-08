import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { listAreas } from '../api/areas';
import { createTask, listTasks, updateTask } from '../api/tasks';
import AreaDetailPage from '../pages/AreaDetailPage';

vi.mock('../api/areas', () => ({
  areaImageUrl: (id: string) => `/api/areas/${id}/image`,
  listAreas: vi.fn(),
}));

vi.mock('../api/tasks', () => ({
  createTask: vi.fn(),
  listTasks: vi.fn(),
  updateTask: vi.fn(),
}));

const areaId = '3f1d3f6e-0000-4000-8000-000000000001';
const outroId = '3f1d3f6e-0000-4000-8000-000000000002';

function montar() {
  return render(
    <MemoryRouter initialEntries={[`/areas/${areaId}`]}>
      <Routes>
        <Route path="/areas/:areaId" element={<AreaDetailPage />} />
        <Route path="/tasks" element={<div>Tarefa aberta a partir da área</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AreaDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listAreas).mockResolvedValue([
      { id: areaId, title: 'Moradia', imageType: null },
      { id: outroId, title: 'Finanças', imageType: null },
    ]);
    vi.mocked(listTasks).mockResolvedValue({
      content: [
        {
          id: '3f1d3f6e-0000-4000-8000-0000000000aa',
          title: 'Pintar parede',
          description: null,
          status: 'TODO',
          priority: 'HIGH',
          dueDate: '2026-11-01',
          parentId: null,
          areaId,
          createdAt: '2026-10-01T10:00:00Z',
          updatedAt: '2026-10-01T10:00:00Z',
          subtaskCount: 0,
          estimatedTime: null,
          estimatedUnit: null,
        },
      ],
      page: 0,
      size: 50,
      totalItems: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    vi.mocked(createTask).mockResolvedValue({
      id: '3f1d3f6e-0000-4000-8000-0000000000bb',
      title: 'Comprar tinta',
      description: null,
      status: 'TODO',
      priority: 'MEDIUM',
      dueDate: null,
      parentId: null,
      areaId,
      createdAt: '2026-10-08T10:00:00Z',
      updatedAt: '2026-10-08T10:00:00Z',
      subtaskCount: 0,
      estimatedTime: null,
      estimatedUnit: null,
    });
  });

  it('mostra o cabecalho do quadro e as tarefas filtradas por ele', async () => {
    montar();

    expect(await screen.findByRole('heading', { name: 'Moradia' })).toBeInTheDocument();
    expect(screen.getByText('1 tarefa neste quadro')).toBeInTheDocument();
    expect(await screen.findByText('Pintar parede')).toBeInTheDocument();
    expect(listTasks).toHaveBeenCalledWith(expect.objectContaining({ areaId }));
  });

  it('navega para o detalhe da tarefa ao clicar nela', async () => {
    montar();

    await userEvent.click(await screen.findByRole('button', { name: /Pintar parede/ }));

    expect(await screen.findByText('Tarefa aberta a partir da área')).toBeInTheDocument();
  });

  it('mostra erro quando o quadro nao existe', async () => {
    vi.mocked(listAreas).mockResolvedValue([]);

    montar();

    expect(await screen.findByText('Quadro não encontrado.')).toBeInTheDocument();
  });

  it('cria uma tarefa vinculada ao quadro atual', async () => {
    montar();
    await screen.findByRole('heading', { name: 'Moradia' });

    await userEvent.click(screen.getByRole('button', { name: 'Nova tarefa neste quadro' }));
    await userEvent.type(screen.getByLabelText(/Título \*/), 'Comprar tinta');
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'HIGH');
    await userEvent.type(screen.getByLabelText('Prazo'), '2026-12-05');
    await userEvent.click(screen.getByRole('button', { name: 'Criar tarefa' }));

    expect(createTask).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Comprar tinta',
        priority: 'HIGH',
        dueDate: '2026-12-05',
        areaId,
      }),
    );
  });

  it('move uma tarefa para outro quadro usando o seletor', async () => {
    montar();
    await screen.findByText('Pintar parede');

    await userEvent.selectOptions(
      screen.getByLabelText('Mover Pintar parede para outro quadro'),
      outroId,
    );

    expect(updateTask).toHaveBeenCalledWith(
      '3f1d3f6e-0000-4000-8000-0000000000aa',
      expect.objectContaining({ areaId: outroId, title: 'Pintar parede' }),
    );
  });

  it('nao mostra o seletor de mover quando so existe um quadro', async () => {
    vi.mocked(listAreas).mockResolvedValue([
      { id: areaId, title: 'Moradia', imageType: null },
    ]);

    montar();
    await screen.findByText('Pintar parede');

    expect(
      screen.queryByLabelText('Mover Pintar parede para outro quadro'),
    ).not.toBeInTheDocument();
  });
});