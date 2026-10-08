import { useCallback, useEffect, useRef, useState } from 'react';
import { buscarConversa, buscarMensagens, enviarMensagem, listarConversas } from '../api/assistant';
import type { ChatMessage, ChatRole, ConversationSummary } from '../types/assistant';
import ChatWindow from '../components/assistant/ChatWindow';

const CHAVE_CONVERSA = 'task.assistant.conversationId';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

function responseText(response: unknown): string {
  if (typeof response === 'string') {
    return response;
  }

  if (response && typeof response === 'object') {
    const data = response as Record<string, unknown>;
    for (const key of ['message', 'response', 'content', 'answer']) {
      if (typeof data[key] === 'string') {
        return data[key];
      }
    }
    return JSON.stringify(response);
  }

  return String(response ?? '');
}

function formatarData(iso: string): string {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) {
    return '';
  }
  return data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export default function AssistantPage() {
  const [mensagens, setMensagens] = useState<ChatMessage[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(
    () => sessionStorage.getItem(CHAVE_CONVERSA),
  );
  const [digitando, setDigitando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [conversas, setConversas] = useState<ConversationSummary[]>([]);
  const [ativaId, setAtivaId] = useState<string | null>(null);
  const [carregandoConversa, setCarregandoConversa] = useState<boolean>(
    () => sessionStorage.getItem(CHAVE_CONVERSA) !== null,
  );
  const [historicoVisivel, setHistoricoVisivel] = useState(true);
  const proximoId = useRef(1);

  const recarregarConversas = useCallback(() => {
    listarConversas()
      .then(setConversas)
      .catch(() => {
        // Histórico é suporte: falha ao listar não quebra a conversa em andamento.
      });
  }, []);

  useEffect(() => {
    recarregarConversas();
  }, [recarregarConversas]);

  useEffect(() => {
    const salva = sessionStorage.getItem(CHAVE_CONVERSA);
    if (salva === null) {
      return;
    }
    buscarMensagens(salva)
      .then((linhas) => {
        setMensagens(
          linhas.map((linha) => ({
            id: `msg-${proximoId.current++}`,
            role: linha.role,
            text: linha.content,
          })),
        );
        setConversationId(salva);
        setAtivaId(salva);
      })
      .catch(() => {
        // A conversa salva nao existe mais (404): recomeca sem ela gravada.
        sessionStorage.removeItem(CHAVE_CONVERSA);
        setConversationId(null);
        setAtivaId(null);
      })
      .finally(() => setCarregandoConversa(false));
  }, []);

  function adicionarMensagem(role: ChatRole, text: string) {
    setMensagens((anteriores) => [
      ...anteriores,
      { id: `msg-${proximoId.current++}`, role, text },
    ]);
  }

  function persistirConversa(id: string | null) {
    setConversationId(id);
    setAtivaId(id);
    if (id === null) {
      sessionStorage.removeItem(CHAVE_CONVERSA);
    } else {
      sessionStorage.setItem(CHAVE_CONVERSA, id);
    }
  }

  async function abrirConversa(conversa: ConversationSummary) {
    setErro(null);
    setCarregandoConversa(true);
    try {
      const perfil = await buscarConversa(conversa.id);
      setMensagens(
        perfil.messages.map((linha) => ({
          id: `msg-${proximoId.current++}`,
          role: linha.role,
          text: linha.content,
        })),
      );
      persistirConversa(perfil.id);
    } catch (caught) {
      setErro(messageOf(caught));
    } finally {
      setCarregandoConversa(false);
    }
  }

  async function enviar(texto: string) {
    setErro(null);
    adicionarMensagem('user', texto);
    setDigitando(true);
    try {
      const reply = await enviarMensagem(conversationId, texto);
      persistirConversa(reply.conversationId);
      adicionarMensagem('assistant', responseText(reply.response));
      recarregarConversas();
    } catch (caught) {
      setErro(messageOf(caught));
    } finally {
      setDigitando(false);
    }
  }

  function novaConversa() {
    setMensagens([]);
    persistirConversa(null);
    setErro(null);
  }

  return (
    <section aria-labelledby="assistant-heading" className="assistant">
      <header className="assistant__header">
        <h2 id="assistant-heading">Assistente</h2>
        <div className="assistant__acoes">
          <button
            type="button"
            onClick={() => setHistoricoVisivel((visivel) => !visivel)}
            aria-expanded={historicoVisivel}
            aria-controls="assistant-historico"
          >
            {historicoVisivel ? 'Ocultar histórico' : 'Mostrar histórico'}
          </button>
          <button type="button" onClick={novaConversa} disabled={digitando || mensagens.length === 0}>
            Nova conversa
          </button>
        </div>
      </header>

      <div className="assistant__corpo">
        {historicoVisivel && (
          <nav
            className="assistant__historico"
            id="assistant-historico"
            aria-label="Histórico de conversas"
          >
            <h3 className="assistant__historico-titulo">Histórico</h3>
            {conversas.length === 0 ? (
              <p className="assistant__historico-vazio">Nenhuma conversa salva.</p>
            ) : (
              <ol className="assistant__conversas">
                {conversas.map((conversa) => (
                  <li
                    key={conversa.id}
                    className={conversa.id === ativaId ? 'assistant__conversa--ativa' : ''}
                  >
                    <button
                      type="button"
                      onClick={() => abrirConversa(conversa)}
                      disabled={digitando || carregandoConversa}
                      aria-current={conversa.id === ativaId ? 'true' : undefined}
                    >
                      <span className="assistant__conversa-titulo">
                        {conversa.title || 'Conversa'}
                      </span>
                      <span className="assistant__conversa-data">
                        {formatarData(conversa.updatedAt)}
                      </span>
                    </button>
                  </li>
                ))}
              </ol>
            )}
          </nav>
        )}

        <ChatWindow mensagens={mensagens} digitando={digitando} erro={erro} onEnviar={enviar} />
      </div>
    </section>
  );
}