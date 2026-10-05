# spec.md — F03-ai-task-features

## Objetivo
Implementar recursos de IA para tarefas: improve, analyze, decompose (+apply), com structured output, validação, porta/adaptador e painel de IA.

## Requisitos cobertos
RF-10..RF-14, RF-24, RNF-02, RNF-10..RNF-15, ERR-03..ERR-05, TST-01..TST-03

## User stories

US-020: Melhorar tarefa (RF-10)
- POST /api/ai/tasks/improve retorna sugestão (title,description) sem persistir. Aplicação decide ao atualizar tarefa.

US-021: Analisar tarefa (RF-11, RF-12)
- POST /api/ai/tasks/{id}/analyze retorna objeto tipado {priority, complexity, estimatedHours, reason}. Interpretação pelo sistema; nunca altera tarefa automaticamente.

US-022: Decompor e aplicar (RF-13, RF-14)
- POST /api/ai/tasks/{id}/decompose retorna sugestões; POST /api/ai/tasks/{id}/decompose/apply cria subtarefas relacionadas via parentTaskId (valida antes de criar).

US-023: Qualidade IA (RNF-10-RNF-15)
- Prompts versionados/parametrizados; structured output + validação pós-LLM; retry controlado; contexto mínimo; porta/adaptador (trocar provedor só em ai.adapter+config); IA integrada à app.

## Casos de borda
Resposta inválida JSON → retry → ERR-04; comunicação falha → ERR-03; indisponível → ERR-05; análise com enums inválidos rejeitada.

## Erros esperados
ERR-03, ERR-04, ERR-05

## Fora de escopo
Chat assistente.

## Perguntas em aberto
[NEEDS CLARIFICATION] Modelo Ollama com suporte a tool calling? (pode não ser necessário aqui, mas afetar F04)
