package com.desafio.taskmanager.assistant.application;

import com.desafio.taskmanager.assistant.domain.ChatConversation;
import com.desafio.taskmanager.assistant.domain.ChatMessage;
import com.desafio.taskmanager.assistant.domain.ChatRole;
import com.desafio.taskmanager.assistant.infra.ChatConversationRepository;
import com.desafio.taskmanager.assistant.infra.ChatMessageRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gravacao de um turno do assistente (F04, T-F06-06): a conversa (quando nova)
 * e as duas mensagens, USER e ASSISTANT, na mesma transacao.
 *
 * <p>Fica numa classe propria porque a transacao tem que começar depois da
 * resposta da IA: enquanto o modelo pensa, nenhuma conexao fica reservada.
 * Como metodo publico de outro bean, o proxy do Spring abre a transacao de
 * verdade — um {@code @Transactional} em metodo privado chamado de dentro da
 * propria classe seria ignorado.
 */
@Component
public class ChatTurnWriter {

    private final ChatConversationRepository conversas;
    private final ChatMessageRepository mensagens;

    public ChatTurnWriter(ChatConversationRepository conversas, ChatMessageRepository mensagens) {
        this.conversas = conversas;
        this.mensagens = mensagens;
    }

    /**
     * Grava o turno completo. Se a IA falhou, o metodo nunca e chamado e nada
     * vai para o banco.
     *
     * <p>F10: antes de gravar, a conversa nova deriva o {@code title} do
     * historico da primeira mensagem do usuario e a conversa (nova ou
     * retomada) registra a ultima atividade ({@code updatedAt}) — e isso que
     * mantem a lista de conversas do mais recente para o mais antigo.
     */
    @Transactional
    public void gravarTurno(ChatConversation conversa, String mensagem, String resposta) {
        conversa.defineTituloPadrao(mensagem);
        conversa.toca();
        conversas.save(conversa);
        mensagens.save(new ChatMessage(conversa.getId(), ChatRole.USER, mensagem));
        mensagens.save(new ChatMessage(conversa.getId(), ChatRole.ASSISTANT, resposta));
    }
}