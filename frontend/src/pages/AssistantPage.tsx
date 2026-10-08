import { useCallback, useEffect, useRef, useState } from 'react';
import { buscarConversa, enviarMensagem, listarConversas } from '../api/assistant';
import type { ChatMessage, ChatRole, ConversationSummary } from '../types/assistant';
import ChatWindow from '../components/assistant/ChatWindow';

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
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [digitando, setDigitando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [conversas, setConversas] = useState<ConversationSummary[]>([]);
  const [ativaId, setAtivaId] = useState<string | null>(null);
  const [carregandoConversa, setCarregandoConversa] = useState(false);
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

  function adicionarMensagem(role: ChatRole, text: string) {
    setMensagens((anteriores) => [
      ...anteriores,
      { id: `msg-${proximoId.current++}`, role, text },
    ]);
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
      setConversationId(perfil.id);
      setAtivaId(perfil.id);
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
      setConversationId(reply.conversationId);
      setAtivaId(reply.conversationId);
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
    setConversationId(null);
    setAtivaId(null);
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