package com.desafio.taskmanager.ai.port.dto;

/**
 * Complexidade estimada pela análise de IA (RF-11). Ao contrário de
 * {@code TaskPriority}, não é domínio nem persistência: existe só no contrato
 * com o modelo, com os valores exatos do schema que ele deve devolver
 * (LOW/MEDIUM/HIGH, design.md F03).
 */
public enum TaskComplexity {

    LOW,
    MEDIUM,
    HIGH
}
