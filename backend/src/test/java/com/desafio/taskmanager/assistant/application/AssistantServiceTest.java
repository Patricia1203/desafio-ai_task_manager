package com.desafio.taskmanager.assistant.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import com.desafio.taskmanager.assistant.domain.ChatConversation;
import com.desafio.taskmanager.assistant.domain.ChatMessage;
import com.desafio.taskmanager.assistant.domain.ChatRole;
import com.desafio.taskmanager.assistant.infra.ChatConversationRepository;
import com.desafio.taskmanager.assistant.infra.ChatMessageRepository;
import com.desafio.taskmanager.assistant.port.AssistantPort;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de negocio da F04 contra a porta fake: conversa nova sem id, retomada
 * pelo conversationId, janela de historico limitada e persistencia so depois
 * da resposta da IA. O contrato HTTP e o mapeamento de erros ficam no
 * {@code AssistantControllerTest}.
 */
class AssistantServiceTest {

    private ChatConversationRepository conversas;
    private ChatMessageRepository mensagens;
    private PortaFake ia;
    private AssistantService service;

    @BeforeEach
    void setUp() {
        conversas = mock(ChatConversationRepository.class);
        mensagens = mock(ChatMessageRepository.class);
        ia = new PortaFake();
        service = new AssistantService(ia, conversas, mensagens, new ChatTurnWriter(conversas, mensagens));
        when(conversas.save(any(ChatConversation.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));
        when(mensagens.save(any(ChatMessage.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));
    }

    // --- conversa nova / retomada ---

    @Test
    void semConversationIdCriaConversaNovaEPersisteOTurno() {
        AssistantService.RespostaChat resultado = service.chat(null, "Quais tarefas estao pendentes?");

        assertThat(resultado.conversationId()).isNotNull();
        assertThat(resultado.resposta()).isEqualTo("Nenhuma tarefa encontrada.");
        assertThat(ia.historicos.get(0)).isEmpty();
        assertThat(ia.mensagens).containsExactly("Quais tarefas estao pendentes?");
        verify(conversas).save(any(ChatConversation.class));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(mensagens, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ChatMessage::getRole)
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(captor.getAllValues()).extracting(ChatMessage::getContent)
                .containsExactly("Quais tarefas estao pendentes?", "Nenhuma tarefa encontrada.");
        assertThat(captor.getAllValues()).allSatisfy(linha ->
                assertThat(linha.getConversationId()).isEqualTo(resultado.conversationId()));
    }

    @Test
    void comConversationIdRetomaOHistoricoApartirDasMensagensPersistidas() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        // O banco entrega do mais novo para o mais velho; o service inverte
        // antes de mandar ao modelo (T-F06-06).
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of(
                new ChatMessage(id, ChatRole.ASSISTANT, "Resposta da primeira"),
                new ChatMessage(id, ChatRole.USER, "Primeira")));

        AssistantService.RespostaChat resultado = service.chat(id, "Segunda pergunta");

        assertThat(resultado.conversationId()).isEqualTo(conversa.getId());
        assertThat(ia.historicos.get(0)).extracting(AssistantPort.Mensagem::content)
                .containsExactly("Primeira", "Resposta da primeira");
        assertThat(ia.historicos.get(0)).extracting(AssistantPort.Mensagem::role)
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(ia.mensagens).containsExactly("Segunda pergunta");
        // A conversa retomada volta a ser enviada ao repositorio, mas e merge da
        // mesma conversa: nenhuma conversa nova e criada neste caminho (T-F06-06).
        verify(conversas).save(conversa);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(mensagens, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ChatMessage::getRole)
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(captor.getAllValues()).extracting(ChatMessage::getContent)
                .containsExactly("Segunda pergunta", "Nenhuma tarefa encontrada.");
        assertThat(captor.getAllValues()).allSatisfy(linha ->
                assertThat(linha.getConversationId()).isEqualTo(conversa.getId()));
    }

