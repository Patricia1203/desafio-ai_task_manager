# design.md — F02-task-management

## Visão geral
CRUD Task, status/prioridades, filtros, summary, auto-relacionamento para subtarefas.

## Componentes
task.api, task.application, task.domain, task.infra.

## Modelo
Task(id UUID, title, description, status (enum TODO/IN_PROGRESS/DONE), priority (LOW/MEDIUM/HIGH), dueDate, createdAt, updatedAt, parentTaskId UUID nullable). Índices (status, priority, dueDate, parent_task_id).

## Endpoints
GET /api/tasks?status&priority&page&size; GET /api/tasks/{id}; POST /api/tasks; PUT /api/tasks/{id}; PATCH /api/tasks/{id}/status; DELETE /api/tasks/{id}; GET /api/tasks/{id}/subtasks; GET /api/tasks/summary.

## Validações
Bean Validation + regras de negócio em Service. Status/priority enums.

## Erros
ERR-01 404, ERR-02 400, ERR-06 500.

## Estratégia de teste
TST-01 unitários, TST-02 @WebMvcTest, TST-04 integração.
