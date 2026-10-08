import { afterEach, describe, expect, it, vi } from 'vitest';
import { areaImageUrl, areasPorId, createArea, listAreas, updateArea } from '../api/areas';
function imagem(payload: unknown) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(payload), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }),
  );
  vi.stubGlobal('fetch', fetchMock);
  return {
    fetchMock,
    requisicao: () => fetchMock.mock.calls[0]?.[1] as RequestInit | undefined,
  };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('areas (client multipart)', () => {
  it('listAreas devolve a lista de areas', async () => {
    const { requisicao } = imagem([{ id: '1', title: 'Moradia', imageType: null }]);

    const areas = await listAreas();

    expect(areas).toHaveLength(1);
    expect(requisicao()?.method ?? 'GET').toBe('GET');
  });

  it('createArea envia o FormData sem Content-Type de JSON', async () => {
    const form = new FormData();
    form.set('title', 'Moradia');
    const { requisicao } = imagem({ id: '1', title: 'Moradia', imageType: null });

    await createArea(form);

    const init = requisicao();
    const cabecalhos = init?.headers as Record<string, string> | undefined;
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(form);
    expect(cabecalhos?.['Content-Type']).toBeUndefined();
  });

  it('updateArea envia o FormData no PUT', async () => {
    const form = new FormData();
    form.set('title', 'Moradia');
    const { requisicao } = imagem({ id: '1', title: 'Moradia', imageType: null });

    await updateArea('1', form);

    expect(requisicao()?.method).toBe('PUT');
    expect(requisicao()?.body).toBe(form);
  });

  it('areaImageUrl aponta para o recurso de bytes da foto sem repetir /api', () => {
    const url = areaImageUrl('1');
    expect(url).toContain('/areas/1/image');
    expect(url).not.toContain('/api/api');
  });

  it('areasPorId monta mapa id -> area', () => {
    const mapa = areasPorId([{ id: '1', title: 'Moradia', imageType: null }]);
    expect(mapa.get('1')?.title).toBe('Moradia');
    expect(mapa.size).toBe(1);
  });
});