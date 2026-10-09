package com.desafio.taskmanager.area.infra;

import java.util.UUID;

/**
 * Projecao de leitura do resumo da area (F15): so as colunas id/title/image_type,
 * nunca a coluna {@code bytea image}.
 *
 * <p>Sem a projecao, {@code findAll()} materializava a entidade inteira e cada
 * quadro trazia ate {@link com.desafio.taskmanager.area.domain.WorkArea#MAX_IMAGE_SIZE}
 * bytes so para montar {@code {id, title, imageType}} na listagem.
 */
public record WorkAreaSummary(UUID id, String title, String imageType) {

    /** Ha foto quando o content type foi gravado junto (imagem e tipo andam juntos). */
    public boolean hasImage() {
        return imageType != null;
    }
}
