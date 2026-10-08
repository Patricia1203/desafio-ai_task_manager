# design.md — F09-subtask-completion

Decisões fechadas com o usuário em 2026-10-08 (perguntas: contrato do backend e recusa do modal).

## 1. Contrato da API (campo opcional no PATCH status)

O backend conclui o pai e as filhas **no mesmo endpoint existente**, sem rota nova:

- `PATCH /api/tasks/{id}/status` ganha o campo **opcional** `completeSubtasks` (Boolean, contrato EN
  conforme F06) no corpo — ex.: `{"status":"DONE","completeSubtasks":true}`.
- O campo só tem efeito quando `status == DONE`. Com outro status é ignorado (comportamento de
  mudar só a si mesmo). Ausente/`false` = comportamento atual (conclui apenas o pai).
- Sem `completeSubtasks` o PATCH continua válido para clientes antigos (retrocompatível).

### Por que campo opcional e não endpoint novo

- Mesmo recurso (o status da tarefa) e mesma semântica (transição); adicionar uma rota nova
  aumentaria a superfície da API sem ganho.
- O frontend já chama `PATCH /tasks/{id}/status` para qualquer troca; com o campo, a conclusão em
  cascata é um único request transacional no lugar de N requests separados.

## 2. Regra no TaskService (RF-06 combinada com RF-14)

```java
@Transactional
public Task changeStatus(UUID id, TaskStatus newStatus, boolean completeSubtasks)
```

- `changeStatus(id, status)` (2 argumentos) continua existindo e delega com `false` — testes e
  demais chamadores intocados.
- Com `completeSubtasks && newStatus == DONE`: conclui o pai e depois **todas as subtarefas diretas
  não-DONE** (`findByParentIdOrderByCreatedAtAsc`); filhas que já estão `DONE` são no-op
  (`changeStatus` retorna cedo em status igual).
- **Escopo: filhas diretas apenas** — mesmo universo do modal de exclusão em cascata (T-F02-05) e do
  bloco de subtarefas (F07). Netas continuam sob responsabilidade da sua própria mãe. Registrado
  para não "consertar" alargando o escopo sem pedido.
- A transição do pai é feita primeiro: se o pai for recusado (ex.: já `DONE` indo para outro estado
  não se aplica aqui porque a flag só atua com `DONE`), nada é concluído — tudo na mesma transação.

## 3. UX no TaskDetail (padrão do modal de exclusão)

Fluxo idêntico ao da exclusão em cascata (T-F02-05), que o usuário já validou:

1. Usuário seleciona `CONCLUÍDA` no `StatusSelect`.
2. O `TaskDetail` consulta as pendentes (reusa o `subtasks` já carregado; se ainda não carregou,
   busca na hora, como `handleDelete` faz).
3. Sem pendentes → `changeStatus(task.id, "DONE")` direto (hoje).
4. Com pendentes → `role="alertdialog"` com a lista de pendentes e os botões **Concluir** /
   **Cancelar** (classes `.dialog*` existentes).
5. **Concluir** → `changeStatus(task.id, "DONE", true)`; `onChanged` atualiza o pai e as subtarefas
   locais passam a `DONE` para os badges refletirem; fecha o modal.
6. **Cancelar** → fecha sem chamar a API; o select (controlado por `task.status`) permanece no
   valor anterior — **cancela tudo** (decisão do usuário).

## 4. Erros

- Falha no request de conclusão: mensagem em `role="alert"` dentro/frente do modal (reusa o
  `statusError`) e o modal continua aberto para nova tentativa — mesmo padrão do `deleteError`.

## 5. Rastreabilidade

- Tasks: T-F09-01 (backend), T-F09-02 (frontend), T-F09-03 (docs).
- Reqs: RF-06 (transição de status) + RF-14 (subtarefas).