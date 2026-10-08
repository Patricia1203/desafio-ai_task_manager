import { request } from './client';
import type {
  PageResponse,
  Task,
  TaskInput,
  TaskPriority,
  TaskStatus,
  TaskSummary,
} from '../types/task';

export interface ListParams {
  status?: TaskStatus;
  priority?: TaskPriority;
  areaId?: string;
  page?: number;
  size?: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function numberOf(value: unknown): number {
  return typeof value === 'number' ? value : 0;
}

function normalizePageResponse<T>(payload: unknown): PageResponse<T> {
  if (Array.isArray(payload)) {
    return {
      content: payload as T[],
      page: 0,
      size: payload.length,
      totalItems: payload.length,
      totalPages: payload.length > 0 ? 1 : 0,
      first: true,
      last: true,
    };
  }

  const page = isRecord(payload) ? payload : {};
  const content = Array.isArray(page.content)
    ? page.content
    : Array.isArray(page.items)
      ? page.items
      : [];

  return {
    content: content as T[],
    page: numberOf(page.page),
    size: numberOf(page.size) || content.length,
    totalItems: numberOf(page.totalItems) || numberOf(page.totalElements) || content.length,
    totalPages: numberOf(page.totalPages) || (content.length > 0 ? 1 : 0),
    first: page.first !== false,
    last: page.last !== false,
  };
}

function normalizeSummary(payload: unknown): TaskSummary {
  const summary = isRecord(payload) ? payload : {};
  return {
    total: numberOf(summary.total),
    pending: numberOf(summary.pending) || numberOf(summary.pendentes),
    inProgress: numberOf(summary.inProgress) || numberOf(summary.emAndamento),
    done: numberOf(summary.done) || numberOf(summary.concluidas),
    highPriority: numberOf(summary.highPriority) || numberOf(summary.altaPrioridade),
  };
}

function queryString(params: ListParams): string {
  const search = new URLSearchParams();
  if (params.status) search.set('status', params.status);
  if (params.priority) search.set('priority', params.priority);
  if (params.areaId) search.set('areaId', params.areaId);
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  const query = search.toString();
  return query ? `?${query}` : '';
}

export function listTasks(params: ListParams = {}): Promise<PageResponse<Task>> {
  return request<unknown>(`/tasks${queryString(params)}`).then(normalizePageResponse<Task>);
}

export function getSummary(): Promise<TaskSummary> {
  return request<unknown>('/tasks/summary').then(normalizeSummary);
}

export function getTask(id: string): Promise<Task> {
  return request<Task>(`/tasks/${id}`);
}

export function getSubtasks(id: string): Promise<Task[]> {
  return request<Task[]>(`/tasks/${id}/subtasks`);
}

export function createTask(input: TaskInput): Promise<Task> {
  return request<Task>('/tasks', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function updateTask(id: string, input: TaskInput): Promise<Task> {
  return request<Task>(`/tasks/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function changeStatus(
  id: string,
  status: TaskStatus,
  completeSubtasks?: boolean,
): Promise<Task> {
  const body: Record<string, unknown> = { status };
  if (completeSubtasks !== undefined) {
    body.completeSubtasks = completeSubtasks;
  }
  return request<Task>(`/tasks/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function deleteTask(id: string): Promise<void> {
  return request<void>(`/tasks/${id}`, { method: 'DELETE' });
}
