package com.desafio.taskmanager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fronteira do provedor de IA (RNF-14): se {@code org.springframework.ai}
 * aparecer em domínio, serviço ou porta, a dependência do provedor vaza para
 * dentro do núcleo e "trocar provedor só em ai.adapter + config" vira mentira.
 * O acoplamento certo é falhar aqui, na build, e não na primeira troca.
 *
 * <p>Varre os fontes, não o classpath: o que se quer proibir é a intenção no
 * código que o time edita, inclusive uso fully-qualified sem import.
 */
@DisplayName("camadas de negocio nao conhecem o Spring AI")
class LayerDependenciesTest {

    private static final String PROIBIDO = "org.springframework.ai";

    private static final List<String> PACOTES_PROTEGIDOS = List.of(
            "com/desafio/taskmanager/task/domain",
            "com/desafio/taskmanager/task/application",
            "com/desafio/taskmanager/ai/port",
            "com/desafio/taskmanager/assistant/port",
            "com/desafio/taskmanager/assistant/application");

    @Test
    @DisplayName("task.domain, task.application e as portas/servicos de IA nao citam org.springframework.ai")
    void nenhumaCamadaDeNegocioImportaSpringAi() throws IOException {
        Path raiz = fonteJava();
        List<String> violacoes = new ArrayList<>();

        for (String pacote : PACOTES_PROTEGIDOS) {
            Path dir = raiz.resolve(pacote);
            assertThat(Files.isDirectory(dir)).as("diretorio %s existe", pacote).isTrue();
            try (Stream<Path> arquivos = Files.walk(dir)) {
                for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
                    if (Files.readString(arquivo, StandardCharsets.UTF_8).contains(PROIBIDO)) {
                        violacoes.add(raiz.relativize(arquivo).toString());
                    }
                }
            }
        }

        assertThat(violacoes)
                .as("arquivos citando %s onde o provedor deve ser invisivel", PROIBIDO)
                .isEmpty();
    }

    /**
     * Sobe até achar src/main/java: o surefire roda com working dir em
     * backend/, mas a IDE pode rodar da raiz do repositório.
     */
    private Path fonteJava() {
        Path atual = Path.of("").toAbsolutePath();
        while (atual != null && !Files.isDirectory(atual.resolve("src/main/java/com/desafio"))) {
            atual = atual.getParent();
        }
        assertThat(atual)
                .as("src/main/java encontrado a partir de %s", Path.of("").toAbsolutePath())
                .isNotNull();
        return atual.resolve("src/main/java");
    }
}
