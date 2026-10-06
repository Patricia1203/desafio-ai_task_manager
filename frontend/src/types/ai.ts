import type { TaskPriority } from './task';

export interface Melhoria {
  titulo: string;
  descricao: string;
}

export type Complexidade = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Analise {
  prioridade: TaskPriority;
  complexidade: Complexidade;
  horasEstimadas: number | null;
  justificativa: string;
}

export interface SubtarefaSugerida {
  titulo: string;
  descricao: string | null;
  horasEstimadas: number | null;
}

export interface Decomposicao {
  subtarefas: SubtarefaSugerida[];
}

export interface RascunhoSubtarefa {
  titulo: string;
  descricao: string | null;
  horasEstimadas: number | null;
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
