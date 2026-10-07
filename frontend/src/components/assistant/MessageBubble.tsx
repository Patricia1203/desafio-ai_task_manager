import type { ChatMessage } from '../../types/assistant';

interface MessageBubbleProps {
  mensagem: ChatMessage;
}

export default function MessageBubble({ mensagem }: MessageBubbleProps) {
  const usuario = mensagem.role === 'user';
  return (
    <li
      className={`assistant__bolha ${usuario ? 'assistant__bolha--usuario' : 'assistant__bolha--assistente'}`}
    >
      <span className="assistant__autoria">{usuario ? 'Você' : 'Assistente'}</span>
      <p className="assistant__texto">{mensagem.text}</p>
    </li>
  );
}