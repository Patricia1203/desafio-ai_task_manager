package com.desafio.taskmanager.area.application;

import java.util.List;
import java.util.UUID;

import com.desafio.taskmanager.area.domain.WorkArea;
import com.desafio.taskmanager.area.infra.WorkAreaRepository;
import com.desafio.taskmanager.common.error.BusinessRuleException;
import com.desafio.taskmanager.common.error.ResourceNotFoundException;
import com.desafio.taskmanager.support.PostgresIntegrationTest;
import com.desafio.taskmanager.task.application.TaskService;
import com.desafio.taskmanager.task.application.dto.TaskCommand;
import com.desafio.taskmanager.task.infra.TaskRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TST-04: CRUD e regras de area (F14) contra Postgres real. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class WorkAreaServiceTest extends PostgresIntegrationTest {

    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G' };

    @Autowired
    private WorkAreaService service;

    @Autowired
    private WorkAreaRepository repository;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void clean() {
        taskRepository.deleteAll();
        repository.deleteAll();
    }

    // --- criar (R1) ---

    @Test
    void criarSemImagemGravaOTituloAparado() {
        WorkArea area = service.create("  Pessoal  ", null, null);

        assertThat(area.getTitle()).isEqualTo("Pessoal");
        assertThat(area.hasImage()).isFalse();
        assertThat(repository.findById(area.getId())).isPresent();
    }

    @Test
    void criarComImagemGravaBytesEContentType() {
        WorkArea area = service.create("Projeto", PNG, "image/png");

        assertThat(area.hasImage()).isTrue();
        assertThat(area.getImage()).isEqualTo(PNG);
        assertThat(area.getImageType()).isEqualTo("image/png");
    }

    @Test
    void criarComTituloEmBrancoEhRecusado() {
        assertThatThrownBy(() -> service.create("   ", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("titulo");
    }

    @Test
    void criarComTituloLongoEhRecusado() {
        assertThatThrownBy(() -> service.create("a".repeat(101), null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("100");
    }

    @Test
    void criarComContentTypeNaoImagemEhRecusado() {
        assertThatThrownBy(() -> service.create("Projeto", PNG, "text/plain"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("formato");
    }

    // --- ler (R1) ---

    @Test
    void listarOrdenaPorTitulo() {
        repository.save(new WorkArea("Zeta"));
        repository.save(new WorkArea("Alfa"));

        List<WorkArea> areas = service.list();

        assertThat(areas).extracting(WorkArea::getTitle).containsExactly("Alfa", "Zeta");
    }

    @Test
    void listarPorTituloIgnoraCaixaECoringaLiteral() {
        repository.save(new WorkArea("Relatorio trimestral"));
        repository.save(new WorkArea("100% feito"));
        repository.save(new WorkArea("100 e feito"));

        assertThat(service.list("RELAT"))
                .extracting(WorkArea::getTitle)
                .containsExactly("Relatorio trimestral");
        assertThat(service.list("100%"))
                .extracting(WorkArea::getTitle)
                .containsExactly("100% feito");
    }

    @Test
    void listarPorTituloEmBrancoDevolveTudo() {
        repository.save(new WorkArea("Alfa"));

        assertThat(service.list("   ")).extracting(WorkArea::getTitle).containsExactly("Alfa");
    }

    @Test
    void primeiroQuadroVinculaAsTarefasOrfasAEle() {
        UUID orfa = taskService.create(
                new TaskCommand("Existente", null, null, null, null, null, null)).getId();

        WorkArea primeiro = service.create("Primeiro", null, null);
        WorkArea segundo = service.create("Segundo", null, null);

        assertThat(taskRepository.findById(orfa).orElseThrow().getArea().getId())
                .isEqualTo(primeiro.getId());
        assertThat(primeiro.getId()).isNotEqualTo(segundo.getId());
    }

    @Test
    void buscarImagemDeAreaSemFotoVira404() {
        UUID id = service.create("Sem foto", null, null).getId();

        assertThatThrownBy(() -> service.findImage(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarImagemDeIdInexistenteVira404() {
        assertThatThrownBy(() -> service.findImage(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- editar (R1) ---

    @Test
    void editarSoTituloMantemAFoto() {
        UUID id = service.create("Pessoal", PNG, "image/png").getId();

        WorkArea atualizada = service.update(id, "Minhas tarefas", null, null, false);

        assertThat(atualizada.getTitle()).isEqualTo("Minhas tarefas");
        assertThat(atualizada.getImage()).isEqualTo(PNG);
    }

    @Test
    void editarTrocaAFoto() {
        UUID id = service.create("Pessoal", PNG, "image/png").getId();
        byte[] nova = { 1, 2, 3 };

        WorkArea atualizada = service.update(id, null, nova, "image/jpeg", true);

        assertThat(atualizada.getImage()).isEqualTo(nova);
        assertThat(atualizada.getImageType()).isEqualTo("image/jpeg");
    }

    @Test
    void editarComRemoveImageLimpaAFoto() {
        UUID id = service.create("Pessoal", PNG, "image/png").getId();

        WorkArea atualizada = service.update(id, null, null, null, true);

        assertThat(atualizada.hasImage()).isFalse();
        assertThat(atualizada.getImageType()).isNull();
    }

    @Test
    void editarIdInexistenteVira404() {
        assertThatThrownBy(() -> service.update(UUID.randomUUID(), "X", null, null, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- excluir (R1) ---

    @Test
    void excluirRemoveADoBanco() {
        UUID id = service.create("Descartavel", null, null).getId();

        service.delete(id);

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void excluirIdInexistenteVira404() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}