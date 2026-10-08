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
  getTask: vi.fn(),
}));

const tarefa: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Mover casa',
  description: null,
  status: 'TODO',
  priority: 'MEDIUM',
  dueDate: null,
  parentId: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  subtaskCount: 0,
  estimatedTime: null,
  estimatedUnit: null,
};

const filha: Task = {
  ...tarefa,
  id: '3f1d3f6e-0000-4000-8000-000000000002',
  title: 'Contratar empresa',
};

function montar(sobrescrita: Partial<Task> = {}, onOpen = vi.fn()) {
  const onChanged = vi.fn();
  const onDeleted = vi.fn();
  const onEdit = vi.fn();
  render(
    <TaskDetail
      task={{ ...tarefa, ...sobrescrita }}
      onChanged={onChanged}
      onDeleted={onDeleted}
      onEdit={onEdit}
      onOpen={onOpen}
    />,
  );
  return { onChanged, onDeleted, onEdit, onOpen };
}

describe('TaskDetail', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getSubtasks).mockResolvedValue([]);
  });

  it('a raiz mostra o prazo em dd/mm/aaaa e esconde a linha de tempo', () => {
    montar({ dueDate: '2026-10-20' });

    expect(screen.getByText('20/10/2026')).toBeInTheDocument();
    expect(screen.queryByText('Tempo estimado')).not.toBeInTheDocument();
  });

  it('a raiz sem prazo mostra Sem prazo', () => {
    montar();

    expect(screen.getByText('Sem prazo')).toBeInTheDocument();
  });

  it('a subtarefa mostra o tempo estimado e esconde a linha de prazo', () => {
    montar({ parentId: tarefa.id, estimatedTime: 3, estimatedUnit: 'DAYS' });

    expect(screen.getByText('3 dias')).toBeInTheDocument();
    expect(screen.queryByText('Prazo')).not.toBeInTheDocument();
  });

  it('indica ausencia de tempo estimado na subtarefa', () => {
    montar({ parentId: tarefa.id });

    expect(screen.getByText('Sem tempo estimado')).toBeInTheDocument();
  });

  it('altera o status e devolve a tarefa atualizada', async () => {
    const atualizada = { ...tarefa, status: 'IN_PROGRESS' as const };
    vi.mocked(changeStatus).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'IN_PROGRESS');

    expect(changeStatus).toHaveBeenCalledWith(tarefa.id, 'IN_PROGRESS', false);
    expect(await vi.waitFor(() => onChanged(atualizada))).toBeUndefined();
  });

  it('exibe o erro quando a troca de status falha', async () => {
    vi.mocked(changeStatus).mockRejectedValue(new Error('422: transicao invalida'));
    montar();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE');

    expect(await screen.findByRole('alert')).toHaveTextContent('422: transicao invalida');
    expect(changeStatus).toHaveBeenCalledWith(tarefa.id, 'DONE', false);
  });

  it('conclui direto sem modal quando nao ha subtarefas pendentes', async () => {
    const atualizada = { ...tarefa, status: 'DONE' as const };
    vi.mocked(changeStatus).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE');

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(changeStatus).toHaveBeenCalledWith(tarefa.id, 'DONE', false);
    expect(await vi.waitFor(() => expect(onChanged).toHaveBeenCalledWith(atualizada)));
  });

  it('abre o modal ao concluir pai com subtarefas pendentes e cancelar nao chama a API', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    const { onChanged } = montar();

    await screen.findByRole('region');
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE');

    const dialogo = await screen.findByRole('alertdialog');
    expect(within(dialogo).getByText('Contratar empresa')).toBeInTheDocument();
    expect(changeStatus).not.toHaveBeenCalled();

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

    expect(changeStatus).not.toHaveBeenCalled();
    expect(onChanged).not.toHaveBeenCalled();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('confirma a conclusao com a flag e marca as subtarefas como concluidas', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    const atualizada = { ...tarefa, status: 'DONE' as const };
    vi.mocked(changeStatus).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await screen.findByRole('region');
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE');

    const dialogo = await screen.findByRole('alertdialog');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Concluir' }));

    expect(changeStatus).toHaveBeenCalledWith(tarefa.id, 'DONE', true);
    expect(onChanged).toHaveBeenCalledWith(atualizada);
    expect(await screen.findByText('Concluída', { selector: '.badge--done' })).toBeInTheDocument();
  });

  it('mantem o modal aberto e mostra o erro quando a confirmacao falha', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    vi.mocked(changeStatus).mockRejectedValue(new Error('502: comunicacao com o servidor'));
    montar();

    await screen.findByRole('region');
    await userEvent.selectOptions(screen.getByLabelText('Status'), 'DONE');

    const dialogo = await screen.findByRole('alertdialog');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Concluir' }));

    expect(await within(dialogo).findByText('502: comunicacao com o servidor')).toBeInTheDocument();
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('mantem a ordem da meta: Prioridade, tempo (subtarefa), Criada em, prazo (raiz)', () => {
    montar({ parentId: tarefa.id, dueDate: '2026-10-20', estimatedTime: 2, estimatedUnit: 'HOURS' });

    const meta = screen.getByText('Prioridade').closest('dl');
    expect(meta).not.toBeNull();
    const termos = meta ? Array.from(meta.querySelectorAll('dt')).map((node) => node.textContent) : [];
    expect(termos).toEqual(['Prioridade', 'Tempo estimado', 'Criada em']);
  });

  it('agrupa as subtarefas em bloco e abre a selecionada', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([
      filha,
      { ...tarefa, id: '3f1d3f6e-0000-4000-8000-000000000003', title: 'Desligar contadores' },
    ]);
    const { onOpen } = montar();

    const bloco = await screen.findByRole('region');
    expect(within(bloco).getByText('Subtarefas')).toBeInTheDocument();

    await userEvent.click(within(bloco).getByRole('button', { name: /Desligar contadores/ }));

    expect(onOpen).toHaveBeenCalledWith(
      expect.objectContaining({ id: '3f1d3f6e-0000-4000-8000-000000000003' }),
    );
  });

  it('nao exibe mais o selo Subtarefa de X', () => {
    montar({ id: filha.id, title: 'Contratar empresa', parentId: tarefa.id });

    expect(screen.queryByRole('button', { name: 'Subtarefa de Mover casa' })).not.toBeInTheDocument();
  });

  it('lista as subtarefas no dialogo sem refazer a busca', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    vi.mocked(deleteTask).mockResolvedValue(undefined);
    const { onDeleted } = montar();

    await screen.findByRole('region');

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    const dialogo = await screen.findByRole('alertdialog');
    expect(within(dialogo).getByText('Contratar empresa')).toBeInTheDocument();
    expect(deleteTask).not.toHaveBeenCalled();

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }));

    expect(deleteTask).toHaveBeenCalledWith(tarefa.id);
    expect(onDeleted).toHaveBeenCalledOnce();
    expect(getSubtasks).toHaveBeenCalledTimes(1);
  });

  it('confirma direto a exclusao quando nao ha subtarefas', async () => {
    vi.mocked(deleteTask).mockResolvedValue(undefined);
    const { onDeleted } = montar();

    await screen.findByRole('region');

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));

    const dialogo = await screen.findByRole('alertdialog');
    expect(dialogo).not.toHaveTextContent('junto com as subtarefas');

    await userEvent.click(within(dialogo).getByRole('button', { name: 'Excluir' }));

    expect(deleteTask).toHaveBeenCalledWith(tarefa.id);
    expect(onDeleted).toHaveBeenCalledOnce();
  });

  it('cancela a exclusao sem chamar a API', async () => {
    vi.mocked(getSubtasks).mockResolvedValue([filha]);
    const { onDeleted } = montar();

    await screen.findByRole('region');

    await userEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    const dialogo = await screen.findByRole('alertdialog');
    await userEvent.click(within(dialogo).getByRole('button', { name: 'Cancelar' }));

    expect(deleteTask).not.toHaveBeenCalled();
    expect(onDeleted).not.toHaveBeenCalled();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });
});