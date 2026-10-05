# spec.md — F04-ai-assistant

## Objetivo
Assistente em linguagem natural sobre tarefas do usuário, com grounding (só contexto fornecido), histórico, e opcional Tool Calling somente-leitura.

## Requisitos cobertos
RF-15..RF-19, RF-22, RF-24, RNF-10..RNF-15, ERR-03..ERR-05, TST-01..TST-03

## User stories

US-030: Assistente com grounding (RF-15, RF-16)
- POST /api/assistant/chat recebe {conversationId?, message}, retorna {conversationId, answer}. Backend injeta contexto necessário; modelo não responde fora do contexto. Data atual injetada.

US-031: Histórico (RF-17, RF-18)
- Histórico mantido na sessão; persistido em banco (ChatConversation/ChatMessage).

US-032: Tool Calling somente-leitura (RF-19) [DIF]
- Ferramentas: getPendingTasks, getOverdueTasks, getTaskById, getTasksByPriority, getTasksDueSoon(days), getTaskSummary. Resultados enxutos. Se modelo não suportar, registrar decisão.

US-033: UI (RF-22)
- Tela do assistente com conversa, histórico, indicador digitação, nova conversa.

## Casos de borda
Mensagem sem contexto relevante → dizer não encontrou com base em dados disponíveis.
Tools retornam vazio.

## Erros esperados
ERR-03, ERR-04, ERR-05

## Fora de escopo
Ferramentas de escrita.

## Perguntas em aberto
[NEEDS CLARIFICATION] Modelo suporta tool calling? Validar em STACK.md (Fase 2)
