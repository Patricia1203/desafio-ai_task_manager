package com.desafio.taskmanager.area.api;

import java.net.URI;
import java.util.UUID;

import com.desafio.taskmanager.area.api.dto.WorkAreaResponse;
import com.desafio.taskmanager.area.application.WorkAreaService;
import com.desafio.taskmanager.area.domain.WorkArea;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * API de areas de trabalho (F14).
 *
 * <p>Multipart: {@code title} e o nome (obrigatorio) e {@code image} a foto
 * opcional. No PUT titulo, foto e remocao sao independentes — o cliente manda
 * so o que quer alterar. O content type do arquivo vira o {@code imageType} da
 * area (regra {@code image/*} no dominio).
 *
 * <p>Sem {@code @Validated} no controller (mesma regra documentada no
 * TaskController): a validacao de borda do titulo que nao cabe em anotacao de
 * parametro multipart acontece no dominio ({@code WorkArea#requireTitle}), que
 * responde 422 com mensagem pt-BR.
 */
@RestController
@RequestMapping("/areas")
public class WorkAreaController {

    private final WorkAreaService service;

    public WorkAreaController(WorkAreaService service) {
        this.service = service;
    }

    /** Lista as areas em ordem alfabetica, sem os bytes das fotos. */
    @GetMapping
    public java.util.List<WorkAreaResponse> list() {
        return service.list().stream().map(WorkAreaResponse::of).toList();
    }

    /** Cria a area; 201 com Location. A foto e opcional. */
    @PostMapping
    public ResponseEntity<WorkAreaResponse> create(
            @RequestParam String title,
            @RequestPart(required = false) MultipartFile image) {
        WorkArea created = service.create(title, bytesDe(image), contentTypeDe(image));
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(WorkAreaResponse.of(created));
    }

    /**
     * Edicao parcial: sem titulo mantem o atual; com imagem troca a foto;
     * {@code removeImage=true} sem upload novo limpa a foto.
     */
    @PutMapping("/{id}")
    public WorkAreaResponse update(
            @PathVariable UUID id,
            @RequestParam(required = false) String title,
            @RequestPart(required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage) {
        return WorkAreaResponse.of(service.update(id, title, bytesDe(image), contentTypeDe(image), removeImage));
    }

    /** Exclui a area; as tarefas dela voltam a area nula (nao apagam). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Foto da area: bytes com o content type gravado; 404 se a area ou a foto
     * nao existirem. Cache publico de 1 dia — a foto so muda quando o cliente
     * reenvia um PUT.
     */
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable UUID id) {
        WorkAreaService.WorkAreaImage image = service.findImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(image.bytes());
    }

    private static byte[] bytesDe(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        try {
            return image.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("falha ao ler o arquivo de imagem", e);
        }
    }

    private static String contentTypeDe(MultipartFile image) {
        return image == null || image.isEmpty() ? null : image.getContentType();
    }
}