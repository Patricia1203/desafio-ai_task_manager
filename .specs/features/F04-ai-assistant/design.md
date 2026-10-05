# design.md — F04-ai-assistant

## Visão geral
Assistente com grounding, histórico persistido, ferramentas somente-leitura e Tool Calling opcional.

## Componentes
assistant.api, assistant.application (tools), assistant.domain, assistant.infra.

## Memória
ChatConversation, ChatMessage (JPA). Janela limitada.

## Tools (somente-leitura)
getPendingTasks, getOverdueTasks, getTaskById, getTasksByPriority, getTasksDueSoon(days), getTaskSummary.

## Endpoint
POST /api/assistant/chat → {conversationId, answer}; histórico recuperável.

## Regras
Grounding estrito; data atual injetada; sem ferramentas de escrita.
