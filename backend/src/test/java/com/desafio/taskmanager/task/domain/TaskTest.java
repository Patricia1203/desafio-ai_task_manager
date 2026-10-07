package com.desafio.taskmanager.task.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.desafio.taskmanager.common.error.BusinessRuleException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TST-01: regras do dominio {@link Task} sem Spring, sem container e sem banco.
 *
 * <p><b>Por que um teste puro.</b> As mesmas regras hoje so sao exercitadas em
 * {@code TaskServiceTest}, que sobe o contexto inteiro com Postgres via
 * Testcontainers. Isso torna cada verificacao de regra cara e mistura duas
 * perguntas: "a regra vale?" e "o mapeamento JPA persiste?". Esta classe responde
 * so a primeira, em milissegundos. O custo e que ela nao pega regressao de
 * schema — para isso existe o {@code TaskRepositoryTest}.
 *
 * <p>Sem {@code @SpringBootTest}: nao ha contexto, nao ha proxy, nao ha
 * transacao. A entidade e criada direto pelo construtor de dominio.
 */
@DisplayName("Task — regras de dominio")
class TaskTest {

    private static final LocalDate PRAZO = LocalDate.of(2026, 12, 31);

    @Nested
    @DisplayName("criacao")
    class Criacao {

        @Test
        @DisplayName("nasce com id, TODO, MEDIUM e os dois timestamps iguais")
        void nasceCompleta() {
            Task task = new Task("Titulo", "Descricao", null, PRAZO, null);

            assertThat(task.getId()).isNotNull();
            assertThat(task.getStatus()).isEqualTo(TaskStatus.INITIAL);
            assertThat(task.getPriority()).isEqualTo(TaskPriority.DEFAULT);
            assertThat(task.getCreatedAt()).isNotNull();
            assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());
            assertThat(task.getDueDate()).isEqualTo(PRAZO);
            assertThat(task.isSubtask()).isFalse();
        }

        @Test
        @DisplayName("prioridade explicita e respeitada; o padrao so entra quando falta")
        void prioridadeExplicitaVence() {
            assertThat(new Task("T", null, TaskPriority.HIGH, null, null).getPriority())
                    .isEqualTo(TaskPriority.HIGH);
            assertThat(new Task("T", null, null, null, null).getPriority())
                    .isEqualTo(TaskPriority.DEFAULT);
        }

        @Test
        @DisplayName("o titulo e aparado nas pontas")
        void tituloEAparado() {
            assertThat(new Task("  Espacado  ", null, null, null, null).getTitle())
                    .isEqualTo("Espacado");
        }

        @Test
        @DisplayName("descricao e prazo aceitam nulo")
        void camposOpcionaisAceitamNulo() {
            Task task = new Task("T", null, null, null, null);

            assertThat(task.getDescription()).isNull();
            assertThat(task.getDueDate()).isNull();
        }

