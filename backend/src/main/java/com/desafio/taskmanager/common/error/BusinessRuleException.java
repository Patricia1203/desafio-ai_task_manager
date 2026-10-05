package com.desafio.taskmanager.common.error;

/**
 * Regra de negocio violada. Mapeada para HTTP 422.
 * Exemplos: status nao pode mudar de DONE para IN_PROGRESS, subtarefa invalida.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}