import { useRef, useState } from 'react';
import { enviarMensagem } from '../api/assistant';
import type { MensagemChat, PapelMensagem } from '../types/assistant';
import ChatWindow from '../components/assistant/ChatWindow';

function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Erro inesperado.';
}

export default function AssistantPage() {
  const [mensagens, setMensagens] = useState<MensagemChat[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [digitando, setDigitando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const proximoId = useRef(1);

  function adicionarMensagem(papel: PapelMensagem, texto: string) {
    setMensagens((anteriores) => [
      ...anteriores,
      { id: `msg-${proximoId.current++}`, papel, texto },
    ]);
  }

  async function enviar(texto: string) {
    setErro(null);
    adicionarMensagem('usuario', texto);
    setDigitando(true);
    try {
      const resposta = await enviarMensagem(conversationId, texto);
      setConversationId(resposta.conversationId);
      adicionarMensagem('assistente', resposta.resposta);
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