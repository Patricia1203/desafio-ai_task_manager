import { useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import type { ChatMessage } from '../../types/assistant';
import MessageBubble from './MessageBubble';

interface ChatWindowProps {
  mensagens: ChatMessage[];
  digitando: boolean;
  erro: string | null;
  onEnviar: (texto: string) => void;
}

export default function ChatWindow({ mensagens, digitando, erro, onEnviar }: ChatWindowProps) {
  const [texto, setTexto] = useState('');

  function enviar() {
    const conteudo = texto.trim();
    if (conteudo === '' || digitando) {
      return;
    }
    setTexto('');
    onEnviar(conteudo);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    enviar();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      enviar();
    }
  }

  return (
    <div className="assistant__janela" role="region" aria-label="Conversa">
      {mensagens.length === 0 ? (
        <p role="status" className="assistant__vazio">
          Mande uma mensagem para consultar o assistente.
        </p>
      ) : (
        <ol className="assistant__bolhas" aria-live="polite">
          {mensagens.map((mensagem) => (
            <MessageBubble key={mensagem.id} mensagem={mensagem} />
          ))}
          {digitando && (
            <li className="assistant__bolha assistant__bolha--assistente">
              <span className="assistant__autoria">Assistente</span>
              <p className="assistant__digitando" role="status">
                Digitando...
              </p>
            </li>
          )}
        </ol>
      )}

      {erro && (
        <p className="error-message" role="alert">
          {erro}
        </p>
      )}

      <form className="assistant__form" onSubmit={handleSubmit}>
        <label htmlFor="assistant-entrada" className="field">
          <span className="field__label">Sua mensagem</span>
          <textarea
            id="assistant-entrada"
            rows={3}
            maxLength={5000}
            value={texto}
            disabled={digitando}
            placeholder="Pergunte sobre suas tarefas (Enter para enviar, Shift+Enter para quebrar linha)"
            onChange={(event) => setTexto(event.target.value)}
            onKeyDown={handleKeyDown}
          />
        </label>
        <button type="submit" disabled={digitando || texto.trim() === ''}>
          Enviar
        </button>
      </form>
    </div>
  );
}