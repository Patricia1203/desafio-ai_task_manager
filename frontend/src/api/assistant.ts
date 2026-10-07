import { request } from './client';
import type { ChatReply } from '../types/assistant';

/** POST /assistant/chat. conversationId nulo cria uma conversa nova; preenchido retoma o histórico. */
export function enviarMensagem(
  conversationId: string | null,
  mensagem: string,
): Promise<ChatReply> {
  return request<ChatReply>('/assistant/chat', {
    method: 'POST',
    body: JSON.stringify(
      conversationId === null ? { message: mensagem } : { conversationId, message: mensagem },
    ),
  });
}