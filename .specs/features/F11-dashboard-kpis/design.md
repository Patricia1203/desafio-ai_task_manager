# design.md — F11-dashboard-kpis

## Decisões
- **Contagem por "itens finais" no banco, não em memória.** As 5 contagens viram `@Query`
  correlacionadas (`not exists (select 1 from Task s where s.parent = t)`), recursivas de
  graça: uma subtarefa que tem filhos também não conta. Alternativa de navegar a árvore no
  service foi descartada: traria N entidades para memória e duplicaria a regra.
- **Alta prioridade ≠ "todas HIGH":** antes `countByPriorityValue(HIGH)` incluía DONE e o 5º
  KPI não era uma partição dos demais. Agora é itens finais HIGH **não concluídos**
  (`countLeavesByPriorityValueAndNotDone`), coerente com Pendentes/Em andamento.
- **Métodos antigos removidos, não mantidos lado a lado.** `countAll`, `countByStatusValue`
  e `countByPriorityValueAndNotDone` só o summary usava → foram substituídos; já
  `countByPriorityValue` permanece porque a ferramenta de lista por prioridade do assistente
  (`getTasksByPriority`) conta a página inteira (raízes + subtarefas), sem relação com o KPI.
- **Assistente e dashboard lêem da mesma fonte.** `TaskQueryTools#getTaskSummary` copia as
  cinco contagens novas; continuam iguais por construção (sem divergência dashboard/IA).
- **Base do card é o total (folhas), não "não concluídas".** A primeira versão ("X de Y não
  concluídas") foi rejeitada na revisão com o usuário: "0 de 9 não concluídas" parecia dizer
  que havia 9 de alta prioridade. Base única e neutra (total) elimina a ambiguidade.
- **Valor 0 esconde a base.** `detalheDe(valor, base)`: `0 → "0%"`, senão `"X de Y · Z%"`.
  Regra uniforme para os 4 cards numéricos; evita "0 de 8" em qualquer KPI zerado.
- **Vermelho por classe, não por posição.** `.dashboard__card:nth-child(5)` virou
  `.dashboard__card--perigo` aplicada só quando `chave === 'highPriority' && valor >= 1`.
- **Nenhuma migration.** Não há mudança de schema; as contagens são JPQL puro. O backend foi
  reconstruído no container (rebuild) para validar com os dados reais.

## Fluxo (frontend)
```
GET /tasks/summary ──→ TaskSummary {total,pending,inProgress,done,highPriority}
   │
   ├─ Cards: valor grande + rotulo + detalhe
   │     total:      "{pct}% concluídas"
   │     pending/…:  detalheDe(valor, total)  ("X de Y · Z%" ou "0%")
   │     alta:       cor perigo só se valor >= 1 (classe dashboard__card--perigo)
   └─ Herói/prioridades/prazos: inalterados (lista de raízes)
```

## Backend
- **`TaskRepository`:** novas `countLeaves()`, `countLeavesByStatusValue(status)`,
  `countLeavesByPriorityValueAndNotDone(priority, done)`; removidas `countAll`,
  `countByStatusValue`, `countByPriorityValueAndNotDone`; `countByPriorityValue` mantida.
- **`TaskService.summary` e `TaskQueryTools.getTaskSummary`:** usam as três queries novas.
- **`TaskSummary`:** campos iguais; javadoc atualizado (itens finais; alta = HIGH não DONE).
- **Testes:** `TaskServiceTest` (folhas + alta não concluída), `TaskRepositoryTest`
  (profundidade pai→filha→neta, folha HIGH), `TaskQueryToolsTest` e
  `SpringAiAssistantAdapterTest` (stubs novos). `summaryContaCadaIndicador` e
  `summarySomaPorStatusIgualAoTotal` continuam verdes (só raízes ⇒ folhas = todas).

## Frontend
- `DashboardPage.tsx`: `pctDe(parte, base)` e `detalheDe(valor, base)`; mapa `detalhes` por
  chave; card ganha `dashboard__detalhe` e classe `dashboard__card--perigo` condicional.
- `index.css`: `.dashboard__detalhe` (0.75rem, muted) e
  `.dashboard__card--perigo .dashboard__valor`; removido o `:nth-child(5)`.
- Testes: detalhes "X de Y · %" no cenário 12/5/3/4/2; alta zerada sem a classe de perigo e
  com detalhe sem "de 4".

## Evoluções futuras (não implementadas)
- Aplicar a contagem de itens finais a "Abertas por prioridade", "Próximos prazos" e ao
  "atrasadas" do herói (hoje derivados da listagem de raízes).
- Percentual com uma barra miniatura em cada card, se desejado para a demo.