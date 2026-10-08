# design.md — F14-areas-de-trabalho

## Contrato de API (context-path `/api`)

```
GET  /areas                     → 200 [{ id, title, imageType }]
POST /areas                     multipart(title, image?) → 201 { id, title, imageType }
PUT  /areas/{id}                multipart(title?, image?, removeImage?) → 200 { id, title, imageType }
DELETE /areas/{id}              → 204
GET  /areas/{id}/image          → 200 bytes (Content-Type = imageType) | 404
GET  /tasks?areaId={uuid}       → PageResponse filtrada (combina com status/prioridade/titulo)
POST/PUT /tasks                 passam a aceitar areaId (UUID ou null)
GET  /tasks e /tasks/{id}       TaskResponse ganha areaId (UUID ou null)
```

- `WorkAreaResponse(String id, String title, String imageType)` — **sem os bytes** na listagem.
- `AppliedAreaRequest` para as tarefas: campo `areaId` em `CreateTaskRequest`/`UpdateTaskRequest`
  (UUID opcional). `PUT` com `areaId = null` remove a área.
- Multipart `PUT`: `title` opcional (mantém), `image` substitui quando enviado, `removeImage=true`
  (com `image` ausente) remove a foto.
- `imageType` é o `Content-Type` do arquivo (ex.: `image/png`); gravado ao lado dos bytes.

## Backend

### Migração (V7__create_work_areas.sql)
```sql
CREATE TABLE work_areas (
  id         UUID PRIMARY KEY,
  title      VARCHAR(100) NOT NULL,
  image      BYTEA,
  image_type VARCHAR(50),
  created_at TIMESTAMPTZ NOT NULL
);
ALTER TABLE tasks ADD COLUMN area_id UUID REFERENCES work_areas(id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_area_id ON tasks(area_id);
```

### Pacote `area`
- `area/domain/WorkArea` — entidade: `id` (UUID gerado em Java, padrão do projeto), `title`,
  `image` (byte[]), `imageType`, `createdAt`. Regras: `requireTitle` (trim, `BusinessRuleException`
  "o titulo da area e obrigatorio", máx 100), `requireImageType` quando houver bytes
  (tem de começar com `image/` — 422 "formato de imagem nao suportado"), `setImage(bytes, type)`.
- `area `**infra**`: `WorkAreaRepository extends JpaRepository` + `WorkAreaMapper` (entidade →
  `WorkAreaResponse`).
- `area/application/WorkAreaService`: `list()` (por título asc), `create(title, bytes, type)`,
  `update(id, title, bytes, type, removeImage)`, `delete(id)` (404 se não existe), `findImage(id)`
  → `{bytes, type}` (404 sem foto). `Atualizacao de area: se nenhum (title, image, removeImage)
  for enviado, mantém o estado atual; remoção de foto só via removeImage.
- `area/api/WorkAreaController` + `WorkAreaMapper` — endpoints de `/areas`. Multipart: parâmetros
  `@RequestParam String title` + `@RequestPart(required=false) MultipartFile image` + flag
  `removeImage`. Resposta com `ResponseEntity.created(...)`.
- Em `application.yml`: `spring.servlet.multipart.max-file-size: 5MB` e `max-request-size: 6MB`.

### Tarefas
- `Task` ganha `WorkArea area` (`@ManyToOne(fetch = LAZY)`) e `setArea(WorkArea)`
  (aceita `null`). `TaskMapper`/`TaskResponse` expõem `areaId` (UUID do proxy ou `null`).
- `TaskCommand` ganha `areaId` (UUID opcional). `TaskService`:
  - `create`/`createSubtask`/`update`: resolve a área (`areaRepository.findById`),
    **422** "area de trabalho nao encontrada" se o id não existir, e chama `setArea`.
  - `update` com `areaId == null` chama `setArea(null)` (substitui conteúdo).
- `TaskFilter` ganha `areaId`; `toSpecification` adiciona `(root, q, cb) -> root.get("area").get("id")...`
  quando presente (áreas NOT NULL aqui, então sem null-safe).

### Testes (estratégia, sem inventar contagem)
`WorkAreaServiceTest` (CRUD + regras de title/image/removeImage/404), `WorkAreaControllerTest`
(slice web, multipart: criar/editar com e sem imagem, GET image 200/404, delete),
`TaskServiceTest` (gravar com area existente/inexistente 422/remover no PUT),
`TaskControllerTest` (filtro `areaId`), `TaskQueryTools` e summary: regra de contagem com área
não muda.

## Frontend

- `types/area.ts`: `WorkArea { id: string; title: string; imageType: string | null; imageUrl: string }`
  (`imageUrl` = `/api/areas/{id}/image` quando há `imageType`).
- `api/areas.ts`: `listAreas()`, `createArea(form: FormData)`, `updateArea(id, form)`,
  `deleteArea(id)`, `buscaArea(id)` (ajuda da lista) — usa `client.ts` com `FormData` (sem
  `Content-Type` manual, o browser envia o boundary).
- `TaskForm`: select "Área de trabalho" (vazio = "Sem área") com as opções de `listAreas`
  carregadas no mount; `initialValues.areaId`; manda `areaId: string | null` no submit.
- `TaskDetail`: linha "Área" (nome) quando há área — título resolvido pelo mapa de áreas que a
  página carrega com `listAreas()`.
- `pages/AreasPage.tsx` (`/areas`): busca `listAreas`; card com foto (`<img src={imageUrl}>`),
  título e ações; formulário de nova/edição (nome + `<input type="file" accept="image/*">` +
  remover foto); exclusão com `role="alertdialog"` avisando que as tarefas são preservadas.
  Sem contagem por card (sem endpoint novo) — fora de escopo.
- `pages/AreaDetailPage.tsx` (`/areas/:areaId`): busca a área + `listTasks({ areaId })`
  (client de tasks com o filtro) e reusa o componente de cards/linhas da lista de tarefas.
- `AppLayout.tsx`: nav ganha "Áreas" (`/areas`).
- Testes: `api/areas.test.ts`, `TaskForm.test` (select presente, valor inicial e submit com
  `areaId`), `AreasPage.test` (lista, criar com FormData mockado, excluir com diálogo),
  `AreaDetailPage.test` (filtro chamado com o id).

## Arquivos
- Backend: `db/migration/V7__create_work_areas.sql` (novo), `area/**` (novo), alterações em
  `task/domain/Task`, `task/application/{TaskService,TaskCommand,TaskFilter}.java`,
  `task/api/dto/*`, `task/api/TaskController`, `application.yml`, testes.
- Frontend: `types/area.ts`, `api/areas.ts` (novos), `TaskForm.tsx`, `TaskDetail.tsx`,
  `pages/{AreasPage,AreaDetailPage}.tsx` (novos), `AppLayout.tsx`, `index.css`, testes.