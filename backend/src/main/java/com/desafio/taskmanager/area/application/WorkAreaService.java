package com.desafio.taskmanager.area.application;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaRepository;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio de areas de trabalho (F14).
 *
 * <p>O service orquestra; as invariantes (titulo obrigatorio, foto so com
 * content type {@code image/*}) ficam em {@link WorkArea}. Aqui ficam a
 * existencia (404), a ordem da listagem e o comportamento do {@code PUT}
 * parcial (titulo/ foto/ remocao independentes).
 */
@Service
@Transactional(readOnly = true)
public class WorkAreaService {

    private final WorkAreaRepository repository;

    public WorkAreaService(WorkAreaRepository repository) {
        this.repository = repository;
    }

    /** Lista as areas em ordem alfabetica (sem os bytes das fotos). */
    public List<WorkArea> list() {
        return repository.findAllByOrderByTitleAsc();
    }

    /** Cria uma area; a foto e opcional. */
    @Transactional
    public WorkArea create(String title, byte[] image, String imageType) {
        WorkArea area = new WorkArea(title);
        if (image != null) {
            area.setImage(image, imageType);
        }
        return repository.save(area);
    }

    /**
     * Edicao parcial por multipart: sem titulo mantem o atual; com bytes, troca
     * a foto; {@code removeImage} limpa a foto (ignorado se um upload novo for
     * enviado junto — o upload vale, a remocao nao dá overwrite).
     */
    @Transactional
    public WorkArea update(UUID id, String novoTitulo, byte[] novaImagem, String novoContentType,
            boolean removeImage) {
        WorkArea area = getOrThrow(id);
        if (novoTitulo != null) {
            area.setTitle(novoTitulo);
        }
        if (novaImagem != null) {
            area.setImage(novaImagem, novoContentType);
        } else if (removeImage) {
            area.clearImage();
        }
        return area;
    }

    /** Exclui a area; as tarefas dela voltam a area nula (FK SetNull). */
    @Transactional
    public void delete(UUID id) {
        repository.delete(getOrThrow(id));
    }

    /** Bytes + content type da foto; 404 se a area ou a foto nao existirem. */
    public WorkAreaImage findImage(UUID id) {
        WorkArea area = getOrThrow(id);
        if (!area.hasImage()) {
            throw ResourceNotFoundException.of("imagem da area de trabalho", id);
        }
        return new WorkAreaImage(area.getImage(), area.getImageType());
    }

    private WorkArea getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("area de trabalho", id));
    }

    /** Bytes + content type devolvidos pelo endpoint de imagem. */
    public record WorkAreaImage(byte[] bytes, String contentType) {
    }
}