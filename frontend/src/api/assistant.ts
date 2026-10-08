import { request } from './client';
import type {
  ChatReply,
  ConversationDetail,
  ConversationSummary,
  StoredConversationMessage,
} from '../types/assistant';

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

/** GET /assistant/conversations (F10): histórico com título e última atividade, do mais recente para o mais antigo. */
export function listarConversas(): Promise<ConversationSummary[]> {
  return request<ConversationSummary[]>('/assistant/conversations');
}

/** GET /assistant/conversations/{id} (F10): a conversa inteira para restaurar na janela. */
export function buscarConversa(id: string): Promise<ConversationDetail> {
  return request<ConversationDetail>(`/assistant/conversations/${encodeURIComponent(id)}`);
}

/** GET /assistant/conversations/{id}/messages (T-F06-10): mensagens em ordem cronologica com id e createdAt. */
export function buscarMensagens(id: string): Promise<StoredConversationMessage[]> {
  return request<StoredConversationMessage[]>(
    `/assistant/conversations/${encodeURIComponent(id)}/messages`,
  );
}