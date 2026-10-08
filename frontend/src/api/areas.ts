import { API_BASE_URL, request } from './client';
import type { WorkArea } from '../types/area';

/**
 * Cliente de areas de trabalho (F14).
 *
 * <p>{@code createArea}/{@code updateArea} enviam {@code FormData} (multipart):
 * o {@code client.request} nao mescla {@code Content-Type} manual quando o
 * corpo e um FormData, deixando o browser montar o boundary da requisicao.
 */
export function listAreas(params: { title?: string } = {}): Promise<WorkArea[]> {
  const search = new URLSearchParams();
  if (params.title?.trim()) search.set('title', params.title.trim());
  const query = search.toString();
  return request<WorkArea[]>(`/areas${query ? `?${query}` : ''}`);
}

export function createArea(form: FormData): Promise<WorkArea> {
  return request<WorkArea>('/areas', {
    method: 'POST',
    body: form,
  });
}

export function updateArea(id: string, form: FormData): Promise<WorkArea> {
  return request<WorkArea>(`/areas/${id}`, {
    method: 'PUT',
    body: form,
  });
}

export function deleteArea(id: string): Promise<void> {
  return request<void>(`/areas/${id}`, { method: 'DELETE' });
}

/** Endereco dos bytes da foto; so monta quando a area tem imagem. */
export function areaImageUrl(id: string): string {
  return `${API_BASE_URL}/areas/${id}/image`;
}

/** Mapa id -> area para resolver o titulo nas tarefas sem consultar por item. */
export function areasPorId(areas: WorkArea[]): Map<string, WorkArea> {
  return new Map(areas.map((area) => [area.id, area]));
}