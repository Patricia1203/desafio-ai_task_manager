import type { TaskPriority } from './task';

export interface Melhoria {
  title: string;
  description: string;
}

export type Complexidade = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Analise {
  priority: TaskPriority;
  complexity: Complexidade;
  estimatedHours: number | null;
  reason: string;
}

export interface SubtarefaSugerida {
  title: string;
  description: string | null;
  estimatedHours: number | null;
}

export interface Decomposicao {
  subtasks: SubtarefaSugerida[];
}

export interface RascunhoSubtarefa {
  title: string;
  description: string | null;
  estimatedHours: number | null;
}

export const COMPLEXIDADE_LABELS: Record<Complexidade, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
};

/** Sugestão da decomposição com o estado de seleção da UI (não vem da API). */
export interface SugestaoSubtarefa extends SubtarefaSugerida {
  selecionada: boolean;
}