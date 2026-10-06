import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import TaskList from '../components/task/TaskList';

function tarefa(id: string, titulo: string): Task {
  return {
    id,
    titulo,
    descricao: null,
    status: 'A_FAZER',
    prioridade: 'MEDIA',
    prazo: null,
    idTarefaPai: null,
    criadoEm: '2026-10-01T10:00:00Z',
    atualizadoEm: '2026-10-01T10:00:00Z',
  };
}

const tarefas = [
  tarefa('3f1d3f6e-0000-4000-8000-000000000001', 'Pagar boleto'),
  tarefa('3f1d3f6e-0000-4000-8000-000000000002', 'Revisar contrato'),
];

function renderizar(tarefasParaRender: Task[] = tarefas, sobrescritas = {}) {
  const props = {
    tasks: tarefasParaRender,
    loading: false,
    error: null,
    onRetry: vi.fn(),
    onSelect: vi.fn(),
    ...sobrescritas,
  };
  render(<TaskList {...props} />);
  return props;
}

describe('TaskList', () => {
  it('renderiza o titulo de cada tarefa', () => {
    renderizar();

    expect(screen.getByRole('button', { name: /Pagar boleto/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Revisar contrato/ })).toBeInTheDocument();
  });

  it('filtra as tarefas pelo titulo', async () => {
    renderizar();

    await userEvent.type(screen.getByLabelText('Filtrar por título'), 'contrato');

    expect(screen.getByRole('button', { name: /Revisar contrato/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Pagar boleto/ })).not.toBeInTheDocument();
  });

  it('mostra mensagem de vazio quando nao ha tarefas', () => {
    renderizar([]);

    expect(screen.getByText('Nenhuma tarefa cadastrada. Crie a primeira!')).toBeInTheDocument();
  });

  it('mostra o estado de carregamento', () => {
    renderizar([], { loading: true });

    expect(screen.getByRole('status')).toHaveTextContent('Carregando...');
  });

  it('mostra o erro e oferece nova tentativa', async () => {
    const { onRetry } = renderizar([], { error: 'Falhou a rede' });

    expect(screen.getByRole('alert')).toHaveTextContent('Falhou a rede');
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it('abre o detalhe da tarefa selecionada', async () => {
    const { onSelect } = renderizar();

    await userEvent.click(screen.getByRole('button', { name: /Pagar boleto/ }));

    expect(onSelect).toHaveBeenCalledWith(tarefas[0]);
  });
});
