package com.desafio.taskmanager.area.domain;

import java.time.Instant;
import java.util.UUID;

import com.desafio.taskmanager.common.error.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Area de trabalho (F14): agrupa tarefas num "mural" com nome e foto opcional.
 * Entidade JPA, nunca exposta direto na API — o {@code WorkAreaMapper} converte
 * para o peso leve {@code WorkAreaResponse} (sem os bytes da imagem).
 *
 * <p>A foto vive no banco (BYTEA) ao lado do content type ({@code imageType});
 * as duas regras valem juntas: imagem sem tipo ou tipo sem imagem sao estados
 * impossiveis. A escrita aceita apenas content type {@code image/*}.
 */
@Entity
@Table(name = "work_areas")
public class WorkArea {

    private static final int MAX_TITLE_LENGTH = 100;

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_type", length = 50)
    private String imageType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Construtor para o JPA. Não use: toda criação passa pela factory. */
    protected WorkArea() {
    }

    public WorkArea(String title) {
        this.id = UUID.randomUUID();
        this.title = requireTitle(title);
        this.createdAt = Instant.now();
    }

    /** Título pode ser atualizado sozinho (PUT sem imagem). */
    public void setTitle(String title) {
        this.title = requireTitle(title);
    }

    /**
     * Troca a foto, mantendo os bytes e o content type coerentes. `null` limpa
     * a foto (equivale a {@link #clearImage()}). Content type fora de
     * {@code image/*} e recusado com 422.
     */
    public void setImage(byte[] image, String contentType) {
        if (image == null) {
            clearImage();
            return;
        }
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException("formato de imagem nao suportado; use image/png, image/jpeg...");
        }
        this.image = image;
        this.imageType = contentType;
    }

    public void clearImage() {
        this.image = null;
        this.imageType = null;
    }

    public boolean hasImage() {
        return image != null;
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessRuleException("o titulo da area de trabalho e obrigatorio");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleException("o titulo da area de trabalho deve ter no maximo "
                    + MAX_TITLE_LENGTH + " caracteres");
        }
        return title.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public byte[] getImage() {
        return image;
    }

    public String getImageType() {
        return imageType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WorkArea area) || id == null) {
            return false;
        }
        return id.equals(area.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "WorkArea{id=" + id + ", title='" + title + "'}";
    }
}