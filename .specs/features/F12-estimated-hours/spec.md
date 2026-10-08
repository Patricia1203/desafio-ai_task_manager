# spec.md — F12-estimated-hours

> Feature criada em 2026-10-08 a pedido do usuário: persistir o **tempo estimado** por
> tarefa/subtarefa (com unidade escolhível, **horas ou dias**) e expandir o bloco
> "Próximos prazos" do dashboard: com clique na raiz, as subtarefas aparecem **recuadas,
> uma embaixo da outra**, cada uma com o seu tempo. Escolha de unidade autorizada via
> pergunta ("como a tarefa principal") — o campo carrega valor + unidade juntos.

## Objetivo
Toda tarefa/subtarefa pode registrar quanto tempo em horas ou dias ela deve levar; o tempo
fica visível no detalhe e no "Próximos prazos" do dashboard, que ganha expansão por linha
para revelar as subtarefas recuadas.

## Requisitos
- **Persistência:** colunas `estimated_time DOUBLE PRECISION` e `estimated_unit VARCHAR(10)`
  (`HOURS | DAYS`) na tabela `tasks`, **ambas nulas juntas** (sem tempo = sem unidade);
  migration `V6__tasks_estimated_time.sql`.
- **Contrato EN:** `TaskResponse`, `CreateTaskRequest` e `UpdateTaskRequest` ganham
  `estimatedTime` (número, `> 0` e `<= 200`) e `estimatedUnit` (string `HOURS | DAYS`).
- **Regras de escrita:** `setEstimatedTime` rejeita valor `<= 0` ou `> 200` com
  `BusinessRuleException` ("tempo estimado deve ser maior que zero e no maximo 200");
  unidade sem valor é descartada; sem unidade assume `HOURS`.
- **IA:** a decomposição (`POST /ai/tasks/{id}/decompose`) persiste o `estimatedHours` de
  cada subtarefa gerada como `HOURS`; "melhorar" (`PATCH`/melhoria aplicada) **preserva** o
  tempo estimado existente da tarefa (o PUT do frontend reenvia os campos atuais).
- **Frontend (form/detalhe):** `TaskForm` ganha "Tempo estimado" (número, 0..200, passo 0.1)
  e "Unidade" (Horas/Dias) lado a lado, pré-preenchidos na edição; `TaskDetail` exibe
  `"2 horas"`/`"1 dia"` (singular/plural) ou "Sem tempo estimado".
- **Frontend (dashboard):** "Próximos prazos" mostra o tempo de cada linha e permite
  **expandir** a que tem subtarefas (`GET /tasks/{id}/subtasks`), listando os filhos
  recuados (ul aninhado com borda esquerda), um embaixo do outro, com o tempo de cada;
  expansão recursiva para níveis seguintes.
- **Erros amigáveis:** validação duplicada no form (mensagem pt-BR) e mensagens dos campos
  no backend em pt-BR.

## Fora de escopo (decisões)
- Sem "horas gastas/registro de apontamento" e sem somatório de tempos (nada de estimar o
  total de uma raiz somando as filhas). O changelog de tempo também fica de fora.
- A lista "Próximos prazos" continua derivada das raízes (`GET /tasks?size=50`); a expansão
  busca subtarefas sob demanda (uma requisição só quando o usuário clica).
- O assistente/IA não ganha ferramenta nova para tempo estimado nesta feature.

## Sucesso
1. Criar/editar tarefa e subtarefa com `2.5` + `HOURS` (ou `3` + `DAYS`): grava e devolve no
   contrato; `<= 0`/`> 200` retorna `400` apontando o campo.
2. No dashboard, um prazo com subtarefas tem seta; ao clicar, os filhos aparecem recuados,
   com o tempo de cada; recolher volta ao estado inicial sem recarregar a página.
3. Decomposição gera subtarefas com tempo (horas); "melhorar" uma tarefa não apaga seu tempo.
4. Gates: `mvn test` (272), `npx oxlint`, `npx tsc -b`, `npx vitest run` (59),
   `npx vite build` verdes; E2E real no dev server (expansão + "2.5 horas").