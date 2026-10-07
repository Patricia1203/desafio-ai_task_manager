import type { Melhoria } from '../../types/ai';

interface ImproveResultProps {
  melhoria: Melhoria;
  aplicando: boolean;
  onAplicar: () => void;
  onDescartar: () => void;
}

export default function ImproveResult({
  melhoria,
  aplicando,
  onAplicar,
  onDescartar,
}: ImproveResultProps) {
  return (
    <section className="ai-resultado" aria-labelledby="ai-melhoria-titulo">
      <h5 id="ai-melhoria-titulo">Sugestão de melhoria</h5>
      <p className="ai-resultado__titulo">{melhoria.title}</p>
      <p>{melhoria.description}</p>
      <div className="ai-resultado__acoes">
        <button type="button" onClick={onAplicar} disabled={aplicando}>
          {aplicando ? 'Aplicando...' : 'Aplicar à tarefa'}
        </button>
        <button type="button" onClick={onDescartar} disabled={aplicando}>
          Descartar
        </button>
      </div>
    </section>
  );
}
