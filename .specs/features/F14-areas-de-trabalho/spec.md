# spec.md — F14-areas-de-trabalho

> Feature criada em 2026-10-08 a pedido do usuário: agrupar tarefas em **áreas de trabalho**
> (estilo Trello) com **nome e foto** opcional, em que cada tarefa pertence a **uma** área
> escolhida no formulário da tarefa.

## Objetivo
Permitir organizar as tarefas por área de trabalho (ex.: "Pessoal", "Projeto X"), cada área
com nome e uma foto (imagem carregada via multipart e guardada no banco). A associação é feita
no formulário de criar/editar tarefa (1 área por tarefa, opcional). Páginas novas listam as
áreas com suas fotos e mostram as tarefas de cada área.

## Requisitos

### R1 — Área de trabalho (backend)
- Tabela nova `work_areas` (migração V7): `id UUID` (gerado em Java, padrão do projeto),
  `title VARCHAR(100) NOT NULL`, `image BYTEA` (opcional), `image_type VARCHAR(50)` (opcional,
  e.g. `image/png`), `created_at TIMESTAMPTZ NOT NULL`.
- Entidade `WorkArea` (pacote `area/domain`), repositório `area/infra`, serviço `area/application`.
- Endpoints sob `/api/areas` (context-path `/api` do projeto):
  - `GET /areas` → lista **resumo** `{id, title, imageType}` (sem os bytes) ordenada por título.
  - `POST /areas` → multipart com `title` + `image` (arquivo opcional) → 201 com o resumo.
  - `PUT /areas/{id}` → multipart com `title` + `image` (opcional; substitui quando enviado) → 200
    com o resumo; **REMOVER a foto** exige envio com flag booleana (ver design).
  - `DELETE /areas/{id}` → 204; as tarefas da área voltam a `area_id NULL` (não apagam).
  - `GET /areas/{id}/image` → bytes com `Content-Type` do `image_type`; 404 se a área ou a foto
    não existem.
- Regras: título obrigatório e até 100 (`@NotBlank` + `@Size(max=100)`, mensagens pt-BR);
  imagem deve vir com `Content-Type` começando em `image/`; tamanho máximo 5 MB configurado no
  `application.yml` (`spring.servlet.multipart`).

### R2 — Tarefa tem uma área
- `tasks` ganha `area_id UUID` com FK → `work_areas(id)` e `ON DELETE SET NULL`; índice em
  `area_id`. `Task.area` é `@ManyToOne(fetch = LAZY)` (padrão do `parent`).
- Contrato: `TaskResponse` expõe `areaId` (UUID ou `null`); `CreateTaskRequest`/`UpdateTaskRequest`
  ganham `areaId` opcional. No `PUT`, `areaId` segue a convenção de substituir conteúdo: `null`
  **remove** a área.
- Área inexistente ao gravar a tarefa → `BusinessRuleException` **422** ("area de trabalho nao
  encontrada"). A regra de raiz×substarefa da F13 não muda (área vale para qualquer tarefa).
- `GET /tasks?areaId={uuid}` filtra as tarefas pela área (combina com os filtros existentes de
  status/prioridade/título da lista).

### R3 — Área no frontend
- `types/area.ts` (`WorkArea {id, title, imageType, imageUrl}`) e `api/areas.ts`
  (`listAreas`, `createArea`, `updateArea`, `deleteArea`, `áreaImageUrl(id)`) chamando
  `POST/PUT` com `FormData` (multipart).
- `TaskForm`: select "Área de trabalho" carregado de `listAreas` (opção "Sem área"); valor
  inicial `task.areaId` na edição; envia `areaId` no submit.
- `TaskDetail`: linha "Área" (nome) quando a tarefa tem área (título resolvido pelo mapa de
  áreas carregado uma vez na página).

### R4 — Páginas de áreas
- Rota `/areas`: grade de cards (foto via `GET /areas/{id}/image`, título, contagem de tarefas)
  com criar/editar (nome + upload de foto) e excluir (confirmação; tarefas não são apagadas).
- Rota `/areas/:areaId`: cabeçalho com foto+nome da área e a lista das tarefas dela
  (`GET /tasks?areaId=`, mesmo layout/cards da lista de tarefas).
- Nav do `AppLayout` ganha item "Áreas".

## Fora de escopo (decisões)
- Nenhuma foto em miniatura servida na listagem `GET /areas` (bytes só via `GET /areas/{id}/image`).
- A contagem de tarefas do card pode ser preenchida pelo frontend (filtro por área) ou ignorada
  se custar demais — não há endpoint de contagem novo.
- **Remover só a foto** (sem trocar título) é feito com `PUT` enviando `image=nulo` + flag
  `removeImage=true` quando não houver upload novo.
- A área não aparece nas "Próximos prazos" nem nos KPIs do dashboard (fora do pedido).

## Sucesso
1. POST/PUT de área com multipart grava título e imagem; GET `/areas/{id}/image` devolve os
   bytes com o content type certo; DELETE de área não apaga as tarefas (area vira NULL).
2. Criar/editar tarefa com `areaId` grava e serializa `areaId`; `GET /tasks?areaId=` filtra.
3. Form da tarefa tem o select de área; detalhe mostra a área; página /areas faz CRUD com foto;
   /areas/:areaId lista as tarefas da área.
4. Gates: `mvn test`, `npx oxlint`, `npx tsc -b`, `npx vitest run`, `npx vite build` verdes.