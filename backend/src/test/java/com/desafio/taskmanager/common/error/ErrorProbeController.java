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
        throw new BusinessRuleException("status nao pode voltar de CONCLUIDA para EM_ANDAMENTO");
    }

    @GetMapping("/inesperado")
    void inesperado() {
        throw new IllegalStateException("detalhe interno que nao pode vazar: jdbc:postgresql://host/db");
    }

    @GetMapping("/malformado")
    ProbeRequest malformado(@RequestBody ProbeRequest request) {
        return request;
    }

    /** Lanca 405 de proposito, para provar que o handler nao devolve 500. */
    @GetMapping("/metodo")
    void metodo() throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException("POST");
    }

    /** Lanca 415 de proposito, para provar que o handler nao devolve 500. */
    @PostMapping("/tipo")
    void tipo() throws HttpMediaTypeNotSupportedException {
        throw new HttpMediaTypeNotSupportedException("application/xml");
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
}