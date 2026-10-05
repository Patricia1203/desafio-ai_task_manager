package com.desafio.taskmanager.task.api.dto;

import java.util.List;

/**
 * Envelope de lista paginada. Record generico para que o mesmo formato sirva
 * para tarefas, mensagens do assistente e qualquer outra colecao paginada.
 *
 * <p>Os nomes dos componentes sao os campos do JSON (decisao do usuario,
 * contrato em portugues).
 *
 * @param conteudo     itens da pagina atual
 * @param pagina       indice da pagina, comecando em zero
 * @param tamanho      tamanho da pagina
 * @param totalItens   total de itens no resultado, ignorando a paginacao
 * @param totalPaginas total de paginas
 * @param primeira     se esta e a primeira pagina
 * @param ultima       se esta e a ultima pagina
 */
public record PageResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas,
        boolean primeira,
        boolean ultima) {

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
