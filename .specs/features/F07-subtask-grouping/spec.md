# spec.md — F07-subtask-grouping

## Objetivo
Corrigir o agrupamento das subtarefas em relação à tarefa original. Hoje o vínculo existe no banco (`parent_id`, FK, cascade, `GET /tasks/{id}/subtasks`), mas é invisível: `GET /tasks` devolve tarefas-raiz e subtarefas misturadas numa lista plana e o frontend não apresenta nenhum indício de pai/filho, então cada subtarefa "parece" uma tarefa independente. Request do usuário em 2026-10-07, após a revisão da T-F06-07.

## Escopo
1. **Backend** — a lista pública (`GET /tasks`) passa a devolver só tarefas-raiz; subtarefas continuam acessíveis por `GET /tasks/{id}` e `GET /tasks/{id}/subtasks`. A resposta da lista informa quantas subtarefas cada tarefa tem (`subtaskCount`), com contagem agrupada no banco (sem N+1).
2. **Frontend** — a linha da lista mostra um selo "N subtarefas" quando houver; o detalhe da tarefa passa a exibir as subtarefas agrupadas sob "Subtarefas (N)", e a tarefa que é subtarefa mostra o vínculo "Subtarefa de <título do pai>" com navegação de volta ao pai.
3. **Docs/rastreabilidade** — RF-13/RF-14 refletem o agrupamento; TRACEABILITY/STATE/tasks atualizados.

## Requisitos cobertos
- **RF-13** (§6: decompor em subtarefas) — done; agora com apresentação no detalhe.
- **RF-14** (§6: subtarefas adicionadas como tarefas relacionadas) — done; agora com vínculo visível.
- **RF-02** (§5: CRUD e listagem de tarefas) — a listagem deixa de misturar subtarefas.

## Perguntas em aberto
- Ressolvido e registrado: nenhuma. Observação registrada no design: o assistente (F04) consulta o repositório diretamente e continua vendo subtarefas nas ferramentas de listagem/contagem; será explicitamente mantido, para não reabrir o comportamento dele nesta feature.