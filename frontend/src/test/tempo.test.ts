import { describe, expect, it } from 'vitest';
import { formatarTempoEstimado } from '../utils/tempo';

describe('formatarTempoEstimado', () => {
  it('formata horas no singular e no plural', () => {
    expect(formatarTempoEstimado(1, 'HOURS')).toBe('1 hora');
    expect(formatarTempoEstimado(2, 'HOURS')).toBe('2 horas');
    expect(formatarTempoEstimado(2.5, 'HOURS')).toBe('2.5 horas');
  });

  it('formata dias no singular e no plural', () => {
    expect(formatarTempoEstimado(1, 'DAYS')).toBe('1 dia');
    expect(formatarTempoEstimado(5, 'DAYS')).toBe('5 dias');
  });

  it('retorna vazio quando falta valor ou unidade', () => {
    expect(formatarTempoEstimado(null, 'HOURS')).toBe('');
    expect(formatarTempoEstimado(0, 'HOURS')).toBe('');
    expect(formatarTempoEstimado(2, null)).toBe('');
    expect(formatarTempoEstimado(null, null)).toBe('');
  });
});