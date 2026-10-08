# tasks.md — F11-dashboard-kpis

> Feature criada em 2026-10-08 a pedido do usuário: KPIs do dashboard somam "itens finais"
> (tarefa com subtarefas não conta, contam as subtarefas), com contexto "de Y + %" e alta
> prioridade em aberto. Implementação autorizada com duas perguntas de esclarecimento.

### T-F11-01 — Backend: summary conta itens finais e alta prioridade em aberto
- **Status:** done (commit `0e02381`, 2026-10-08)
- **Reqs:** RF-20
- **Depends on:** —
- **Arquivos (alterar):** `task/infra/TaskRepository.java`, `task/application/TaskService.java`,
  `task/application/dto/TaskSummary.java`, `assistant/application/tools/TaskQueryTools.java`,
  testes (`TaskServiceTest`, `TaskRepositoryTest`, `TaskQueryToolsTest`, `SpringAiAssistantAdapterTest`)
- **O que fazer:** add `countLeaves`, `countLeavesByStatusValue`, `countLeavesByPriorityValueAndNotDone`
  (JPQL correlacionado `not exists`); remover `countAll`/`countByStatusValue`/`countByPriorityValueAndNotDone`
  (só o summary usava); `TaskService.summary` e `TaskQueryTools.getTaskSummary` usarem as mesmas
  contagens.
- **Pronto quando:** no banco real só as folhas contam (2 raízes com 3+5 subs → total 8);
  alta prioridade exclui DONE; suíte verde.
- **Gate:** `mvn test` verde (259).

### T-F11-02 — Frontend: cards com "X de Y + %" e vermelho condicional
- **Status:** done (commits `1a5981f`, `bd79ac2`, 2026-10-08)
- **Reqs:** RF-20
- **Depends on:** T-F11-01
- **Arquivos (alterar):** `frontend/src/pages/DashboardPage.tsx`, `frontend/src/index.css`,
  `frontend/src/pages/DashboardPage.test.tsx`
- **O que fazer:** linha de detalhe por card com base no total — `pctDe(parte, base)` +
  `detalheDe(valor, base)` (valor 0 → `"0%"`; senão `"X de Y · Z%"`); Total → `"Z% concluídas"`;
  vermelho condicional na alta (`dashboard__card--perigo` só com valor >= 1). Segunda passada
  (`bd79ac2`): esconder a base de Total quando o valor é zero (feedback do usuário sobre
  "0 de 8" parecer existir itens).
- **Pronto quando:** cards mostram contexto sem ambiguidade; "0 de 8" some quando zerado;
  alta só vermelha com itens; dev server com dados reais conferido.
- **Gate:** `npx oxlint`, `npx vitest run` (51), `npx tsc -b && npx vite build` verdes.

### T-F11-03 — Docs e rastreabilidade
- **Status:** done (2026-10-08)
- **Reqs:** DOC-01
- **Depends on:** T-F11-01, T-F11-02
- **Arquivos (alterar):** `.specs/features/F11-dashboard-kpis/{spec,design,tasks}.md`,
  `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **O que fazer:** spec/design/tasks da F11; RF-20 na matriz ganha o incremento com os SHAs
  (`0e02381`, `1a5981f`, `bd79ac2`); STATE com o resumo e as decisões.
- **Pronto quando:** matriz e STATE refletem o recurso; sem pendências não justificadas.
- **Gate:** `grep -c "done (parcial"` continua 0; `mvn test` e `npx vitest run` verdes.