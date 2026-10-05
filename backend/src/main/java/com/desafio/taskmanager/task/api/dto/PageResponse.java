package com.desafio.taskmanager.task.api.dto;

import java.util.List;

/**
 * Envelope de lista paginada. Record generico para que o mesmo formato sirva
 * para tarefas, mensagens do assistente e qualquer outra colecao paginada.
 *
 * @param content      itens da pagina atual
 * @param page         indice da pagina, começando em zero
 * @param size         tamanho da pagina
 * @param totalItems   total de itens no resultado, ignorando a paginacao
 * @param totalPages   total de paginas
 * @param first        se esta e a primeira pagina
 * @param last         se esta e a ultima pagina
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