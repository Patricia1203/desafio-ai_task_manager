import { request } from './client';
import type { RespostaChat } from '../types/assistant';

/** POST /assistant/chat. conversationId nulo cria uma conversa nova; preenchido retoma o histórico. */
export function enviarMensagem(
  conversationId: string | null,
  mensagem: string,
): Promise<RespostaChat> {
  return request<RespostaChat>('/assistant/chat', {
    method: 'POST',
    body: JSON.stringify(conversationId === null ? { mensagem } : { conversationId, mensagem }),
  });
}