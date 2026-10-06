import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import { changeStatus, deleteTask, getSubtasks } from '../api/tasks';
import TaskDetail from '../components/task/TaskDetail';

vi.mock('../api/tasks', () => ({
  changeStatus: vi.fn(),
  deleteTask: vi.fn(),
  getSubtasks: vi.fn(),
}));

const tarefa: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  titulo: 'Mover casa',
  descricao: null,
  status: 'A_FAZER',
  prioridade: 'MEDIA',
  prazo: null,
  idTarefaPai: null,
  criadoEm: '2026-10-01T10:00:00Z',
  atualizadoEm: '2026-10-01T10:00:00Z',
};

function montar() {
  const onChanged = vi.fn();
  const onDeleted = vi.fn();
  const onEdit = vi.fn();
  render(
    <TaskDetail task={tarefa} onChanged={onChanged} onDeleted={onDeleted} onEdit={onEdit} />,
  );
  return { onChanged, onDeleted, onEdit };
}

describe('TaskDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('altera o status e devolve a tarefa atualizada', async () => {
    const atualizada = { ...tarefa, status: 'EM_ANDAMENTO' as const };
    vi.mocked(changeStatus).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'EM_ANDAMENTO');

    expect(changeStatus).toHaveBeenCalledWith(tarefa.id, 'EM_ANDAMENTO');
    expect(await vi.waitFor(() => onChanged(atualizada))).toBeUndefined();
  });

  it('exibe o erro quando a troca de status falha', async () => {
    vi.mocked(changeStatus).mockRejectedValue(new Error('422: transicao invalida'));
    montar();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'CONCLUIDA');

    expect(await screen.findByRole('alert')).toHaveTextContent('422: transicao invalida');
  });

  it('lista as subtarefas no dialogo e so dispara o DELETE apos confirmar', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([
      { ...tarefa, id: '3f1d3f6e-0000-4000-8000-000000000002', titulo: 'Contratar empresa' },
      { ...tarefa, id: '3f1d3f6e-0000-4000-8000-000000000003', titulo: 'Desligar contadores' },
    ]);
    vi.mocked(deleteTask).mockResolvedValue(undefined);
    const { onDeleted } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    const dialogo = await screen.findByRole('alertdialog');
    expect(within(dialogo).getByText('Contratar empresa')).toBeInTheDocument();
    expect(within(dialogo).getByText('Desligar contadores')).toBeInTheDocument();
    expect(deleteTask).not.toHaveBeenCalled();

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }));

    expect(deleteTask).toHaveBeenCalledWith(tarefa.id);
    expect(onDeleted).toHaveBeenCalledOnce();
  });

  it('confirma direto a exclusao quando nao ha subtarefas', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([]);
    vi.mocked(deleteTask).mockResolvedValue(undefined);
    const { onDeleted } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    const dialogo = await screen.findByRole('alertdialog');
    expect(dialogo).not.toHaveTextContent('junto com as subtarefas');
    expect(deleteTask).not.toHaveBeenCalled();

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }));

    expect(deleteTask).toHaveBeenCalledWith(tarefa.id);
    expect(onDeleted).toHaveBeenCalledOnce();
  });

  it('cancela a exclusao sem chamar a API', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([
      { ...tarefa, id: '3f1d3f6e-0000-4000-8000-000000000002', titulo: 'Contratar empresa' },
    ]);
    const { onDeleted } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    const dialogo = await screen.findByRole('alertdialog');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

    expect(deleteTask).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
});
