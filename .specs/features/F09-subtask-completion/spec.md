# spec.md — F09-subtask-completion

> Feature criada em 2026-10-08 a pedido do usuário: "Quando conclui a tarefa pai as filhas
> continuam ativas, dá um modal para conclusão se as subtarefas não estiverem como concluídas e
> se aceitar elas passam para concluídas automaticamente". Decisões de contrato e apresentação em
> `design.md` (campo opcional no PATCH; recusa cancela tudo).

## Objetivo

Concluir uma tarefa pai com subtarefas pendentes exige confirmação: o usuário escolhe CONCLUÍDA no
status e, havendo subtarefas não concluídas, um modal pergunta se quer concluí-las também. Aceitando,
pai e subtarefas pendentes vão para CONCLUÍDA numa única transação; recusando, nada muda.

## Critérios de aceite

- **RF-06** — escolher `DONE` numa tarefa sem subtarefas (ou com todas já concluídas) conclui direto, como hoje.
- **RF-06/RF-14** — escolher `DONE` num pai com subtarefas pendentes abre modal listando as pendentes; confirmar conclui pai + pendentes na mesma transação (o `changeStatus` no pai e nas filhas transaciona junto); cancelar não altera nada (o select volta ao status anterior).
- **RF-06** — a semântica do `PATCH /tasks/{id}/status` sem o campo novo permanece idêntica à anterior (conclui só o pai).
- **RF-06** — conclusão em cascata só faz sentido com `status=DONE`; com outro status o campo é ignorado.
- **RNF-14** — a regra de "quais filhas concluir" vive no `TaskService` (camada de aplicação), não no controller nem no frontend.