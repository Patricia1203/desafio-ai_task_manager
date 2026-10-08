import { useRef, useState } from 'react';
import { enviarMensagem } from '../api/assistant';
import type { ChatMessage, ChatRole } from '../types/assistant';
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

export default function AssistantPage() {
  const [mensagens, setMensagens] = useState<ChatMessage[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [digitando, setDigitando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const proximoId = useRef(1);

  function adicionarMensagem(role: ChatRole, text: string) {
    setMensagens((anteriores) => [
      ...anteriores,
      { id: `msg-${proximoId.current++}`, role, text },
    ]);
  }

  async function enviar(texto: string) {
    setErro(null);
    adicionarMensagem('user', texto);
    setDigitando(true);
    try {
      const reply = await enviarMensagem(conversationId, texto);
      setConversationId(reply.conversationId);
      adicionarMensagem('assistant', responseText(reply.response));
    } catch (caught) {
      setErro(messageOf(caught));
    } finally {
      setDigitando(false);
    }
  }

  function novaConversa() {
    setMensagens([]);
    setConversationId(null);
    setErro(null);
  }

  return (
    <section aria-labelledby="assistant-heading" className="assistant">
      <header className="assistant__header">
        <h2 id="assistant-heading">Assistente</h2>
        <button type="button" onClick={novaConversa} disabled={digitando || mensagens.length === 0}>
          Nova conversa
        </button>
      </header>

      <ChatWindow mensagens={mensagens} digitando={digitando} erro={erro} onEnviar={enviar} />
    </section>
  );
}