package com.desafio.taskmanager.assistant.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.api.dto.ChatMessageResponse;
import com.desafio.taskmanager.assistant.api.dto.ConversationDetail;
import com.desafio.taskmanager.assistant.api.dto.ConversationMessage;
import com.desafio.taskmanager.assistant.api.dto.ConversationSummary;
import com.desafio.taskmanager.assistant.application.AssistantService;
import com.desafio.taskmanager.common.error.GlobalExceptionHandler;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;
import com.desafio.taskmanager.common.error.LlmCommunicationException;
import com.desafio.taskmanager.common.error.LlmUnavailableException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.common.web.TraceIdFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP do assistente (US-030) com o service mockado: rotas, status
 * (200/400/404/502/503), JSON em portugues (RF-24) e formato ProblemDetail.
 * As regras de negocio ficam no {@code AssistantServiceTest}; o chapeu do
 * 400 usa a convencao de FieldErrorItem do resto da api.
 */
@WebMvcTest(AssistantController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class})
class AssistantControllerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService service;

    // --- POST /assistant/chat ---

    @Test
    void chatRetorna200ComConversaEResposta() throws Exception {
        when(service.chat(null, "Quais tarefas estao pendentes?"))
                .thenReturn(new AssistantService.RespostaChat(ID, "Temos 2 tarefas em aberto."));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Quais tarefas estao pendentes?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(ID.toString()))
                .andExpect(jsonPath("$.response").value("Temos 2 tarefas em aberto."));
    }

    @Test
    void chatSemMensagemRetorna400ComOCampo() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"%s"}
                                """.formatted(ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].reason").value("mensagem nao pode ser vazia"));

        verifyNoInteractions(service);
    }

    @Test
    void chatComMensagemAcimaDoLimiteRetorna400() throws Exception {
        String mensagem = "a".repeat(5001);

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"%s"}
                                """.formatted(mensagem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("message"))
                .andExpect(jsonPath("$.errors[0].reason").value("mensagem deve ter no maximo 5000 caracteres"));

        verifyNoInteractions(service);
    }

    @Test
    void chatComUuidInvalidoNoCorpoRetorna400() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"nao-e-uuid","message":"oi"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", containsString("requisicao-malformada")));

        verifyNoInteractions(service);
    }

    @Test
    void chatComConversaInexistenteRetorna404() throws Exception {
        when(service.chat(ID, "oi"))
                .thenThrow(ResourceNotFoundException.of("conversa", ID));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"%s","message":"oi"}
                                """.formatted(ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", containsString("nao-encontrado")))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    // --- GET /assistant/conversations (F10) ---

    @Test
    void conversationsRetorna200ComOsResumosDoMaisRecenteParaOMaisAntigo() throws Exception {
        when(service.listarConversas()).thenReturn(List.of(
                new ConversationSummary(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "Quais tarefas estao pendentes?", Instant.parse("2026-10-08T10:00:00Z")),
                new ConversationSummary(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "Planejar a semana", Instant.parse("2026-10-07T09:00:00Z"))));

        mockMvc.perform(get("/assistant/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$[0].title").value("Quais tarefas estao pendentes?"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-10-08T10:00:00Z"))
                .andExpect(jsonPath("$[1].title").value("Planejar a semana"));
    }

    @Test
    void conversationRetorna200ComAMensagensEmOrdem() throws Exception {
        ConversationMessage usuario = new ConversationMessage("user", "O que tenho para hoje?");
        ConversationMessage assistente = new ConversationMessage("assistant", "Voce tem uma tarefa vencida.");
        when(service.conversa(ID)).thenReturn(new ConversationDetail(
                ID, "O que tenho para hoje?", Instant.parse("2026-10-08T11:00:00Z"),
                List.of(usuario, assistente)));

        mockMvc.perform(get("/assistant/conversations/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.title").value("O que tenho para hoje?"))
                .andExpect(jsonPath("$.updatedAt").value("2026-10-08T11:00:00Z"))
                .andExpect(jsonPath("$.messages", hasSize(2)))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("O que tenho para hoje?"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.messages[1].content").value("Voce tem uma tarefa vencida."));
    }

    @Test
    void conversationInexistenteRetorna404() throws Exception {
        when(service.conversa(ID))
                .thenThrow(ResourceNotFoundException.of("conversa", ID));

        mockMvc.perform(get("/assistant/conversations/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", containsString("nao-encontrado")))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    // --- GET /assistant/conversations/{id}/messages (T-F06-10) ---

    @Test
    void messagesRetorna200ComIdRoleConteudoECreatedAtEmOrdem() throws Exception {
        when(service.mensagensDaConversa(ID)).thenReturn(List.of(
                new ChatMessageResponse(10L, "user", "O que tenho para hoje?",
                        Instant.parse("2026-10-08T10:00:00Z")),
                new ChatMessageResponse(11L, "assistant", "Voce tem uma tarefa vencida.",
                        Instant.parse("2026-10-08T10:00:05Z"))));

        mockMvc.perform(get("/assistant/conversations/{id}/messages", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("O que tenho para hoje?"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-08T10:00:00Z"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].role").value("assistant"))
                .andExpect(jsonPath("$[1].createdAt").value("2026-10-08T10:00:05Z"));
    }

    @Test
    void messagesDeConversaInexistenteRetorna404() throws Exception {
        when(service.mensagensDaConversa(ID))
                .thenThrow(ResourceNotFoundException.of("conversa", ID));

        mockMvc.perform(get("/assistant/conversations/{id}/messages", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", containsString("nao-encontrado")))
                .andExpect(jsonPath("$.title").value("Recurso nao encontrado"));
    }

    // --- falhas do LLM ---

    @Test
    void respostaInvalidaDaIaRetorna502ComOCodigoLlmInvalidResponse() throws Exception {
        when(service.chat(null, "oi"))
                .thenThrow(new InvalidLlmResponseException("assistente devolveu resposta vazia"));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"oi"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", containsString("resposta-llm-invalida")))
                .andExpect(jsonPath("$.title").value("Resposta invalida da IA"))
                .andExpect(jsonPath("$.code").value("LLM_INVALID_RESPONSE"))
                // erro de IA herda o envelope de erro comum (T-F02-05k)
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    @Test
    void falhaDeComunicacaoComAIARetorna502ComTypeProprio() throws Exception {
        when(service.chat(null, "oi"))
                .thenThrow(new LlmCommunicationException("falha de comunicacao com o LLM: timeout"));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"oi"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type", containsString("erro-de-comunicacao-com-llm")))
                .andExpect(jsonPath("$.title").value("Falha na comunicacao com a IA"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    @Test
    void iaIndisponivelRetorna503() throws Exception {
        when(service.chat(null, "oi"))
                .thenThrow(new LlmUnavailableException("LLM indisponivel: Connection refused"));

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"oi"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type", containsString("llm-indisponivel")))
                .andExpect(jsonPath("$.title").value("IA indisponivel"))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}