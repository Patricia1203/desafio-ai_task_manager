export interface RespostaChat {
  conversationId: string;
  resposta: string;
}

export type PapelMensagem = 'usuario' | 'assistente';

/** Mensagem exibida na janela de conversa: o id só identifica a bolha na UI, não vem da API. */
export interface MensagemChat {
  id: string;
  papel: PapelMensagem;
  texto: string;
}