import { useState } from 'react';
import type { Task } from '../../types/task';
import { PRIORITY_LABELS, STATUS_LABELS } from '../../types/task';

interface TaskScheduleProps {
  tarefas: Task[];
}

const DIAS_SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];
const MESES = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

function chave(data: Date): string {
  const mm = String(data.getMonth() + 1).padStart(2, '0');
  const dd = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mm}-${dd}`;
}

function dataLocalISO(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return new Date(ano, mes - 1, dia);
}

function formatarData(chaveIso: string): string {
  const [ano, mes, dia] = chaveIso.split('-').map(Number);
  return `${String(dia).padStart(2, '0')}/${String(mes).padStart(2, '0')}/${ano}`;
}

export default function TaskSchedule({ tarefas }: TaskScheduleProps) {
  const [diaSelecionado, setDiaSelecionado] = useState<string | null>(null);
  const [hoje] = useState(() => {
    const dia = new Date();
    dia.setHours(0, 0, 0, 0);
    return dia;
  });

  const porDia = new Map<string, Task[]>();
  for (const tarefa of tarefas) {
    if (!tarefa.dueDate) {
      continue;
    }
    const chaveDia = chave(dataLocalISO(tarefa.dueDate));
    const lista = porDia.get(chaveDia) ?? [];
    lista.push(tarefa);
    porDia.set(chaveDia, lista);
  }

  const diasDaSemana = Array.from({ length: 7 }, (_, i) => {
    const dia = new Date(hoje);
    dia.setDate(hoje.getDate() + i);
    return dia;
  });

  const ano = hoje.getFullYear();
  const mes = hoje.getMonth();
  const primeiroDoMes = new Date(ano, mes, 1);
  const offset = primeiroDoMes.getDay();
  const totalDiasMes = new Date(ano, mes + 1, 0).getDate();
  const celulas = Array.from({ length: offset + totalDiasMes }, (_, i) => i - offset + 1);

  const selecionadas = diaSelecionado ? (porDia.get(diaSelecionado) ?? []) : [];

  return (
    <>
      <section className="schedule" aria-label="Linha do tempo das próximas tarefas">
        <h3 className="schedule__titulo">Próximos 7 dias</h3>
        <ul className="schedule__semana">
          {diasDaSemana.map((dia) => {
            const chaveDia = chave(dia);
            const doDia = porDia.get(chaveDia) ?? [];
            const ehHoje = chave(dia) === chave(hoje);
            return (
              <li key={chaveDia}>
                <button
                  type="button"
                  className={`schedule__dia-celula${ehHoje ? ' schedule__dia-celula--hoje' : ''}`}
                  aria-label={`Atividades de ${formatarData(chaveDia)}`}
                  onClick={() => setDiaSelecionado(chaveDia)}
                >
                  <span className="schedule__dia-nome">
                    {ehHoje ? 'hoje' : DIAS_SEMANA[dia.getDay()]}
                  </span>
                  <span className="schedule__dia-numero">{dia.getDate()}</span>
                  <span className="schedule__dia-mes">{MESES[dia.getMonth()]}</span>
                  {doDia.length > 0 && (
                    <span className="schedule__dia-contagem">
                      {doDia.length} {doDia.length === 1 ? 'tarefa' : 'tarefas'}
                    </span>
                  )}
                </button>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="schedule" aria-label="Calendário de tarefas">
        <h3 className="schedule__titulo">
          {MESES[mes]} {ano}
        </h3>
        <div className="schedule__calendario" role="grid" aria-label="Calendário do mês">
          <div className="schedule__cabecalho">
            {DIAS_SEMANA.map((nome) => (
              <span key={nome} className="schedule__cabecalho-dia">
                {nome}
              </span>
            ))}
          </div>
          {celulas.map((dia, i) => {
            const seVazio = dia < 1;
            if (seVazio) {
              return <span key={`vazio-${i}`} className="schedule__dia schedule__dia--vazio" />;
            }
            const data = new Date(ano, mes, dia);
            const chaveDia = chave(data);
            const doDia = porDia.get(chaveDia) ?? [];
            const ehHoje = chaveDia === chave(hoje);
            return (
              <button
                type="button"
                key={chaveDia}
                className={`schedule__dia${ehHoje ? ' schedule__dia--hoje' : ''}${doDia.length > 0 ? ' schedule__dia--com-tarefa' : ''}`}
                aria-label={`Atividades de ${formatarData(chaveDia)}`}
                onClick={() => setDiaSelecionado(chaveDia)}
              >
                <span className="schedule__dia-numero">{dia}</span>
                {doDia.length > 0 && (
                  <span className="schedule__dia-contagem">{doDia.length}</span>
                )}
              </button>
            );
          })}
        </div>
      </section>

      {diaSelecionado && (
        <div
          className="dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="schedule-dialogo-titulo"
        >
          <h4 id="schedule-dialogo-titulo">Atividades de {formatarData(diaSelecionado)}</h4>
          {selecionadas.length === 0 ? (
            <p className="async-state__empty">Nenhuma tarefa para este dia.</p>
          ) : (
            <ul className="schedule__resumo">
              {selecionadas.map((tarefa) => (
                <li key={tarefa.id} className="schedule__resumo-item">
                  {tarefa.title}
                  <span className="schedule__resumo-meta">
                    <span
                      className={`badge badge--prioridade badge--prioridade-${tarefa.priority.toLowerCase()}`}
                    >
                      {PRIORITY_LABELS[tarefa.priority]}
                    </span>
                    <span className={`badge badge--${tarefa.status.toLowerCase()}`}>
                      {STATUS_LABELS[tarefa.status]}
                    </span>
                  </span>
                  {tarefa.description && (
                    <span className="schedule__resumo-descricao">{tarefa.description}</span>
                  )}
                </li>
              ))}
            </ul>
          )}
          <div className="dialog__acoes">
            <button type="button" onClick={() => setDiaSelecionado(null)}>
              Fechar
            </button>
          </div>
        </div>
      )}
    </>
  );
}