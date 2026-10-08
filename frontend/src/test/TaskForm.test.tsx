import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import TaskForm from '../components/task/TaskForm';

const areaId = '3f1d3f6e-0000-4000-8000-000000000033';

const tarefa: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Revisar contrato',
  description: 'Ler o anexo II',
  status: 'TODO',
  priority: 'HIGH',
  dueDate: '2026-10-20',
  parentId: null,
  areaId,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
  subtaskCount: 0,
  estimatedTime: 2,
  estimatedUnit: 'HOURS',
};

const subtarefa: Task = {
  ...tarefa,
  id: '3f1d3f6e-0000-4000-8000-0000000000aa',
  parentId: '3f1d3f6e-0000-4000-8000-0000000000bb',
  dueDate: null,
  estimatedTime: 2,
  estimatedUnit: 'HOURS',
};

describe('TaskForm', () => {
  const areas = [
    { id: areaId, title: 'Moradia', imageType: null },
    { id: '3f1d3f6e-0000-4000-8000-000000000055', title: 'Finanças', imageType: null },
  ];

  it('recusa submit sem titulo e mostra o erro no campo', async () => {
    const onSubmit = vi.fn();
    render(<TaskForm areas={areas} submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('Informe o título da tarefa.');
  });

  it('envia os campos preenchidos com titulo aparado', async () => {
    const onSubmit = vi.fn();
    render(<TaskForm areas={areas} submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText(/Título/), '  Pagar boleto  ');
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'LOW');
    await userEvent.selectOptions(screen.getByLabelText('Quadro'), 'Finanças');
    await userEvent.type(screen.getByLabelText('Prazo'), '2026-11-01');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Pagar boleto',
      description: null,
      priority: 'LOW',
      dueDate: '2026-11-01',
      estimatedTime: null,
      estimatedUnit: null,
      areaId: '3f1d3f6e-0000-4000-8000-000000000055',
    });
  });

  it('envia Sem quadro quando nenhum quadro e escolhido', async () => {
    const onSubmit = vi.fn();
    render(<TaskForm areas={areas} submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText(/Título/), 'Pagar boleto');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ areaId: null }));
  });

  it('mostra o quadro da tarefa como selecionado ao editar', () => {
    render(<TaskForm task={tarefa} areas={areas} submitting={false} onSubmit={vi.fn()} onCancel={vi.fn()} />);

    expect(screen.getByLabelText('Quadro')).toHaveValue(areaId);
  });

  it('envia o tempo estimado com a unidade escolhida', async () => {
    const onSubmit = vi.fn();
    render(
      <TaskForm
        task={{ ...subtarefa, title: '', estimatedTime: null, estimatedUnit: null }}
        areas={areas}
        submitting={false}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />,
    );

    await userEvent.type(screen.getByLabelText(/Título/), 'Criar relatorio');
    await userEvent.type(screen.getByLabelText('Tempo estimado'), '4');
    await userEvent.selectOptions(screen.getByLabelText('Unidade'), 'DAYS');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Criar relatorio',
        estimatedTime: 4,
        estimatedUnit: 'DAYS',
        dueDate: null,
      }),
    );
  });

  it('recusa tempo estimado fora do intervalo', async () => {
    const onSubmit = vi.fn();
    render(
      <TaskForm
        task={{ ...subtarefa, title: '', estimatedTime: null, estimatedUnit: null }}
        areas={areas}
        submitting={false}
        onSubmit={onSubmit}
        onCancel={vi.fn()}
      />,
    );

    await userEvent.type(screen.getByLabelText(/Título/), 'Criar relatorio');
    await userEvent.type(screen.getByLabelText('Tempo estimado'), '250');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'O tempo estimado deve ser maior que zero e até 200.',
    );
  });

  it('a raiz edita somente o prazo e nao mostra tempo', () => {
    render(
      <TaskForm task={tarefa} areas={areas} submitting={false} onSubmit={vi.fn()} onCancel={vi.fn()} />,
    );

    expect(screen.getByLabelText(/Título/)).toHaveValue('Revisar contrato');
    expect(screen.getByLabelText('Prioridade')).toHaveValue('HIGH');
    expect(screen.getByLabelText('Prazo')).toHaveValue('2026-10-20');
    expect(screen.queryByLabelText('Tempo estimado')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Unidade')).not.toBeInTheDocument();
  });

  it('a subtarefa edita somente o tempo e nao mostra prazo', () => {
    render(
      <TaskForm
        task={subtarefa}
        areas={areas}
        submitting={false}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('Tempo estimado')).toHaveValue(2);
    expect(screen.getByLabelText('Unidade')).toHaveValue('HOURS');
    expect(screen.queryByLabelText('Prazo')).not.toBeInTheDocument();
  });

  it('desabilita os botoes enquanto o submit esta em andamento', () => {
    render(<TaskForm areas={areas} submitting={true} onSubmit={vi.fn()} onCancel={vi.fn()} />);

    expect(screen.getByRole('button', { name: 'Salvando...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });
});