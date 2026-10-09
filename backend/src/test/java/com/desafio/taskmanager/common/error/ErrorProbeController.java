package com.desafio.taskmanager.common.error;

import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Controller descartavel usado só nos testes do handler de erro. Nao contem
 * regra de negocio: existe para provocar cada tipo de excecao via HTTP.
 */
@RestController
@RequestMapping("/__test")
class ErrorProbeController {

    record ProbeRequest(@NotBlank(message = "campo obrigatorio") String field) {
    }

    @PostMapping("/validacao")
    ProbeRequest validacao(@jakarta.validation.Valid @RequestBody ProbeRequest request) {
        return request;
    }

    @GetMapping("/inexistente")
    void inexistente() {
        throw ResourceNotFoundException.of("Tarefa", 42L);
    }

    @GetMapping("/regra")
    void regra() {
        throw new BusinessRuleException("status nao pode voltar de DONE para IN_PROGRESS");
    }

    @GetMapping("/inesperado")
    void inesperado() {
        throw new IllegalStateException("detalhe interno que nao pode vazar: jdbc:postgresql://host/db");
    }

    @GetMapping("/malformado")
    ProbeRequest malformado(@RequestBody ProbeRequest request) {
        return request;
    }

    @GetMapping("/metodo")
    void metodo() throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException("POST");
    }

    @PostMapping("/tipo")
    void tipo() throws HttpMediaTypeNotSupportedException {
        throw new HttpMediaTypeNotSupportedException("application/xml");
    }

    @PostMapping("/upload-grande")
    void uploadGrande() {
        throw new MaxUploadSizeExceededException(5 * 1024 * 1024L,
                new IllegalStateException("the request was rejected because its size exceeds the configured maximum"));
    }

    /**
     * Simula a falha de persistencia de ERR-06. Nao toca em banco: a excecao
     * em si e o que o handler deve mapear.
     */
    @GetMapping("/banco")
    void banco() {
        throw new DataAccessResourceFailureException(
                "conexao recusada em jdbc:postgresql://host:5432/taskmanager");
    }

    /** ERR-03: falha de comunicacao com o LLM vira 502 com type proprio. */
    @GetMapping("/llm-comunicacao")
    void llmComunicacao() {
        throw new LlmCommunicationException("falha de comunicacao com o LLM: broken pipe");
    }

    /** ERR-05: LLM indisponivel vira 503. */
    @GetMapping("/llm-indisponivel")
    void llmIndisponivel() {
        throw new LlmUnavailableException("LLM indisponivel: Connection refused");
    }

    /** ERR-04: resposta do LLM fora do contrato vira 502 com code LLM_INVALID_RESPONSE. */
    @GetMapping("/llm-invalida")
    void llmInvalida() {
        throw new InvalidLlmResponseException(
                "resposta da IA invalida apos 2 tentativas: json fora do contrato");
    }
}