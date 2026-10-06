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
  page?: number;
  size?: number;
}

function queryString(params: ListParams): string {
  const search = new URLSearchParams();
  if (params.status) search.set('status', params.status);
  if (params.priority) search.set('priority', params.priority);
  if (params.page !== undefined) search.set('page', String(params.page));
  if (params.size !== undefined) search.set('size', String(params.size));
  const query = search.toString();
  return query ? `?${query}` : '';
}

export function listTasks(params: ListParams = {}): Promise<PageResponse<Task>> {
  return request<PageResponse<Task>>(`/tasks${queryString(params)}`);
}

export function getSummary(): Promise<TaskSummary> {
  return request<TaskSummary>('/tasks/summary');
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

export function changeStatus(id: string, status: TaskStatus): Promise<Task> {
  return request<Task>(`/tasks/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export function deleteTask(id: string): Promise<void> {
  return request<void>(`/tasks/${id}`, { method: 'DELETE' });
}
