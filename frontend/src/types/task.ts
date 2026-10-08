export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export type TaskTimeUnit = 'HOURS' | 'DAYS';

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  parentId: string | null;
  createdAt: string;
  updatedAt: string;
  subtaskCount: number;
  estimatedTime: number | null;
  estimatedUnit: TaskTimeUnit | null;
}

export interface TaskInput {
  title: string;
  description: string | null;
  priority: TaskPriority;
  dueDate: string | null;
  estimatedTime: number | null;
  estimatedUnit: TaskTimeUnit | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TaskSummary {
  total: number;
  pending: number;
  inProgress: number;
  done: number;
  highPriority: number;
}

export const STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluída',
};

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
};

export const STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export const PRIORITY_OPTIONS: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

export const TIME_UNIT_LABELS: Record<TaskTimeUnit, string> = {
  HOURS: 'Horas',
  DAYS: 'Dias',
};

export const TIME_UNIT_OPTIONS: TaskTimeUnit[] = ['HOURS', 'DAYS'];
