import type { MensagemChat } from '../../types/assistant';

interface MessageBubbleProps {
  mensagem: MensagemChat;
}

export default function MessageBubble({ mensagem }: MessageBubbleProps) {
  const usuario = mensagem.papel === 'usuario';
  return (
    <li
      className={`assistant__bolha ${usuario ? 'assistant__bolha--usuario' : 'assistant__bolha--assistente'}`}
    >
      <span className="assistant__autoria">{usuario ? 'Você' : 'Assistente'}</span>
      <p className="assistant__texto">{mensagem.texto}</p>
    </li>
  );
}