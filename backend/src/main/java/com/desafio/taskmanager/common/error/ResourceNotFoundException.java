package com.desafio.taskmanager.common.error;

/**
 * Recurso solicitado nao existe. Mapeada para HTTP 404.
 * Usada por tasks, IA e assistente.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String recurso, Object id) {
        return new ResourceNotFoundException(recurso + " nao encontrado: " + id);
    }
}