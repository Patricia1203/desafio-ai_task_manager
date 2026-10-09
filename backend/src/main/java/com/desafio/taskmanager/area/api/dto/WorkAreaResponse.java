package com.desafio.taskmanager.area.api.dto;

import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaSummary;

/**
 * Area exposta na API (F14) — sempre sem os bytes da foto. {@code imageType}
 * informa se ha foto ({@code image/*}) e o cliente monta o endereco
 * {@code GET /areas/{id}/image} por conta propria.
 */
public record WorkAreaResponse(UUID id, String title, String imageType) {

    public static WorkAreaResponse of(WorkArea area) {
        return new WorkAreaResponse(area.getId(), area.getTitle(), area.getImageType());
    }

    /** Mesmo resumo a partir da projecao de leitura usada na listagem (F15). */
    public static WorkAreaResponse of(WorkAreaSummary area) {
        return new WorkAreaResponse(area.id(), area.title(), area.imageType());
    }
}
