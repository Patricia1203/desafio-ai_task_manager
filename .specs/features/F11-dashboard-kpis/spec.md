# spec.md — F11-dashboard-kpis

> Feature criada em 2026-10-08 a pedido do usuário: os KPIs do dashboard contavam tarefas e
> subtarefas como linhas independentes; ele pediu que, quando uma tarefa tem subtarefas,
> só as subtarefas sejam contadas, e que os cards ganhem contexto ("de quantos" ou "%").
> Explicitação feita via perguntas (2026-10-08): formato **"X de Y + %"** e alta prioridade
> contando **só as em aberto**.

## Objetivo
Dashboard com KPIs coerentes: contagem de **itens finais** (folhas) — tarefa que tem
subtask não conta, contam as subtarefas de menor nível — e cards com contexto "de Y + %".
Alta prioridade só fica vermelha quando há itens.

## Requisitos
- **Itens finais:** `total/pending/inProgress/done` contam apenas linhas sem subtarefas
  (regra recursiva: `not exists` filho). Tarefa com filhas é representada pelas filhas, não
  por ela mesma.
- **Alta prioridade em aberto:** `highPriority` conta itens finais de prioridade HIGH ainda
  **não concluídos** (antes incluía DONE). O `TaskSummary` mantém o mesmo contrato de
  `{total,pending,inProgress,done,highPriority}`.
- **Herói consistente:** o assistente (`TaskQueryTools#getTaskSummary`) usa as mesmas
  contagens do dashboard, então a IA responde com os mesmos números.
- **Cards com contexto:** cada KPI mostra o valor e uma linha de detalhe. Quando o valor é
  `0`, o detalhe é só `"0%"` (esconde a base — "0 de 8" parecia indicar que existiam itens);
  com valor > 0 mostra `"X de Y · Z%"` onde a base é o total. O card Total mostra
  `"Z% concluídas"`.
- **Vermelho condicional:** a cor perigo no valor só é aplicada à "Alta prioridade" quando
  o valor é `>= 1` (antes o 5º card era sempre vermelho por `:nth-child`, mesmo zerado).
- **Base "de" sempre o total** (não "não concluídas") — o "X de Y não concluídas" foi
  abandonado após feedback do usuário por ser confuso.

## Fora de escopo (decisões)
- Os blocos "Abertas por prioridade", "Próximos prazos" e o "atrasadas" do herói continuam
  derivados da listagem de raízes (`GET /tasks?size=50`) — o usuário não pediu para mudá-los.
- Nenhuma migration: o cálculo é feito por `@Query` do repositório (JPQL), sem tabela/visão.

## Sucesso
1. No banco real, o Total passou a refletir só itens finais (verificado: 2 raízes com 3 e 5
   subtarefas → `total 8`, não 10).
2. "Alta prioridade" com valor 0 aparece em azul e sem base "de Y"; com itens, em vermelho.
3. Cards mostram contexto "X de Y · Z%" (ou "0%") sem ambiguidade.
4. Gates: `mvn test` (259), `npx oxlint`, `npx vitest run` (51), `npx vite build` verdes.