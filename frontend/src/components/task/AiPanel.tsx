import { useState } from 'react';
import type { Analise, Melhoria, SugestaoSubtarefa } from '../../types/ai';
import type { Task } from '../../types/task';
import { ApiError } from '../../api/client';
import { analyzeTask, applyDecomposition, decomposeTask, improveTask } from '../../api/ai';
import { updateTask } from '../../api/tasks';
import ImproveResult from './ImproveResult';
import AnalysisResult from './AnalysisResult';
import DecompositionResult from './DecompositionResult';

interface AiPanelProps {
  task: Task;
  onChanged: (task: Task) => void;
  onSubtasksCreated: () => void;
}

type Operacao = 'melhorar' | 'analisar' | 'dividir' | 'aplicar' | 'adicionar';

function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError && erro.problem.code === 'LLM_INVALID_RESPONSE') {
    return 'A IA não conseguiu montar uma resposta válida desta vez. Tente novamente em instantes.';
  }
  if (erro instanceof Error) {
    return erro.message;
  }
  return 'Erro inesperado.';
}

export default function AiPanel({ task, onChanged, onSubtasksCreated }: AiPanelProps) {
  const [operacao, setOperacao] = useState<Operacao | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [melhoria, setMelhoria] = useState<Melhoria | null>(null);
  const [analise, setAnalise] = useState<Analise | null>(null);
  const [sugestoes, setSugestoes] = useState<SugestaoSubtarefa[] | null>(null);

  const ocupado = operacao !== null;

  async function executar(op: Operacao, acao: () => Promise<void>) {
    setOperacao(op);
    setErro(null);
    try {
      await acao();
    } catch (caught) {
      setErro(mensagemDeErro(caught));
    } finally {
      setOperacao(null);
    }
  }

  function melhorar() {
    return executar('melhorar', async () => {
      setMelhoria(await improveTask(task.title, task.description));
    });
  }

  function analisar() {
    return executar('analisar', async () => {
      setAnalise(await analyzeTask(task.id));
    });
  }

  function dividir() {
    return executar('dividir', async () => {
      const resposta = await decomposeTask(task.id);
      setSugestoes(resposta.subtasks.map((sugestao) => ({ ...sugestao, selecionada: true })));
      setMelhoria(null);
      setAnalise(null);
    });
  }

  function aplicarMelhoria() {
    if (!melhoria) {
      return;
    }
    const sugestao = melhoria;
    return executar('aplicar', async () => {
      const atualizada = await updateTask(task.id, {
        title: sugestao.title,
        description: sugestao.description,
        priority: task.priority,
        dueDate: task.dueDate,
      });
      setMelhoria(null);
      onChanged(atualizada);
    });
  }

  function adicionarSubtarefas() {
    if (!sugestoes) {
      return;
    }
    const escolhidas = sugestoes
      .filter((sugestao) => sugestao.selecionada)
      .map(({ title, description, estimatedHours }) => ({ title, description, estimatedHours }));
    if (escolhidas.length === 0) {
      return;
    }
    return executar('adicionar', async () => {
      await applyDecomposition(task.id, escolhidas);
      setSugestoes(null);
      onSubtasksCreated();
    });
  }

  function alternarSelecao(indice: number) {
    setSugestoes((anteriores) =>
      anteriores === null
        ? null
        : anteriores.map((sugestao, i) =>
            i === indice ? { ...sugestao, selecionada: !sugestao.selecionada } : sugestao,
          ),
    );
  }

  function removerSugestao(indice: number) {
    setSugestoes((anteriores) =>
      anteriores === null ? null : anteriores.filter((_, i) => i !== indice),
    );
  }

  return (
    <section className="ai-panel" aria-labelledby="ai-panel-titulo">
      <h4 id="ai-panel-titulo">IA da tarefa</h4>

      <div className="ai-panel__acoes">
        <button type="button" onClick={melhorar} disabled={ocupado}>
          {operacao === 'melhorar' ? 'Melhorando...' : 'Melhorar'}
        </button>
        <button type="button" onClick={analisar} disabled={ocupado}>
          {operacao === 'analisar' ? 'Analisando...' : 'Analisar'}
        </button>
        <button type="button" onClick={dividir} disabled={ocupado}>
          {operacao === 'dividir' ? 'Dividindo...' : 'Dividir'}
        </button>
      </div>

      {ocupado && (
        <p role="status" className="ai-panel__status">
          Consultando a IA...
        </p>
      )}

      {erro && (
        <p className="error-message" role="alert">
          {erro}
        </p>
      )}

      {melhoria && (
        <ImproveResult
          melhoria={melhoria}
          aplicando={operacao === 'aplicar'}
          onAplicar={aplicarMelhoria}
          onDescartar={() => setMelhoria(null)}
        />
      )}

      {analise && <AnalysisResult analise={analise} onFechar={() => setAnalise(null)} />}

      {sugestoes && (
        <DecompositionResult
          sugestoes={sugestoes}
          adicionando={operacao === 'adicionar'}
          onAlternar={alternarSelecao}
          onRemover={removerSugestao}
          onAdicionar={adicionarSubtarefas}
          onDescartar={() => setSugestoes(null)}
        />
      )}
    </section>
  );
}
