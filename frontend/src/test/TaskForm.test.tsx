import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Task } from '../types/task';
import TaskForm from '../components/task/TaskForm';

const tarefa: Task = {
  id: '3f1d3f6e-0000-4000-8000-000000000001',
  title: 'Revisar contrato',
  description: 'Ler o anexo II',
  status: 'TODO',
  priority: 'HIGH',
  dueDate: '2026-10-20',
  parentId: null,
  createdAt: '2026-10-01T10:00:00Z',
  updatedAt: '2026-10-01T10:00:00Z',
};

describe('TaskForm', () => {
  it('recusa submit sem titulo e mostra o erro no campo', async () => {
    const onSubmit = vi.fn();
    render(<TaskForm submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('Informe o título da tarefa.');
  });

  it('envia os campos preenchidos com titulo aparado', async () => {
    const onSubmit = vi.fn();
    render(<TaskForm submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText(/Título/), '  Pagar boleto  ');
    await userEvent.selectOptions(screen.getByLabelText('Prioridade'), 'LOW');
    await userEvent.type(screen.getByLabelText('Prazo'), '2026-11-01');
    await userEvent.click(screen.getByRole('button', { name: 'Salvar' }));

    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Pagar boleto',
      description: null,
      priority: 'LOW',
      dueDate: '2026-11-01',
    });
  });

  it('preenche os campos ao editar uma tarefa existente', () => {
    const onSubmit = vi.fn();
    render(
      <TaskForm task={tarefa} submitting={false} onSubmit={onSubmit} onCancel={vi.fn()} />,
    );

    expect(screen.getByLabelText(/Título/)).toHaveValue('Revisar contrato');
    expect(screen.getByLabelText('Prioridade')).toHaveValue('HIGH');
    expect(screen.getByLabelText('Prazo')).toHaveValue('2026-10-20');
  });

  it('desabilita os botoes enquanto o submit esta em andamento', () => {
    render(
      <TaskForm submitting={true} onSubmit={vi.fn()} onCancel={vi.fn()} />,
    );

    expect(screen.getByRole('button', { name: 'Salvando...' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled();
  });
});