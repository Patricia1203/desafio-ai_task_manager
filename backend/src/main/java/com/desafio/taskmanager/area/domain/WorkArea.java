package com.desafio.taskmanager.area.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
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
 * impossiveis. A escrita aceita apenas a whitelist {@code png/jpeg/webp/gif} e
 * confere os magic bytes — o SVG fica de fora de proposito (pode carregar script
 * e virar XSS armazenado quando servido inline na mesma origem).
 */
@Entity
@Table(name = "work_areas")
public class WorkArea {

    private static final int MAX_TITLE_LENGTH = 100;

    /** Teto do upload de foto, espelhado em {@code spring.servlet.multipart.max-file-size}. */
    public static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private static final String FORMATO_INVALIDO =
            "formato de imagem nao suportado; use image/png, image/jpeg, image/webp ou image/gif";

    /** Whitelist explicita: SVG e afins ficam de fora (XSS armazenado). */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif");

    private static final byte[] PNG_MAGIC = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] GIF87A_MAGIC = { 'G', 'I', 'F', '8', '7', 'a' };
    private static final byte[] GIF89A_MAGIC = { 'G', 'I', 'F', '8', '9', 'a' };

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
     * a foto (equivale a {@link #clearImage()}). Sao recusados com 422: content
     * type fora da whitelist, bytes cujo magic number nao bate com o tipo
     * declarado, e arquivo acima de {@link #MAX_IMAGE_SIZE}.
     */
    public void setImage(byte[] image, String contentType) {
        if (image == null) {
            clearImage();
            return;
        }
        if (image.length > MAX_IMAGE_SIZE) {
            throw new BusinessRuleException("imagem excede o tamanho maximo de 5MB");
        }
        String declarado = contentType == null ? null : contentType.toLowerCase(Locale.ROOT).trim();
        String detectado = detectImageType(image);
        if (declarado == null || !ALLOWED_CONTENT_TYPES.contains(declarado)
                || !declarado.equals(detectado)) {
            throw new BusinessRuleException(FORMATO_INVALIDO);
        }
        this.image = image;
        this.imageType = declarado;
    }

    public void clearImage() {
        this.image = null;
        this.imageType = null;
    }

    public boolean hasImage() {
        return image != null;
    }

    /** Detecta o tipo real pelos magic bytes; {@code null} se nao for um formato aceito. */
    private static String detectImageType(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC)) {
            return "image/png";
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        if (startsWith(bytes, GIF87A_MAGIC) || startsWith(bytes, GIF89A_MAGIC)) {
            return "image/gif";
        }
        if (isWebp(bytes)) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
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
