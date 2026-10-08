import { describe, expect, it, vi } from 'vitest';
import { ApiError, request } from '../api/client';

describe('client', () => {
  it('formata erros de validacao no formato FieldErrorItem do backend', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            title: 'Requisicao invalida',
            errors: [{ field: 'title', reason: 'nao pode estar em branco' }],
          }),
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    await expect(request('/tasks', { method: 'POST' })).rejects.toMatchObject({
      message: 'title: nao pode estar em branco',
    } as ApiError);
  });
});
