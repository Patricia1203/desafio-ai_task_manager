package com.desafio.taskmanager.assistant.infra;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.desafio.taskmanager.assistant.domain.ChatConversation;
import com.desafio.taskmanager.assistant.domain.ChatMessage;
import com.desafio.taskmanager.assistant.domain.ChatRole;

import com.desafio.taskmanager.support.PostgresIntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
        conversa.defineTituloPadrao("Quais tarefas estao pendentes?");
        conversations.saveAndFlush(conversa);
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "Quais tarefas estao pendentes?"));
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.ASSISTANT, "Nenhuma tarefa encontrada."));

        List<ChatMessage> encontradas =
                messages.ultimasMensagens(conversa.getId(), PageRequest.of(0, 20));

        assertThat(encontradas).hasSize(2);
        assertThat(encontradas.get(0).getRole()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(encontradas.get(0).getContent()).isEqualTo("Nenhuma tarefa encontrada.");
        assertThat(encontradas.get(0).getCreatedAt()).isNotNull();
        assertThat(encontradas.get(1).getRole()).isEqualTo(ChatRole.USER);
        assertThat(encontradas.get(1).getContent()).isEqualTo("Quais tarefas estao pendentes?");
        assertThat(conversa.getCreatedAt()).isNotNull();
        assertThat(conversations.findById(conversa.getId()).orElseThrow().getTitle())
                .isEqualTo("Quais tarefas estao pendentes?");
    }

    @Test
    void listaMensagensDoMaisNovoParaOMaisVelho() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        UUID id = conversa.getId();
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Primeira mensagem"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.ASSISTANT, "Segunda mensagem"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Terceira mensagem"));

        // A consulta volta invertida porque e assim que o banco corta com o LIMIT
        // (T-F06-06); virar a ordem cronologica e responsabilidade da aplicacao,
        // coberto pelo AssistantServiceTest.
        assertThat(mensagensUltimas(id))
                .extracting(ChatMessage::getContent)
                .containsExactly("Terceira mensagem", "Segunda mensagem", "Primeira mensagem");
    }

    @Test
    void aJanelaTrazSoAsUltimasMensagensDoMaisNovoParaOMaisVelho() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        IntStream.rangeClosed(1, 25).forEach(i ->
                messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "msg " + i)));

        List<ChatMessage> janela = messages.ultimasMensagens(conversa.getId(), PageRequest.of(0, 20));

        assertThat(janela).hasSize(20);
        assertThat(janela.get(0).getContent()).isEqualTo("msg 25");
        assertThat(janela.get(19).getContent()).isEqualTo("msg 6");
    }

    @Test
    void aJanelaLimitaNoBancoEBuscaPeloIdDaConversa() {
        ChatConversation outra = conversations.saveAndFlush(ChatConversation.nova());
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(outra.getId(), ChatRole.USER, "de outra"));
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "desta"));

        List<ChatMessage> janela = messages.ultimasMensagens(conversa.getId(), PageRequest.of(0, 20));

        assertThat(janela).extracting(ChatMessage::getContent).containsExactly("desta");
    }

    @Test
    void isolaMensagensEntreConversas() {
        ChatConversation primeira = conversations.saveAndFlush(ChatConversation.nova());
        ChatConversation segunda = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(primeira.getId(), ChatRole.USER, "Da primeira"));
        messages.saveAndFlush(new ChatMessage(segunda.getId(), ChatRole.USER, "Da segunda"));

        List<ChatMessage> soPrimeira =
                messages.ultimasMensagens(primeira.getId(), PageRequest.of(0, 20));

        assertThat(soPrimeira).extracting(ChatMessage::getContent).containsExactly("Da primeira");
    }

    @Test
    void listaConversasDaMaisRecenteParaAMaisAntigaPelaAtividade() {
        ChatConversation antiga = conversations.saveAndFlush(ChatConversation.nova());
        ChatConversation recente = conversations.saveAndFlush(ChatConversation.nova());
        recente.toca();
        conversations.saveAndFlush(recente);

        List<ChatConversation> historico = conversations.findAllByOrderByUpdatedAtDesc();

        assertThat(historico).extracting(ChatConversation::getId)
                .containsExactly(recente.getId(), antiga.getId());
    }

    @Test
    void historicoCompletoVemEmOrdemCronologica() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        UUID id = conversa.getId();
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Primeira"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.ASSISTANT, "Segunda"));
        messages.saveAndFlush(new ChatMessage(id, ChatRole.USER, "Terceira"));

        List<ChatMessage> completo = messages.historicoCompleto(id);

        assertThat(completo).extracting(ChatMessage::getContent)
                .containsExactly("Primeira", "Segunda", "Terceira");
    }

    @Test
    void deletarConversaRemoveAsMensagensEmCascata() {
        ChatConversation conversa = conversations.saveAndFlush(ChatConversation.nova());
        messages.saveAndFlush(new ChatMessage(conversa.getId(), ChatRole.USER, "Vai sumir junto"));

        conversations.deleteById(conversa.getId());
        conversations.flush();

        assertThat(conversations.findById(conversa.getId())).isEmpty();
        assertThat(messages.ultimasMensagens(conversa.getId(), PageRequest.of(0, 20))).isEmpty();
    }

    /** A janela do service (T-F06-06) e a consulta com o limite; a ordem e feita na aplicacao. */
    private List<ChatMessage> mensagensUltimas(UUID conversationId) {
        return messages.ultimasMensagens(conversationId, PageRequest.of(0, 20));
    }
}