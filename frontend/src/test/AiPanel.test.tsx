import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import { ApiError } from '../api/client';
import { analyzeTask, applyAnalysis, applyDecomposition, decomposeTask, improveTask } from '../api/ai';
import { updateTask } from '../api/tasks';
import AiPanel from '../components/task/AiPanel';

vi.mock('../api/ai', () => ({
  improveTask: vi.fn(),
  analyzeTask: vi.fn(),
  applyAnalysis: vi.fn(),
  decomposeTask: vi.fn(),
  applyDecomposition: vi.fn(),
}));

vi.mock('../api/tasks', () => ({
  updateTask: vi.fn(),
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

function montar() {
  const onChanged = vi.fn();
  const onSubtasksCreated = vi.fn();
  render(<AiPanel task={tarefa} onChanged={onChanged} onSubtasksCreated={onSubtasksCreated} />);
  return { onChanged, onSubtasksCreated };
}

describe('AiPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('melhora a tarefa, exibe a sugestão e aplica quando o usuário confirma', async () => {
    vi.mocked(improveTask).mockResolvedValue({
      title: 'Mover casa com calma',
      description: 'Contrate uma empresa com uma semana de antecedencia',
    });
    const atualizada = {
      ...tarefa,
      title: 'Mover casa com calma',
      description: 'Contrate uma empresa com uma semana de antecedencia',
    };
    vi.mocked(updateTask).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Melhorar' }));

    expect(improveTask).toHaveBeenCalledWith('Mover casa', null);
    expect(await screen.findByText('Sugestão de melhoria')).toBeInTheDocument();
    expect(screen.getByText('Mover casa com calma')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Aplicar à tarefa' }));

    expect(updateTask).toHaveBeenCalledWith(tarefa.id, {
      title: 'Mover casa com calma',
      description: 'Contrate uma empresa com uma semana de antecedencia',
      priority: 'MEDIUM',
      dueDate: null,
      estimatedTime: null,
      estimatedUnit: null,
    });
    await vi.waitFor(() => expect(onChanged).toHaveBeenCalledWith(atualizada));
    expect(screen.queryByText('Sugestão de melhoria')).not.toBeInTheDocument();
  });

  it('analisa a tarefa e exibe prioridade, complexidade, horas e justificativa', async () => {
    vi.mocked(analyzeTask).mockResolvedValue({
      priority: 'HIGH',
      complexity: 'MEDIUM',
      estimatedHours: 12.5,
      reason: 'Titulo generico e prazo curto',
    });
    montar();

    await userEvent.click(screen.getByRole('button', { name: 'Analisar' }));

    expect(analyzeTask).toHaveBeenCalledWith(tarefa.id);
    const secao = (await screen.findByText('Análise da tarefa')).closest('section');
    expect(secao).toHaveTextContent('Alta');
    expect(secao).toHaveTextContent('Média');
    expect(secao).toHaveTextContent('12,5 h');
    expect(secao).toHaveTextContent('Titulo generico e prazo curto');
  });

  it('aplica a sugestao da analise na tarefa e atualiza a tela', async () => {
    vi.mocked(analyzeTask).mockResolvedValue({
      priority: 'HIGH',
      complexity: 'HIGH',
      estimatedHours: 4,
      reason: 'Priorize a mudanca',
    });
    const atualizada = { ...tarefa, priority: 'HIGH' as const, estimatedTime: 4, estimatedUnit: 'HOURS' as const };
    vi.mocked(applyAnalysis).mockResolvedValue(atualizada);
    const { onChanged } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Analisar' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Aplicar Sugestão' }));

    expect(applyAnalysis).toHaveBeenCalledWith(tarefa.id, { priority: 'HIGH', estimatedHours: 4 });
    await vi.waitFor(() => expect(onChanged).toHaveBeenCalledWith(atualizada));
    expect(screen.queryByText('Análise da tarefa')).not.toBeInTheDocument();
  });

  it('divide a tarefa, deixa o usuário remover itens e envia só as selecionadas', async () => {
    vi.mocked(decomposeTask).mockResolvedValue({
      subtasks: [
        { title: 'Contratar empresa', description: 'Ligar para 3 empresas', estimatedHours: 2 },
        { title: 'Desligar contadores', description: null, estimatedHours: null },
      ],
    });
    vi.mocked(applyDecomposition).mockResolvedValue([]);
    const { onSubtasksCreated } = montar();

    await userEvent.click(screen.getByRole('button', { name: 'Dividir' }));

    expect(decomposeTask).toHaveBeenCalledWith(tarefa.id);
    const primeira = await screen.findByRole('checkbox', { name: /Contratar empresa/ });
    const segunda = screen.getByRole('checkbox', { name: /Desligar contadores/ });
    expect(primeira).toBeChecked();
    expect(segunda).toBeChecked();

    await userEvent.click(screen.getByRole('button', { name: 'Remover Desligar contadores' }));
    expect(
      screen.queryByRole('checkbox', { name: /Desligar contadores/ }),
    ).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar como tarefas' }));

    expect(applyDecomposition).toHaveBeenCalledWith(tarefa.id, [
      { title: 'Contratar empresa', description: 'Ligar para 3 empresas', estimatedHours: 2 },
    ]);
    await vi.waitFor(() => expect(onSubtasksCreated).toHaveBeenCalledOnce());
    expect(screen.queryByText('Subtarefas sugeridas')).not.toBeInTheDocument();
  });

  it('desmarca uma sugestão para não enviá-la', async () => {
    vi.mocked(decomposeTask).mockResolvedValue({
      subtasks: [
        { title: 'Contratar empresa', description: null, estimatedHours: null },
        { title: 'Desligar contadores', description: null, estimatedHours: null },
      ],
    });
    vi.mocked(applyDecomposition).mockResolvedValue([]);
    montar();

    await userEvent.click(screen.getByRole('button', { name: 'Dividir' }));
    const segunda = await screen.findByRole('checkbox', { name: /Desligar contadores/ });
    await userEvent.click(segunda);
    expect(segunda).not.toBeChecked();

    await userEvent.click(screen.getByRole('button', { name: 'Adicionar como tarefas' }));

    expect(applyDecomposition).toHaveBeenCalledWith(tarefa.id, [
      { title: 'Contratar empresa', description: null, estimatedHours: null },
    ]);
  });

  it('mostra mensagem amigável quando a IA devolve resposta inválida', async () => {
    vi.mocked(improveTask).mockRejectedValue(
      new ApiError(502, {
        type: 'https://desafio.ai-task-manager/errors/resposta-llm-invalida',
        title: 'Resposta invalida da IA',
        status: 502,
        detail: 'O servico de IA devolveu uma resposta fora do contrato',
        code: 'LLM_INVALID_RESPONSE',
      }),
    );
    montar();

    await userEvent.click(screen.getByRole('button', { name: 'Melhorar' }));

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent('A IA não conseguiu montar uma resposta válida desta vez');
  });

  it('desabilita os botões e mostra status enquanto a chamada está em voo', async () => {
    vi.mocked(analyzeTask).mockReturnValue(new Promise(() => {}));
    montar();

    await userEvent.click(screen.getByRole('button', { name: 'Analisar' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Consultando a IA...');
    expect(screen.getByRole('button', { name: 'Analisando...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Melhorar' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Dividir' })).toBeDisabled();
  });
});
