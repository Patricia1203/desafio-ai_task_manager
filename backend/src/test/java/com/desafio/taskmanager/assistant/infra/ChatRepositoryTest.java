package com.desafio.taskmanager.assistant.infra;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.domain.ChatConversation;
import com.desafio.taskmanager.assistant.domain.ChatMessage;
import com.desafio.taskmanager.assistant.domain.ChatRole;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TST-04 contra Postgres real: as entidades de chat precisam mapear exatamente
 * o que a migration V3 criou, sob {@code ddl-auto=validate}. Qualquer divergencia
 * de coluna ou de tipo quebra o boot do contexto, nao so um assert.
 *
 * <p>O container vem de {@link PostgresIntegrationTest} (compartilhado); o
 * {@code @BeforeEach} limpa as tabelas de chat porque o banco atravessa as
 * classes de integracao.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ChatRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private ChatConversationRepository conversations;

    @Autowired
    private ChatMessageRepository messages;

    @BeforeEach
    void limpaTabelas() {
        conversations.deleteAll();
        conversations.flush();
    }

    @Test
    void gravaELeConversaComMensagens() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "Quais tarefas estao pendentes?"));
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.ASSISTANT, "Nenhuma tarefa encontrada."));

        List<ChatMessage> encontradas =
                messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversa.getId());

        assertThat(encontradas).hasSize(2);
        assertThat(encontradas.get(0).getRole()).isEqualTo(ChatRole.USER);
        assertThat(encontradas.get(0).getContent()).isEqualTo("Quais tarefas estao pendentes?");
        assertThat(encontradas.get(0).getCreatedAt()).isNotNull();
        assertThat(encontradas.get(1).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(encontradas.get(1).getContent()).isEqualTo("Nenhuma tarefa encontrada.");
        assertThat(conversa.getCreatedAt()).isNotNull();
    }

    @Test
    void listaMensagensEmOrdemCronologica() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        UUID id = conversa.getId();
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Primeira mensagem"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.ASSISTANT, "Segunda mensagem"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Terceira mensagem"));

        assertThat(messages.findByConversationIdOrderByCreatedAtAscIdAsc(id))
                .extracting(ChatMessage::getContent)
                .containsExactly("Primeira mensagem", "Segunda mensagem", "Terceira mensagem");
    }

    @Test
    void isolaMensagensEntreConversas() {
        ChatConversation primeira = conversations.saveAndFlush(ChatConversation.nova());
        ChatConversation segunda = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(primeira.getId(), ChatRole.USER, "Da primeira"));
        messages.saveAndFlush(new ChatMessage(segunda.getId(), ChatRole.USER, "Da segunda"));

        List<ChatMessage> soPrimeira =
                messages.findByConversationIdOrderByCreatedAtAscIdAsc(primeira.getId());

        assertThat(soPrimeira).extracting(ChatMessage::getContent).containsExactly("Da primeira");
    }

    @Test
    void deletarConversaRemoveAsMensagensEmCascata() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "Vai sumir junto"));

        conversations.deleteById(conversa.getId());
        conversations.flush();

        assertThat(conversations.findById(conversa.getId())).isEmpty();
        assertThat(messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversa.getId())).isEmpty();
    }
}