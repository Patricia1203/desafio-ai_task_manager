export type TaskStatus = 'A_FAZER' | 'EM_ANDAMENTO' | 'CONCLUIDA';

export type TaskPriority = 'BAIXA' | 'MEDIA' | 'ALTA';

export interface Task {
  id: string;
  titulo: string;
  descricao: string | null;
  status: TaskStatus;
  prioridade: TaskPriority;
  prazo: string | null;
  idTarefaPai: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface TaskInput {
  titulo: string;
  descricao: string | null;
  prioridade: TaskPriority;
  prazo: string | null;
}

export interface PageResponse<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export interface TaskSummary {
  total: number;
  pendentes: number;
  emAndamento: number;
  concluidas: number;
  altaPrioridade: number;
}

export const STATUS_LABELS: Record<TaskStatus, string> = {
  A_FAZER: 'A fazer',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
};

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

export const STATUS_OPTIONS: TaskStatus[] = ['A_FAZER', 'EM_ANDAMENTO', 'CONCLUIDA'];

export const PRIORITY_OPTIONS: TaskPriority[] = ['BAIXA', 'MEDIA', 'ALTA'];
