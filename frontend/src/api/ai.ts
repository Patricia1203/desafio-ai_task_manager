import { request } from './client';
import type { Analise, Decomposicao, Melhoria, RascunhoSubtarefa } from '../types/ai';
import type { Task } from '../types/task';

export function improveTask(title: string, description: string | null): Promise<Melhoria> {
  return request<Melhoria>('/ai/tasks/improve', {
    method: 'POST',
    body: JSON.stringify({ title, description }),
  });
}

export function analyzeTask(id: string): Promise<Analise> {
  return request<Analise>(`/ai/tasks/${id}/analyze`, { method: 'POST' });
}

export function decomposeTask(id: string): Promise<Decomposicao> {
  return request<Decomposicao>(`/ai/tasks/${id}/decompose`, { method: 'POST' });
}

export function applyDecomposition(
  id: string,
  subtasks: RascunhoSubtarefa[],
): Promise<Task[]> {
  return request<Task[]>(`/ai/tasks/${id}/decompose/apply`, {
    method: 'POST',
    body: JSON.stringify({ subtasks }),
  });
}