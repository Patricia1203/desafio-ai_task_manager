import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getSummary } from '../api/tasks';
import DashboardPage from './DashboardPage';

vi.mock('../api/tasks', () => ({
  getSummary: vi.fn(),
}));

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renderiza o titulo da pagina', () => {
    vi.mocked(getSummary).mockResolvedValue({
      total: 0,
      pendentes: 0,
      emAndamento: 0,
      concluidas: 0,
      altaPrioridade: 0,
    });
    render(<DashboardPage />);
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
  });

  it('mostra os indicadores do summary', async () => {
    vi.mocked(getSummary).mockResolvedValue({
      total: 12,
      pendentes: 5,
      emAndamento: 3,
      concluidas: 4,
      altaPrioridade: 2,
    });
    render(<DashboardPage />);

    expect(await screen.findByText('Total de tarefas')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Pendentes')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('Em andamento')).toBeInTheDocument();
    expect(screen.getByText('Concluídas')).toBeInTheDocument();
    expect(screen.getByText('Alta prioridade')).toBeInTheDocument();
  });

  it('mostra o erro quando o summary falha e permite tentar de novo', async () => {
    vi.mocked(getSummary)
      .mockRejectedValueOnce(new Error('Falhou a rede'))
      .mockResolvedValueOnce({
        total: 1,
        pendentes: 1,
        emAndamento: 0,
        concluidas: 0,
        altaPrioridade: 0,
      });
    render(<DashboardPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Falhou a rede');
    await userEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(await screen.findByText('Total de tarefas')).toBeInTheDocument();
    expect(getSummary).toHaveBeenCalledTimes(2);
  });
});
