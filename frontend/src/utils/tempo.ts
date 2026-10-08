import type { TaskTimeUnit } from '../types/task';

const SINGULARES: Record<TaskTimeUnit, string> = { HOURS: 'hora', DAYS: 'dia' };
const PLURAIS: Record<TaskTimeUnit, string> = { HOURS: 'horas', DAYS: 'dias' };

export function formatarTempoEstimado(
  valor: number | null | undefined,
  unidade: TaskTimeUnit | null | undefined,
): string {
  if (!valor || !unidade) {
    return '';
  }
  return `${valor} ${valor === 1 ? SINGULARES[unidade] : PLURAIS[unidade]}`;
}
