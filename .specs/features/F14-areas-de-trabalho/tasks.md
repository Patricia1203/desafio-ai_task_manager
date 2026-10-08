# tasks.md — F14-areas-de-trabalho

> Feature criada em 2026-10-08 a pedido do usuário: agrupar tarefas por **área de trabalho**
> (estilo Trello) com nome + foto opcional, uma área por tarefa escolhida no formulário.

### T-F14-01 — Backend: work_areas + foto (multipart) + CRUD + GET de imagem
- **Status:** done (commit `a8eb111`)
- **Reqs:** RF-12 (lista filtravel), RF-25 (ajuste)
- **Depends on:** —
- **Arquivos (alterar):** `db/migration/V7__create_work_areas.sql` (novo), `area/domain/WorkArea.java`,
  `area/infra/{WorkAreaRepository,WorkAreaMapper}.java`, `area/application/WorkAreaService.java`,
  `area/api/WorkAreaController.java` (novos), `application.yml` (multipart 5MB), testes
  (`WorkAreaServiceTest`, `WorkAreaControllerTest`).
- **O que fazer:** migration V7 (`work_areas` + `tasks.area_id` FK `ON DELETE SET NULL` + índice);
  entidade `WorkArea` com `requireTitle` e imagem com content-type `image/*`; service de CRUD
  (listar sem bytes, criar, editar com troca/remoção de foto via `removeImage`, excluir, buscar
  bytes da imagem — 404 se sem foto); controller multipart `POST/PUT` com `title`+`image`(opcional)
  e `GET /areas/{id}/image`.
- **Pronto quando:** criar/editar com imagem grava e lista `{id, title, imageType}`; GET de imagem
  devolve os bytes com o content-type; `removeImage` limpa a foto; DELETE 204 e tarefas preservadas;
  título vazio/formato inválido/área inexistente dão 4xx pt-BR; suítes verdes.
- **Gate:** `mvn test`.

### T-F14-02 — Backend: área na tarefa (areaId no contrato) e filtro ?areaId
- **Status:** done (commit `7446970`)
- **Reqs:** RF-12, RF-25 (ajuste)
- **Depends on:** T-F14-01
- **Arquivos (alterar):** `task/domain/Task.java` (`@ManyToOne(LAZY)` `area` + `setArea`),
  `task/application/{TaskCommand,TaskService,TaskFilter,toSpecification}.java` (`areaId`),
  `task/api/dto/{TaskResponse,CreateTaskRequest,UpdateTaskRequest}.java`, testes
  (`TaskServiceTest`, `TaskControllerTest`).
- **O que fazer:** `TaskResponse` ganha `areaId`; requests aceitam `areaId` opcional; no `PUT`
  `areaId = null` remove a área; área inexistente → 422 "area de trabalho nao encontrada";
  `GET /tasks?areaId=` filtra a lista (combina com os filtros atuais).
- **Pronto quando:** criar/editar com `areaId` grava e serializa; `null` no PUT remove;
  id inexistente → 422; `?areaId=` filtra; suítes verdes.
- **Gate:** `mvn test`.

### T-F14-03 — Frontend: tipos, API, select no formulário e linha no detalhe
- **Status:** done (commits `587c829` + ajuste `4955f6e` — voltar da subtarefa vai ao detalhe do pai)
- **Reqs:** RF-25 (ajuste)
- **Depends on:** T-F14-01 (contrato de API)
- **Arquivos (alterar):** `types/area.ts` (novo), `api/areas.ts` (novo, FormData),
  `components/task/TaskForm.tsx` (select "Área de trabalho" com "Sem área" e valor inicial),
  `components/task/TaskDetail.tsx` (linha "Área"), `pages/TasksPage.tsx` (mapa de áreas),
  testes (`api/areas.test.ts`, `TaskForm.test`, `TaskDetail.test`).
- **O que fazer:** tipos + client de áreas (multipart via `client.ts`); select carregado de
  `listAreas` no mount; submit envia `areaId`; detalhe mostra o nome da área.
- **Pronto quando:** select lista as áreas com a selecionada marcada na edição; submit envia
  `areaId`; detalhe exibe a área; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.

### T-F14-04 — Frontend: páginas "Quadros" (/areas e /areas/:areaId) + nav + observações do usuário
- **Status:** done (commit `b04898c`)
- **Reqs:** RF-12, RF-25 (ajuste)
- **Depends on:** T-F14-03
- **Arquivos (alterar):** `pages/AreasPage.tsx`, `pages/AreaDetailPage.tsx` (novos), `AppLayout.tsx`
  (item "Áreas" → "Quadros"), `router.tsx`, `api/areas.ts` (`listAreas({title})`), `index.css`
  (cards), `TaskForm`/`TaskDetail`/`TaskList` (label "Quadro"/"Sem quadro", slot
  `adicionaisPorTarefa`), testes (`AreasPage.test`, `AreaDetailPage.test`, `AppLayout.test`).
