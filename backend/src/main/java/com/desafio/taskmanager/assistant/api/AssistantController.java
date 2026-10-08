package com.desafio.taskmanager.assistant.api;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.assistant.api.dto.ChatMessageResponse;
import com.desafio.taskmanager.assistant.api.dto.ChatRequest;
import com.desafio.taskmanager.assistant.api.dto.ChatResponse;
import com.desafio.taskmanager.assistant.api.dto.ConversationDetail;
import com.desafio.taskmanager.assistant.api.dto.ConversationSummary;
import com.desafio.taskmanager.assistant.application.AssistantService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API do assistente (US-030): POST /assistant/chat (publica em
 * {@code /api/assistant/chat} por causa do {@code context-path}).
 *
 * <p>O controller valida a borda ({@link ChatRequest}) e delega ao
 * {@link AssistantService}. Erros nao sao tratados aqui:
 * {@code ResourceNotFoundException} e as violacoes de Bean Validation viram
 * ProblemDetail pelo {@code GlobalExceptionHandler}; falha do LLM vira
 * 502/503 conforme ERR-03/ERR-04/ERR-05.
 *
 * <p>Sem {@code @Validated} de proposito (mesmo motivo do TaskController): a
 * validacao nativa do Spring nos parametros ja cai no handler global como 400.
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final AssistantService service;

    public AssistantController(AssistantService service) {
        this.service = service;
    }

    /** RF-15/RF-16. 200 com a conversa e a resposta; 404 se o conversationId nao existe. */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        AssistantService.RespostaChat resultado = service.chat(request.conversationId(), request.message());
        return new ChatResponse(resultado.conversationId(), resultado.resposta());
    }

    /** F10. 200 com o historico de conversas (resumo), da mais recente para a mais antiga. */
    @GetMapping("/conversations")
    public List<ConversationSummary> conversations() {
        return service.listarConversas();
    }

    /** F10. 200 com a conversa e as mensagens; 404 se o conversationId nao existe. */
    @GetMapping("/conversations/{id}")
    public ConversationDetail conversation(@PathVariable UUID id) {
        return service.conversa(id);
    }

    /** T-F06-10. 200 com as mensagens da conversa em ordem cronologica; 404 se o conversationId nao existe. */
    @GetMapping("/conversations/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable UUID id) {
        return service.mensagensDaConversa(id);
    }
}