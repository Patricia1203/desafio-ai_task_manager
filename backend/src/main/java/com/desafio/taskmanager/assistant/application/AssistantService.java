package com.desafio.taskmanager.assistant.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.desafio.taskmanager.assistant.api.dto.ChatMessageResponse;
import com.desafio.taskmanager.assistant.api.dto.ConversationDetail;
import com.desafio.taskmanager.assistant.api.dto.ConversationMessage;
import com.desafio.taskmanager.assistant.api.dto.ConversationSummary;
import com.desafio.taskmanager.assistant.domain.ChatConversation;
import com.desafio.taskmanager.assistant.domain.ChatMessage;
import com.desafio.taskmanager.assistant.domain.ChatRole;
import com.desafio.taskmanager.assistant.infra.ChatConversationRepository;
import com.desafio.taskmanager.assistant.infra.ChatMessageRepository;
import com.desafio.taskmanager.assistant.port.AssistantPort;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Caso de uso do assistente (F04, US-030 a US-032): a conversa em linguagem
 * natural, com grounding pelas ferramentas somente-leitura, memoria persistida
 * e retomada pelo {@code conversationId}.
 *
 * <p>Regras de negocio daqui (RF-17, RF-18):
 * <ul>
 *   <li>sem {@code conversationId} o service cria uma conversa nova; com id,
 *       retoma a existente ou devolve 404 - nunca reusa mensagem de outra
 *       conversa;</li>
 *   <li>o historico vai ao modelo cortado na janela ({@link #JANELA_HISTORICO}
 *       mensagens), em ordem cronologica; mensagens antigas alem dela nao
 *       sobem para o provedor;</li>
 *   <li>roteiro imutavel por chamada: usuario e resposta so sao persistidos
 *       depois que a IA respondeu - se a chamada falhar, nenhuma mensagem para
 *       e gravada;</li>
 *   <li>nenhuma ferramenta de escrita: o assistente so consulta (RF-19).</li>
 * </ul>
 *
 * <p><b>Sem {@code @Transactional} na classe (T-F06-06).</b> A transacao ficava
 * aberta do inicio ao fim do caso de uso, entao a chamada ao LLM — que pode
 * levar minutos — segurava uma conexao do pool o tempo todo, e poucas
 * conversas simultaneas o esgotavam. Agora so a gravacao do turno tem
 * transacao, no {@link ChatTurnWriter}. Uma conversa nova nem chega a ser
 * gravada antes da resposta: se o modelo falhar, o banco continua vazio como
 * antes.
 *
 * <p>O service fala com {@link AssistantPort} (a porta da feature) e com os
 * dois repositorios de chat; nao importa Spring AI - quem conhece o provedor
 * e so {@code ai.adapter} (RNF-14).
 *
 * <p>F10: o mesmo service expoe o historico — {@link #listarConversas()} (a
 * sidebar com titulo e ultima atividade, da mais recente para a mais antiga)
 * e {@link #conversa(UUID)} (a conversa inteira para retomar). O titulo nasce
 * da primeira mensagem do usuario em {@code ChatTurnWriter#gravarTurno}.
 */
@Service
public class AssistantService {

    /** Janela de historico enviada ao modelo: as ultimas N mensagens da conversa. */
    private static final int JANELA_HISTORICO = 20;

    private final AssistantPort ia;
    private final ChatConversationRepository conversas;
    private final ChatMessageRepository mensagens;
    private final ChatTurnWriter gravador;

    public AssistantService(
            AssistantPort ia,
            ChatConversationRepository conversas,
            ChatMessageRepository mensagens,
            ChatTurnWriter gravador) {
        this.ia = ia;
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.gravador = gravador;
    }

    /**
     * US-030/US-031. Um turno da conversa: resolve a conversa (nova ou
     * retomada), monta o historico na janela, chama a IA com grounding e
     * persiste usuario + resposta.
     */
    public RespostaChat chat(UUID conversationId, String mensagem) {
        ChatConversation conversa = conversationId == null
                ? ChatConversation.nova()
                : conversas.findById(conversationId)
                        .orElseThrow(() -> ResourceNotFoundException.of("conversa", conversationId));

        List<AssistantPort.Mensagem> historico = historicoNaJanela(conversa.getId());

        String resposta = ia.chat(historico, mensagem);

        gravador.gravarTurno(conversa, mensagem, resposta);
        return new RespostaChat(conversa.getId(), resposta);
    }

    /**
     * A janela chega do banco do mais novo para o mais velho (e por isso o
     * {@code LIMIT} corta as mensagens antigas); aqui ela vira a ordem
     * cronologica que o modelo precisa ler.
     */
    private List<AssistantPort.Mensagem> historicoNaJanela(UUID conversationId) {
        List<ChatMessage> ultimas = mensagens.ultimasMensagens(
                conversationId, PageRequest.of(0, JANELA_HISTORICO));
        List<ChatMessage> janela = new ArrayList<>(ultimas);
        Collections.reverse(janela);
        return janela.stream()
                .map(linha -> new AssistantPort.Mensagem(linha.getRole(), linha.getContent()))
                .toList();
    }

    public record RespostaChat(UUID conversationId, String resposta) {
    }

    /** F10. O historico de conversas: id, titulo e ultima atividade, do mais recente para o mais antigo. */
    public List<ConversationSummary> listarConversas() {
        return conversas.findAllByOrderByUpdatedAtDesc().stream()
                .map(conversa -> new ConversationSummary(
                        conversa.getId(), tituloOuFallback(conversa), conversa.getUpdatedAt()))
                .toList();
    }

    /** F10. Perfil de uma conversa para restaurar: resumo + mensagens na ordem cronologica; 404 se nao existe. */
    public ConversationDetail conversa(UUID conversationId) {
        ChatConversation conversa = conversas.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("conversa", conversationId));
        List<ConversationMessage> linhas = mensagens.historicoCompleto(conversationId).stream()
                .map(linha -> new ConversationMessage(
                        linha.getRole().name().toLowerCase(Locale.ROOT), linha.getContent()))
                .toList();
        return new ConversationDetail(
                conversa.getId(), tituloOuFallback(conversa), conversa.getUpdatedAt(), linhas);
    }

    /** T-F06-10. As mensagens de uma conversa na ordem cronologica, com id e timestamp; 404 se a conversa nao existe. */
    public List<ChatMessageResponse> mensagensDaConversa(UUID conversationId) {
        conversas.findById(conversationId)
                .orElseThrow(() -> ResourceNotFoundException.of("conversa", conversationId));
        return mensagens.historicoCompleto(conversationId).stream()
                .map(linha -> new ChatMessageResponse(
                        linha.getId(),
                        linha.getRole().name().toLowerCase(Locale.ROOT),
                        linha.getContent(),
                        linha.getCreatedAt()))
                .toList();
    }

    private static String tituloOuFallback(ChatConversation conversa) {
        return conversa.getTitle() == null || conversa.getTitle().isBlank()
                ? "Conversa"
                : conversa.getTitle();
    }
}