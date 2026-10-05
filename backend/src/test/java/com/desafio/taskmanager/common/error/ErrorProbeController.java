package com.desafio.taskmanager.common.error;

import jakarta.validation.constraints.NotBlank;
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
}