- **O que fazer:** `/areas` — grade de cards (foto via imageUrl, título) com criar/editar
  (nome + upload + remover foto) e excluir com `alertdialog` avisando que as tarefas são
  preservadas; busca pelo título server-side (`GET /areas?title=`); cards maiores
  (minmax 260px, foto 190px); `/areas/:areaId` — cabeçalho com seta de voltar, as tarefas dela
  via `GET /tasks?areaId=`, "Nova tarefa neste quadro" e mover/transferir entre quadros
  (`updateTask` com `areaId` novo, com o filtro "Filtrar por título" oculto quando o quadro está
  vazio); nav com "Quadros".
- **Pronto quando:** cards renderizam foto e nome; busca filtra a grade; criar/editar/excluir
  funcionam com o backend; criar/mover tarefas dentro do quadro funcionam; página do quadro lista
  as tarefas filtradas; rotas e nav ok; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.

### T-F14-05 — Docs e rastreabilidade + E2E
- **Status:** done (SHAs por task nos commits de código + o commit de docs; E2E headless anotado abaixo)
- **Reqs:** DOC-01
- **Depends on:** T-F14-01..04, T-F14-06..09
- **Arquivos (alterar):** `.specs/features/F14-areas-de-trabalho/{spec,design,tasks}.md`,
  `.specs/project/TRACEABILITY.md`, `.specs/project/STATE.md`
- **Pronto quando:** matriz e STATE refletem a F14; SHAs por task; E2E headless: criar quadro com
  foto, associar tarefa, buscar por título e mover tarefa entre quadros.
- **Gate:** `grep -c "done (parcial"` matriz = 0; suítes verdes.
- **E2E (2026-10-08, containers rebuildados — `docker compose build backend frontend && up -d`):**
  `GET /areas?title=dom` → `['Domésticos']`; criar tarefa sem quadro → `areaId:null`; mover via
  `PUT /tasks/{id}` com `areaId` do quadro → filtro `GET /tasks?areaId=` traz a tarefa; limpeza
  DELETE 204. Backend `mvn test` **322/0**; front **97 + build**.

### T-F14-07 — Backend: filtro por título nas tarefas (`?title=`)
- **Status:** done (commit `a194e1c`)
- **Reqs:** RF-12
- **Depends on:** —
- **Arquivos (alterar):** `task/application/dto/TaskFilter.java`, `task/application/TaskService.java`,
  `task/api/TaskController.java`, testes (`TaskServiceTest`, `TaskControllerTest`).
- **O que fazer:** `TaskFilter` ganha `title` (ctors de 2/3 args preservados); `toSpecification`
  adiciona `tituloContem` (LIKE lower, escape de `\` `%` `_`); `GET /tasks?title=` repassa ao filtro.
- **Pronto quando:** contém parcial sem diferenciar caixa; `%`/`_` do termo literais; combina com
  status/prioridade/área; param repassa ao filtro; suítes verdes.
- **Gate:** `mvn test`.

### T-F14-06 — Backend: busca de quadro por título e vínculo das órfãs ao 1º quadro
- **Status:** done (commit `6ee6cb8`)
- **Reqs:** RF-12, RF-25 (ajuste)
- **Depends on:** T-F14-02
- **Arquivos (alterar):** `area/infra/WorkAreaRepository.java` (Specification),
  `area/application/WorkAreaService.java` (list(title) + backfill), `area/api/WorkAreaController.java`
  (`?title=`), `task/infra/TaskRepository.java` (`assignAreaToOrphans`), testes.
- **O que fazer:** `GET /areas?title=` (contém, caixa-insensível, `%`/`_` literais, ordem alfabética);
  criar o **1º** quadro vincula num UPDATE em lote as tarefas ainda sem quadro (as que já existiam).
- **Pronto quando:** busca filtra a grade; 1º quadro absorve as órfãs e o 2º não mexe nelas;
  suítes verdes.
- **Gate:** `mvn test`.

### T-F14-09 — Frontend: agenda na página Tarefas (timeline, calendário e resumo diário)
- **Status:** done (commit `fae869c`)
- **Reqs:** RF-12
- **Depends on:** T-F14-04
- **Arquivos (alterar):** `components/task/TaskSchedule.tsx` (novo), `pages/TasksPage.tsx`,
  `test/TasksPage.test.tsx`.
- **O que fazer:** sem kanban; a página Tarefas continua com a lista e ganha duas seções abaixo:
  "Próximos 7 dias" (hoje + 6, cada célula clicável com contagem de vencimentos) e o calendário
  do mês atual (grid dom..sáb, hoje destacado); clicar em um dia abre um `dialog` "Atividades de
  dd/mm/aaaa" com as tarefas do dia mostrando prioridade, status e descrição/resumo. O campo de
  busca por título que existia fora do quadro foi removido (a busca vive em Quadros).
- **Pronto quando:** timeline e calendário renderizam com os dados reais; clicar no dia abre o
  resumo com prioridade + resumo; busca fora do quadro não existe mais; testes verdes.
- **Gate:** `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build`.