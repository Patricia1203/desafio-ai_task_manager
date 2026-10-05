# spec.md — F02-task-management

## Objetivo
Implementar CRUD de tarefas, status/prioridades, filtros, dashboard, API REST e UI básica.

## Requisitos cobertos
RF-01..RF-09, RF-20..RF-21, RF-23, ERR-01, ERR-02, ERR-06, TST-01, TST-02, TST-04

## User stories

US-010: Criar tarefa (RF-01, RF-07, ERR-02)
- Given dados válidos, When POST /api/tasks, Then 201 Location + TaskResponse com campos id,título,descrição,status,prioridade,prazo,data de criação.
- Given dados inválidos, When POST, Then 400 ProblemDetail com campos.

US-011: Listar/visualizar/editar/excluir (RF-02-RF-05)
- GET /api/tasks lista; GET /api/tasks/{id} 200 ou 404 (ERR-01); PUT /api/tasks/{id} edita ou 404/400; DELETE /api/tasks/{id} remove ou 404.

US-012: Status e prioridades (RF-06, RF-08, RF-09)
- PATCH /api/tasks/{id}/status altera entre A_FAZER, EM_ANDAMENTO, CONCLUIDA. Valores inválidos → 400.

US-013: Dashboard (RF-20)
- GET /api/tasks/summary retorna total, pendentes, em andamento, concluídas, alta prioridade.

US-014: UI (RF-21)
- Telas Dashboard e Tarefas com criar/editar/excluir/visualizar/alterar status/acessar IA.

## Casos de borda
Prazo nulo, descrição vazia? validar conforme Bean Validation; tarefa inexistente; exclusão sem subtarefas (definir comportamento depois se necessário).

## Erros esperados
ERR-01, ERR-02, ERR-06

## Fora de escopo
IA nesta feature.

## Perguntas em aberto
[NEEDS CLARIFICATION] Tipo de dueDate (LocalDate/OffsetDateTime) e fuso?