    @Test
    void conversationIdInexistenteLanca404SemPersistirNada() {
        UUID id = UUID.randomUUID();
        when(conversas.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.chat(id, "Qualquer pergunta"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(conversas, never()).save(any(ChatConversation.class));
        verify(mensagens, never()).save(any(ChatMessage.class));
    }

    // --- janela de historico ---

    @Test
    void enviaAsUltimasVinteMensagensDaConversaEmOrdemCronologica() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        // O banco ja corta em 20 e entrega do mais novo para o mais velho
        // (T-F06-06); a janela que o modelo le precisa estar invertida.
        List<ChatMessage> janelaDoBanco = IntStream.rangeClosed(0, 19)
                .mapToObj(i -> new ChatMessage(id,
                        i % 2 == 0 ? ChatRole.USER : ChatRole.ASSISTANT, "msg " + (24 - i)))
                .toList();
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(janelaDoBanco);

        service.chat(id, "nova");

        assertThat(ia.historicos.get(0)).hasSize(20);
        assertThat(ia.historicos.get(0).get(0).content()).isEqualTo("msg 5");
        assertThat(ia.historicos.get(0).get(19).content()).isEqualTo("msg 24");
    }

    @Test
    void aJanelaEPedidaAoBancoComLimiteDeVinte() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of());

        service.chat(id, "nova");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(mensagens).ultimasMensagens(eq(conversa.getId()), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getPageNumber()).isZero();
    }

    @Test
    void gravaOTurnoSoDepoisDaRespostaDaIa() {
        AssistantPort porta = mock(AssistantPort.class);
        when(porta.chat(anyList(), anyString())).thenReturn("Resposta da IA");
        AssistantService comPortaMock =
                new AssistantService(porta, conversas, mensagens, new ChatTurnWriter(conversas, mensagens));
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of());

        comPortaMock.chat(null, "pergunta");

        // Nenhuma escrita antes da resposta: e o que impede a transacao de ficar
        // aberta enquanto o modelo pensa (T-F06-06).
        InOrder ordem = inOrder(porta, conversas, mensagens);
        ordem.verify(porta).chat(anyList(), eq("pergunta"));
        ordem.verify(conversas).save(any(ChatConversation.class));
        ordem.verify(mensagens, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void falhaDeComunicacaoComAIAPropagaSemPersistir() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of());
        LlmCommunicationException erro = new LlmCommunicationException("timeout no LLM");
        ia.falha = erro;

        assertThatThrownBy(() -> service.chat(id, "pergunta"))
                .isSameAs(erro);

        verify(mensagens, never()).save(any(ChatMessage.class));
    }

    @Test
    void iaIndisponivelPropagaSemPersistir() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of());
        LlmUnavailableException erro = new LlmUnavailableException("Connection refused");
        ia.falha = erro;

        assertThatThrownBy(() -> service.chat(id, "pergunta"))
                .isSameAs(erro);

        verify(mensagens, never()).save(any(ChatMessage.class));
    }

    @Test
    void respostaInvalidaDaIaPropagaSemPersistir() {
        UUID id = UUID.randomUUID();
        ChatConversation conversa = ChatConversation.nova();
        when(conversas.findById(id)).thenReturn(Optional.of(conversa));
        when(mensagens.ultimasMensagens(any(), any())).thenReturn(List.of());
        InvalidLlmResponseException erro =
                new InvalidLlmResponseException("resposta da IA fora do contrato");
        ia.falha = erro;

        assertThatThrownBy(() -> service.chat(id, "pergunta"))
                .isSameAs(erro);

        verify(mensagens, never()).save(any(ChatMessage.class));
    }

    /** Porta fake: guarda o historico e a mensagem recebidos; roteia resposta ou falha. */
    private static final class PortaFake implements AssistantPort {

        private final List<List<Mensagem>> historicos = new ArrayList<>();
        private final List<String> mensagens = new ArrayList<>();
        private String resposta = "Nenhuma tarefa encontrada.";
        private RuntimeException falha;

        @Override
        public String chat(List<Mensagem> historico, String mensagem) {
            historicos.add(historico);
            mensagens.add(mensagem);
            if (falha != null) {
                throw falha;
            }
            return resposta;
        }
    }
}