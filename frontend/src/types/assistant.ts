export interface ChatReply {
  conversationId: string;
  response: string;
}

export type ChatRole = 'user' | 'assistant';

/** Mensagem exibida na janela de conversa: o id só identifica a bolha na UI, não vem da API. */
export interface ChatMessage {
  id: string;
  role: ChatRole;
  text: string;
}