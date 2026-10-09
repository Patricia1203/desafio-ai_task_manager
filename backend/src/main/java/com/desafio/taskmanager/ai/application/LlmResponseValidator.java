package com.desafio.taskmanager.ai.application;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.desafio.taskmanager.ai.adapter.config.AiProperties;
import com.desafio.taskmanager.ai.port.dto.ProposedSubtask;
import com.desafio.taskmanager.ai.port.dto.TaskAnalysis;
import com.desafio.taskmanager.ai.port.dto.TaskDecomposition;
import com.desafio.taskmanager.ai.port.dto.TaskImprovement;
import com.desafio.taskmanager.common.error.InvalidLlmResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validação pós-LLM (RNF-10, ERR-04): a resposta crua só vira record tipado
 * depois de passar por aqui.
 *
 * <p>Duas camadas de rejeição. O parse já derruba JSON malformado, campo
 * desconhecido e enum fora do contrato (o Jackson lança {@link JacksonException}).
 * As checagens de domínio cobrem o que o parse deixa passar: texto vazio ou
 * acima do máximo, horas fora de (0, {@code max-estimated-hours}] e quantidade
 * de subtarefas fora de {@code min-subtasks..max-subtasks}.
 *
 * <p>Os limites vêm da configuração ({@link AiProperties}) para o mesmo número
 * valer em produção e no teste, sem literal repetido — o {@code AiProperties}
 * é a fonte única de {@code app.ai.*}. O mapper é estrito
 * ({@code FAIL_ON_UNKNOWN_PROPERTIES}): sobrar campo na resposta do modelo é
 * erro aqui, não silêncio — assim o retry do adaptador (T-F03-02) tem o que
 * corrigir.
 */
@Component
public class LlmResponseValidator {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final int minSubtasks;
    private final int maxSubtasks;
    private final double maxEstimatedHours;
    private final int maxTitleLength;
    private final int maxTextLength;

    public LlmResponseValidator(AiProperties properties) {
        this.minSubtasks = properties.minSubtasks();
        this.maxSubtasks = properties.maxSubtasks();
        this.maxEstimatedHours = properties.maxEstimatedHours();
        this.maxTitleLength = properties.maxTitleLength();
        this.maxTextLength = properties.maxTextLength();
    }

    /** RF-10: título e descrição são obrigatorios e limitados. */
    public TaskImprovement validateImprovement(String rawJson) {
        TaskImprovement improvement = parse(rawJson, TaskImprovement.class, "melhoria");
        requireText(improvement.title(), "title", maxTitleLength, "melhoria");
        requireText(improvement.description(), "description", maxTextLength, "melhoria");
        return improvement;
    }

    /** RF-11: enums já chegam do parse; aqui ficam horas, motivo e obrigatoriedade. */
    public TaskAnalysis validateAnalysis(String rawJson) {
        TaskAnalysis analysis = parse(rawJson, TaskAnalysis.class, "analise");
        if (analysis.priority() == null) {
            throw new InvalidLlmResponseException(
                    "campo 'priority' ausente na resposta da IA de analise");
        }
        if (analysis.complexity() == null) {
            throw new InvalidLlmResponseException(
                    "campo 'complexity' ausente na resposta da IA de analise");
        }
        requireHours(analysis.estimatedHours(), "analise");
        requireText(analysis.reason(), "reason", maxTextLength, "analise");
        return analysis;
    }

    /** RF-12: quantidade no intervalo, títulos únicos, textos e horas válidos. */
    public TaskDecomposition validateDecomposition(String rawJson) {
        TaskDecomposition decomposition = parse(rawJson, TaskDecomposition.class, "decomposicao");
        if (decomposition.subtasks() == null) {
            throw new InvalidLlmResponseException(
                    "campo 'subtasks' ausente na resposta da IA de decomposicao");
        }
        int total = decomposition.subtasks().size();
        if (total < minSubtasks || total > maxSubtasks) {
            throw new InvalidLlmResponseException("quantidade de subtarefas (" + total
                    + ") fora do intervalo " + minSubtasks + ".." + maxSubtasks
                    + " na resposta da IA de decomposicao");
        }
        Set<String> titles = new HashSet<>();
        for (int i = 0; i < total; i++) {
            ProposedSubtask subtask = decomposition.subtasks().get(i);
            if (subtask == null) {
                throw new InvalidLlmResponseException(
                        "subtarefa nula na posicao " + i + " da resposta da IA de decomposicao");
            }
            requireText(subtask.title(), "subtasks[" + i + "].title", maxTitleLength, "decomposicao");
            requireText(subtask.description(), "subtasks[" + i + "].description", maxTextLength, "decomposicao");
            if (subtask.estimatedHours() != null) {
                requireHours(subtask.estimatedHours(), "decomposicao");
            }
            if (!titles.add(normalize(subtask.title()))) {
                throw new InvalidLlmResponseException(
                        "subtarefa duplicada na resposta da IA: '" + subtask.title() + "'");
            }
        }
        return decomposition;
    }

    private <T> T parse(String rawJson, Class<T> type, String useCase) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new InvalidLlmResponseException("resposta da IA vazia no caso de " + useCase);
        }
        try {
            return JSON.readValue(rawJson, type);
        } catch (JacksonException e) {
            throw new InvalidLlmResponseException(
                    "resposta da IA invalida no caso de " + useCase + ": " + e.getMessage(), e);
        }
    }

    private void requireText(String value, String field, int maxLength, String useCase) {
        if (value == null || value.isBlank()) {
            throw new InvalidLlmResponseException(
                    "campo '" + field + "' vazio na resposta da IA de " + useCase);
        }
        if (value.length() > maxLength) {
            throw new InvalidLlmResponseException("campo '" + field + "' com " + value.length()
                    + " caracteres, acima do limite de " + maxLength
                    + " na resposta da IA de " + useCase);
        }
    }

    private void requireHours(Double hours, String useCase) {
        if (hours == null) {
            throw new InvalidLlmResponseException(
                    "campo 'estimatedHours' ausente na resposta da IA de " + useCase);
        }
        if (hours <= 0 || hours > maxEstimatedHours) {
            throw new InvalidLlmResponseException("campo 'estimatedHours' (" + hours
                    + ") fora do intervalo (0, " + maxEstimatedHours
                    + "] na resposta da IA de " + useCase);
        }
    }

    /** Título duplicado vale ignorando caixa e espaços das pontas. */
    private static String normalize(String title) {
        return title.trim().toLowerCase(Locale.ROOT);
    }
}
