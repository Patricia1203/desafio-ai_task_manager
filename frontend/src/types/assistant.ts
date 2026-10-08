export interface ChatReply {
  conversationId: string;
  response: unknown;
}

export type ChatRole = 'user' | 'assistant';

/** Mensagem exibida na janela de conversa: o id só identifica a bolha na UI, não vem da API. */
export interface ChatMessage {
  id: string;
  role: ChatRole;
  text: string;
}

/** Item do histórico de conversas (F10): o que a sidebar lista e retoma. */
export interface ConversationSummary {
  id: string;
  title: string;
  updatedAt: string;
}

/** Mensagem de uma conversa restaurada (F10): role em minúsculo, vindo da API. */
export interface ConversationMessage {
  role: ChatRole;
  content: string;
}

/** Perfil de uma conversa do histórico: resumo + mensagens na ordem cronológica. */
export interface ConversationDetail extends ConversationSummary {
  messages: ConversationMessage[];
}

/** Mensagem vinda de GET /assistant/conversations/{id}/messages (T-F06-10): id e createdAt reais da conversa salva. */
export interface StoredConversationMessage {
  id: number;
  role: ChatRole;
  content: string;
  createdAt: string;
}