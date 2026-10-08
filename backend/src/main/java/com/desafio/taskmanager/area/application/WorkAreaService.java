package com.desafio.taskmanager.area.application;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaRepository;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio de areas de trabalho (F14).
 *
 * <p>O service orquestra; as invariantes (titulo obrigatorio, foto so com
 * content type {@code image/*}) ficam em {@link WorkArea}. Aqui ficam a
 * existencia (404), a ordem da listagem, a busca por titulo e o comportamento
 * do {@code PUT} parcial (titulo/ foto/ remocao independentes).
 */
@Service
@Transactional(readOnly = true)
public class WorkAreaService {

    private final WorkAreaRepository repository;

    private final TaskRepository taskRepository;

    public WorkAreaService(WorkAreaRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    /** Lista as areas em ordem alfabetica (sem os bytes das fotos). */
    public List<WorkArea> list() {
        return repository.findAllByOrderByTitleAsc();
    }

    /**
     * Lista filtrando por titulo (contem parcial, sem diferenciar caixa e sem
     * deixar {@code %}/{@code _} do termo virarem coringa). Termo em branco
     * devolve a lista completa, igual a {@link #list()}.
     */
    public List<WorkArea> list(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return list();
        }
        return repository.findAll(
                tituloContem(titulo.trim()),
                org.springframework.data.domain.Sort.by("title").ascending());
    }

    /**
     * Cria uma area; a foto e opcional. Quando e o <b>primeiro</b> quadro, as
     * tarefas que ainda nao tem quadro (as que existiam antes da F14) sao
     * vinculadas a ele, no mesmo commit — pedido do usuario.
     */
    @Transactional
    public WorkArea create(String title, byte[] image, String imageType) {
        boolean primeiroQuadro = repository.count() == 0;
        WorkArea area = new WorkArea(title);
        if (image != null) {
            area.setImage(image, imageType);
        }
        area = repository.save(area);
        if (primeiroQuadro) {
            taskRepository.assignAreaToOrphans(area);
        }
        return area;
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

    /** Contem parcial, sem diferenciar caixa e sem deixar %/_ do termo virarem coringa. */
    private static Specification<WorkArea> tituloContem(String termo) {
        String escapado = termo
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String pattern = "%" + escapado.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    /** Bytes + content type devolvidos pelo endpoint de imagem. */
    public record WorkAreaImage(byte[] bytes, String contentType) {
    }
}