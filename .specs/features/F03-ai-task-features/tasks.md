# tasks.md — F03-ai-task-features

### T-F03-01 — Porta, DTOs e config AI
- Status: pending
- Reqs: RNF-14,RNF-11
- Depende de: T-F02-04
- Arquivos: ai/port/TaskAiPort.java, ai/dto/*, config/AiConfig.java, resources/prompts/*.st
- O que fazer: Interface porta, records tipados, BeanOutputConverter verificado, prompts parametrizados.
- Pronto quando: Contratos definidos; porta sem import Spring AI.
- Testes: TST-03 unidade.
- Gate: cd backend && mvn -q test -Dtest=*AiPort*Test* 2>&1 | tail -1
- Commit: dd: Porta de IA, DTOs estruturados e prompts

### T-F03-02 — Adaptador Spring AI + tratamento erros
- Status: pending
- Reqs: RNF-02,RNF-10..RNF-15,ERR-03..ERR-05
- Depende de: T-F03-01
- Arquivos: ai/adapter/TaskAiAdapter.java
- O que fazer: ChatClient, structured output, validação pós-LLM, retry 1x, mapeamento erros.
- Pronto quando: Valida enums/intervalos; retry controlado; erros mapeados.
- Testes: TST-03 com ChatModel mockado.
- Gate: cd backend && mvn -q test -Dtest=*TaskAiAdapter*Test* 2>&1 | tail -5
- Commit: dd: Adaptador Spring AI com validação e retry

### T-F03-03 — Serviço AI para tarefas (improve/analyze/decompose/apply)
- Status: pending
- Reqs: RF-10..RF-14,RF-24
- Depende de: T-F03-02
- Arquivos: task/application/AiTaskService.java, ai/api/AiTaskController.java
- O que fazer: Lógica de aplicação decide; improve/decompose não persistem; apply cria subtarefas via parentTaskId validando.
- Pronto quando: Endpoints corretos, sem persistência indevida.
- Testes: TST-01,TST-02,TST-03.
- Gate: cd backend && mvn -q test -Dtest=*AiTask*Test* 2>&1 | tail -8
- Commit: dd: Serviço e endpoints de IA para tarefas

### T-F03-04 — Painel de IA no frontend
- Status: pending
- Reqs: RF-10..RF-14,RF-21
- Depende de: T-F03-03
- Arquivos: frontend/src/api/ai.ts, frontend/src/components/task/AiPanel.tsx
- O que fazer: Melhorar/analisar/decompor/aplicar com loading/erro; exibir sugestões.
- Pronto quando: Build passa, interações testadas.
- Testes: Vitest com mocks.
- Gate: cd frontend && npm run build && npx vitest run --reporter=basic 2>&1 | tail -5
- Commit: dd: Painel de IA para melhorar/analisar/decompor tarefas
