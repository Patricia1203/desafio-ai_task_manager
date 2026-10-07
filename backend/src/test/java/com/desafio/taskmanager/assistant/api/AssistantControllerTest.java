package com.desafio.taskmanager.assistant.api;

import java.util.UUID;

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
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
                                {"mensagem":"Quais tarefas estao pendentes?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(ID.toString()))
                .andExpect(jsonPath("$.resposta").value("Temos 2 tarefas em aberto."));
    }

    @Test
    void chatSemMensagemRetorna400ComOCampo() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"%s"}
                                """.formatted(ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("mensagem"))
                .andExpect(jsonPath("$.errors[0].reason").value("mensagem nao pode ser vazia"));

        verifyNoInteractions(service);
    }

    @Test
    void chatComMensagemAcimaDoLimiteRetorna400() throws Exception {
        String mensagem = "a".repeat(5001);

        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mensagem":"%s"}
                                """.formatted(mensagem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("mensagem"))
                .andExpect(jsonPath("$.errors[0].reason").value("mensagem deve ter no maximo 5000 caracteres"));

        verifyNoInteractions(service);
    }

    @Test
    void chatComUuidInvalidoNoCorpoRetorna400() throws Exception {
        mockMvc.perform(post("/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"nao-e-uuid","mensagem":"oi"}
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
                                {"conversationId":"%s","mensagem":"oi"}
                                """.formatted(ID)))
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
                                {"mensagem":"oi"}
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
                                {"mensagem":"oi"}
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
                                {"mensagem":"oi"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type", containsString("llm-indisponivel")))
                .andExpect(jsonPath("$.title").value("IA indisponivel"))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[0-9a-f]{16}")))
                .andExpect(jsonPath("$.timestamp").value(
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }
}