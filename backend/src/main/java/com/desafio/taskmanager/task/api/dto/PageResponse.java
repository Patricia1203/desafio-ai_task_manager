package com.desafio.taskmanager.task.api.dto;

import java.util.List;

/**
 * Envelope de lista paginada. Record generico para que o mesmo formato sirva
 * para tarefas, mensagens do assistente e qualquer outra colecao paginada.
 *
 * <p>Os nomes dos componentes sao os campos do JSON (contrato em ingles desde
 * a revisao F06).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean first,
        boolean last) {

    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}