        @Test
        @DisplayName("titulo nulo, vazio ou so com espaco e recusado")
        void tituloInvalidoERecusado() {
            assertThatThrownBy(() -> new Task(null, null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("titulo nao pode ser vazio");

            assertThatThrownBy(() -> new Task("", null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class);

            assertThatThrownBy(() -> new Task("   ", null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("titulo acima de 200 caracteres e recusado")
        void tituloLongoERecusado() {
            String longo = "a".repeat(201);

            assertThatThrownBy(() -> new Task(longo, null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("200");
        }

        @Test
        @DisplayName("titulo de exatamente 200 caracteres e aceito")
        void tituloNoLimiteEAceito() {
            String limite = "a".repeat(200);

            assertThatCode(() -> new Task(limite, null, null, null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("transicao de status")
    class Transicao {

        @Test
        @DisplayName("TODO para IN_PROGRESS e DONE e permitido")
        void avancoNormal() {
            Task task = new Task("T", null, null, null, null);

            task.changeStatus(TaskStatus.IN_PROGRESS);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);

            task.changeStatus(TaskStatus.DONE);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        @DisplayName("DONE volta so para TODO")
        void concluidaReabreSomentePorAFazer() {
            Task task = new Task("T", null, null, null, null);
            task.changeStatus(TaskStatus.DONE);

            assertThatThrownBy(() -> task.changeStatus(TaskStatus.IN_PROGRESS))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("use TODO para reabrir");

            task.changeStatus(TaskStatus.TODO);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("voltar para o mesmo status nao mexe em nada e nao atualiza updatedAt")
        void repetirOMesmoStatusENoOp() {
            Task task = new Task("T", null, null, null, null);
            Instant original = task.getUpdatedAt();

            task.changeStatus(TaskStatus.TODO);

            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(task.getUpdatedAt()).isEqualTo(original);
        }

        @Test
        @DisplayName("status nulo e recusado")
        void statusNuloERecusado() {
            Task task = new Task("T", null, null, null, null);

            assertThatThrownBy(() -> task.changeStatus(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("toda transicao aceita mexe em updatedAt")
        void transicaoMexeEmUpdatedAt() throws InterruptedException {
            Task task = new Task("T", null, null, null, null);
            Instant antes = task.getUpdatedAt();

            Thread.sleep(2);
            task.changeStatus(TaskStatus.IN_PROGRESS);

            assertThat(task.getUpdatedAt()).isAfter(antes);
            assertThat(task.getCreatedAt()).isBeforeOrEqualTo(antes);
        }
    }

    @Nested
    @DisplayName("edicao de conteudo")
    class Edicao {

        @Test
        @DisplayName("altera os campos de conteudo sem mexer no status")
        void edicaoNaoMexeEmStatusNemCreatedAt() {
            Task task = new Task("Antigo", "Descricao antiga", TaskPriority.LOW, PRAZO, null);
            task.changeStatus(TaskStatus.IN_PROGRESS);
            Instant criadoEm = task.getCreatedAt();

            task.updateContent("Novo", "Descricao nova", TaskPriority.HIGH,
                    LocalDate.of(2027, 1, 15));

            assertThat(task.getTitle()).isEqualTo("Novo");
            assertThat(task.getDescription()).isEqualTo("Descricao nova");
            assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2027, 1, 15));
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(task.getCreatedAt()).isEqualTo(criadoEm);
        }

        @Test
        @DisplayName("prioridade nula na edicao volta ao padrao")
        void prioridadeNulaNaEdicaoVoltaAoPadrao() {
            Task task = new Task("T", null, TaskPriority.HIGH, null, null);

            task.updateContent("T", null, null, null);

            assertThat(task.getPriority()).isEqualTo(TaskPriority.DEFAULT);
        }

        @Test
        @DisplayName("titulo invalido e recusado na edicao e o anterior permanece")
        void edicaoComTituloInvalidoNaoCorrompeOEstado() {
            Task task = new Task("Bom", null, null, null, null);

            assertThatThrownBy(() -> task.updateContent("  ", null, null, null))
                    .isInstanceOf(BusinessRuleException.class);

            assertThat(task.getTitle()).isEqualTo("Bom");
        }

        @Test
        @DisplayName("edicao atualiza updatedAt")
        void edicaoMexeEmUpdatedAt() throws InterruptedException {
            Task task = new Task("T", null, null, null, null);
            Instant antes = task.getUpdatedAt();

            Thread.sleep(2);
            task.updateContent("Outro", null, null, null);

            assertThat(task.getUpdatedAt()).isAfter(antes);
        }
    }

    @Nested
    @DisplayName("subtarefas")
    class Subtarefas {

        @Test
        @DisplayName("com pai, a tarefa se reconhece como subtarefa")
        void comPaiESubtarefa() {
            Task pai = new Task("Pai", null, null, null, null);
            Task filha = new Task("Filha", null, null, null, pai);

            assertThat(filha.isSubtask()).isTrue();
            assertThat(pai.isSubtask()).isFalse();
            assertThat(filha.getParent()).isSameAs(pai);
        }

        @Test
        @DisplayName("o pai nunca tem o mesmo id do filho")
        void paiEFilhoTemIdsDiferentes() {
            Task pai = new Task("Pai", null, null, null, null);
            Task filho = new Task("Filho", null, null, null, pai);

            assertThat(pai.getId()).isNotEqualTo(filho.getId());
            assertThat(filho.getParent().getId()).isEqualTo(pai.getId());
        }

        /**
         * A guarda de pai-de-si-mesmo no construtor so dispara com dois ids
         * iguais, e o id e um UUID gerado dentro da propria entidade: pela API
         * publica ela e inalcancavel. Este teste registra o comportamento real
         * em vez de fingir que a guarda e testavel — a defesa que vale aqui e o
         * {@code CHECK (parent_id IS NULL OR parent_id <> id)} de V2, coberto
         * pelo {@code TaskRepositoryTest}.
         */
        @Test
        @DisplayName("a guarda de auto-referencia e inalcancavel pela API publica")
        void guardaDeAutoReferenciaNaoDisparaPelaApiPublica() {
            Task pai = new Task("Pai", null, null, null, null);

            assertThatCode(() -> new Task("Filho", null, null, null, pai))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("igualdade")
    class Igualdade {

        @Test
        @DisplayName("duas tarefas com o mesmo id sao iguais")
        void mesmoIdImplicaIgualdade() {
            Task a = new Task("A", null, null, null, null);
            Task b = new Task("B", null, null, null, null);

            assertThat(a).isNotEqualTo(b);
            assertThat(a).isEqualTo(a);
            assertThat(a.hashCode()).isEqualTo(a.hashCode());
            assertThat(a).isNotEqualTo(null);
            assertThat(a).isNotEqualTo("nao e uma Task");
        }
    }
}
