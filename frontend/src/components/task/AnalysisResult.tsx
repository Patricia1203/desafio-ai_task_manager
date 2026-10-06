import type { Analise } from '../../types/ai';
import { COMPLEXIDADE_LABELS } from '../../types/ai';
import { PRIORITY_LABELS } from '../../types/task';

interface AnalysisResultProps {
  analise: Analise;
  onFechar: () => void;
}

export default function AnalysisResult({ analise, onFechar }: AnalysisResultProps) {
  return (
    <section className="ai-resultado" aria-labelledby="ai-analise-titulo">
      <h5 id="ai-analise-titulo">Análise da tarefa</h5>
      <dl className="ai-resultado__meta">
        <div>
          <dt>Prioridade sugerida</dt>
          <dd>{PRIORITY_LABELS[analise.prioridade]}</dd>
        </div>
        <div>
          <dt>Complexidade</dt>
          <dd>{COMPLEXIDADE_LABELS[analise.complexidade]}</dd>
        </div>
        <div>
          <dt>Horas estimadas</dt>
          <dd>
            {analise.horasEstimadas === null
              ? 'Não estimadas'
              : `${analise.horasEstimadas.toLocaleString('pt-BR')} h`}
          </dd>
        </div>
        <div>
          <dt>Justificativa</dt>
          <dd>{analise.justificativa}</dd>
        </div>
      </dl>
      <div className="ai-resultado__acoes">
        <button type="button" onClick={onFechar}>
          Fechar
        </button>
      </div>
    </section>
  );
}
