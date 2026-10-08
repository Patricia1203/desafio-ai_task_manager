import { describe, expect, it, vi } from 'vitest';
import { afterEach } from 'vitest';
import { getSummary, listTasks } from '../api/tasks';

function fetchReturning(payload: unknown): void {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response(JSON.stringify(payload), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('tasks (normalização de envelope)', () => {
  it('listTasks aceita array simples e devolve PageResponse normalizado', async () => {
    fetchReturning([{ id: '1', title: 'Primeira' }]);

    const page = await listTasks();

    expect(page.content).toHaveLength(1);
    expect(page.page).toBe(0);
    expect(page.totalItems).toBe(1);
    expect(page.totalPages).toBe(1);
    expect(page.first).toBe(true);
    expect(page.last).toBe(true);
  });

  it('listTasks preserva envelope padrão do backend', async () => {
    const payload = {
      content: [{ id: '2', title: 'Segunda' }],
      page: 1,
      size: 20,
      totalItems: 41,
      totalPages: 3,
      first: false,
      last: false,
    };
    fetchReturning(payload);

    const page = await listTasks({ page: 1 });

    expect(page.content).toHaveLength(1);
    expect(page.page).toBe(1);
    expect(page.size).toBe(20);
    expect(page.totalItems).toBe(41);
    expect(page.totalPages).toBe(3);
    expect(page.first).toBe(false);
  });

  it('getSummary aceita campos legados em português e normaliza', async () => {
    fetchReturning({ total: 5, pendentes: 3, emAndamento: 1, concluidas: 1, altaPrioridade: 2 });

    const summary = await getSummary();

    expect(summary).toEqual({ total: 5, pending: 3, inProgress: 1, done: 1, highPriority: 2 });
  });
});