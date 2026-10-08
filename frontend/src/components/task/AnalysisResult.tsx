import type { Analise } from '../../types/ai';
import { COMPLEXIDADE_LABELS } from '../../types/ai';
import { PRIORITY_LABELS } from '../../types/task';

interface AnalysisResultProps {
  analise: Analise;
  aplicando: boolean;
  onAplicar: () => void;
  onFechar: () => void;
}

export default function AnalysisResult({ analise, aplicando, onAplicar, onFechar }: AnalysisResultProps) {
  return (
    <section className="ai-resultado" aria-labelledby="ai-analise-titulo">
      <h5 id="ai-analise-titulo">Análise da tarefa</h5>
      <dl className="ai-resultado__meta">
        <div>
          <dt>Prioridade sugerida</dt>
          <dd>{PRIORITY_LABELS[analise.priority]}</dd>
        </div>
        <div>
          <dt>Complexidade</dt>
          <dd>{COMPLEXIDADE_LABELS[analise.complexity]}</dd>
        </div>
        <div>
          <dt>Horas estimadas</dt>
          <dd>
            {analise.estimatedHours === null
              ? 'Não estimadas'
              : `${analise.estimatedHours.toLocaleString('pt-BR')} h`}
          </dd>
        </div>
        <div>
          <dt>Justificativa</dt>
          <dd>{analise.reason}</dd>
        </div>
      </dl>
      <div className="ai-resultado__acoes">
        <button type="button" onClick={onAplicar} disabled={aplicando}>
          {aplicando ? 'Aplicando...' : 'Aplicar Sugestão'}
        </button>
        <button type="button" onClick={onFechar} disabled={aplicando}>
          Fechar
        </button>
      </div>
    </section>
  );
}
