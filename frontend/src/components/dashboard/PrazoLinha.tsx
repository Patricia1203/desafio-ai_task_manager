import { useState } from 'react';
import type { Task } from '../../types/task';
import { getSubtasks } from '../../api/tasks';
import { formatarDataBR } from '../../utils/date';
import { formatarTempoEstimado } from '../../utils/tempo';

interface PrazoLinhaProps {
  tarefa: Task;
  hoje: string;
}

export default function PrazoLinha({ tarefa, hoje }: PrazoLinhaProps) {
  const [aberta, setAberta] = useState(false);
  const [carregando, setCarregando] = useState(false);
  const [filhos, setFilhos] = useState<Task[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  function alternar() {
    if (aberta) {
      setAberta(false);
      return;
    }
    setAberta(true);
    if (filhos !== null) {
      return;
    }
    setCarregando(true);
    setErro(null);
    getSubtasks(tarefa.id)
      .then((lista) => {
        setFilhos(lista);
        setCarregando(false);
      })
      .catch((caught) => {
        setErro(caught instanceof Error ? caught.message : 'Erro inesperado.');
        setCarregando(false);
      });
  }

  const atrasada = tarefa.dueDate !== null && tarefa.dueDate < hoje;
  const tempo = formatarTempoEstimado(tarefa.estimatedTime, tarefa.estimatedUnit);

  return (
    <li className="dashboard__prazo">
      <div className="dashboard__prazo-linha">
        {tarefa.subtaskCount > 0 && (
          <button
            type="button"
            className="dashboard__prazo-toggle"
            aria-expanded={aberta}
            aria-label={`${aberta ? 'Recolher' : 'Ver'} subtarefas de ${tarefa.title}`}
            onClick={alternar}
          >
            {aberta ? '▾' : '▸'}
          </button>
        )}
        <strong>{tarefa.title}</strong>
        {tempo && (
          <span className="dashboard__prazo-tempo">{tempo}</span>
        )}
        <span className={`dashboard__data${atrasada ? ' dashboard__data--atrasada' : ''}`}>
          {atrasada ? 'Atrasada · ' : ''}
          {formatarDataBR(tarefa.dueDate)}
        </span>
      </div>
      {aberta && (
        <div className="dashboard__prazo-filhos">
          {carregando && <p className="async-state__empty">Carregando subtarefas...</p>}
          {erro && (
            <p className="async-state__empty" role="alert">
              {erro}
            </p>
          )}
          {filhos !== null && filhos.length === 0 && (
            <p className="async-state__empty">Sem subtarefas.</p>
          )}
          {filhos !== null && filhos.length > 0 && (
            <ul className="dashboard__prazos dashboard__prazos--nivel">
              {filhos.map((filho) => (
                <PrazoLinha key={filho.id} tarefa={filho} hoje={hoje} />
              ))}
            </ul>
          )}
        </div>
      )}
    </li>
  );
}