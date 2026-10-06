import type { SugestaoSubtarefa } from '../../types/ai';

interface DecompositionResultProps {
  sugestoes: SugestaoSubtarefa[];
  adicionando: boolean;
  onAlternar: (indice: number) => void;
  onRemover: (indice: number) => void;
  onAdicionar: () => void;
  onDescartar: () => void;
}

export default function DecompositionResult({
  sugestoes,
  adicionando,
  onAlternar,
  onRemover,
  onAdicionar,
  onDescartar,
}: DecompositionResultProps) {
  const selecionadas = sugestoes.filter((sugestao) => sugestao.selecionada).length;

  return (
    <section className="ai-resultado" aria-labelledby="ai-decomposicao-titulo">
      <h5 id="ai-decomposicao-titulo">Subtarefas sugeridas</h5>
      <ul className="ai-resultado__lista">
        {sugestoes.map((sugestao, indice) => (
          <li key={`${sugestao.titulo}-${indice}`}>
            <label className="ai-resultado__item">
              <input
                type="checkbox"
                checked={sugestao.selecionada}
                onChange={() => onAlternar(indice)}
              />
              <span>
                <strong>{sugestao.titulo}</strong>
                {sugestao.descricao && <> — {sugestao.descricao}</>}
                {sugestao.horasEstimadas !== null && (
                  <> ({sugestao.horasEstimadas.toLocaleString('pt-BR')} h)</>
                )}
              </span>
            </label>
            <button
              type="button"
              onClick={() => onRemover(indice)}
              disabled={adicionando}
              aria-label={`Remover ${sugestao.titulo}`}
            >
              Remover
            </button>
          </li>
        ))}
      </ul>
      <div className="ai-resultado__acoes">
        <button type="button" onClick={onAdicionar} disabled={adicionando || selecionadas === 0}>
          {adicionando ? 'Adicionando...' : 'Adicionar como tarefas'}
        </button>
        <button type="button" onClick={onDescartar} disabled={adicionando}>
          Descartar
        </button>
      </div>
      <p className="ai-resultado__contagem" role="status">
        {selecionadas} de {sugestoes.length} selecionadas
      </p>
    </section>
  );
}
