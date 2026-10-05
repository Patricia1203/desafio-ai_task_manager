# design.md — F02-task-management

## Visão geral
CRUD Task, status/prioridades, filtros, summary, auto-relacionamento para subtarefas.

## Componentes
task.api, task.application, task.domain, task.infra.

## Modelo
Task(id UUID, title, description, status (enum A_FAZER/EM_ANDAMENTO/CONCLUIDA), priority (enum BAIXA/MEDIA/ALTA), dueDate, createdAt, updatedAt, parentTaskId UUID nullable). Índices (status, priority, due_date, parent_task_id).

O modelo JPA mantém os nomes internos em inglês (title, priority, due_date); a tradução para português vale para o **contrato JSON** e para os valores gravados no banco (`CHECK` e `DEFAULT` na V2).

## Contrato JSON (T-F02-05e)
- Tarefa: `id`, `titulo`, `descricao`, `status`, `prioridade`, `prazo`, `idTarefaPai`, `criadoEm`, `atualizadoEm`.
- Requests: `titulo`, `descricao`, `prioridade`, `prazo`.
- Paginação: `conteudo`, `pagina`, `tamanho`, `totalItens`, `totalPaginas`, `primeira`, `ultima`.
- Summary: `total`, `pendentes`, `emAndamento`, `concluidas`, `altaPrioridade`.
- `ProblemDetail` segue o padrão RFC 7807 (`type`, `title`, `status`, `detail`) e não é traduzido.
- Nomes de **query param** seguem em inglês (`status`, `priority`, `page`, `size`): a tradução cobre corpo e resposta JSON, não a URL.

## Endpoints
GET /api/tasks?status&priority&page&size; GET /api/tasks/{id}; POST /api/tasks; PUT /api/tasks/{id}; PATCH /api/tasks/{id}/status; DELETE /api/tasks/{id}; GET /api/tasks/{id}/subtasks; GET /api/tasks/summary.

## Validações
Bean Validation + regras de negócio em Service. Status/priority enums.

## Erros
ERR-01 404, ERR-02 400, ERR-06 500.

## Estratégia de teste
TST-01 unitários, TST-02 @WebMvcTest, TST-04 integração.
