# tasks.md — F15-quadros-hardening-ux

> Feature de ajuste (2026-10-09). Um commit por task. Requisitos derivados dos
> IDs existentes: `RF-25 (ajuste)` para a foto dos quadros e `RNF-20` para os
> headers/413; `RF-12` (lista filtrável) e `RNF-03` (navegação) para o frontend.

| Task | Descrição | Reqs | Commit |
|------|-----------|------|--------|
| T-F15-01 | Backend: hardening da foto (whitelist + magic bytes + 5 MB + 413) e listagem de quadros via projeção sem `bytea`; headers `nosniff`/CSP na imagem | RF-25 (ajuste), RNF-20 | `8d97650` |
| T-F15-02 | Frontend: filtro por título antes do status e sempre visível; rota/tela "Todas as tarefas"; link em `/tasks`; breadcrumb de Tarefas | RF-12, RNF-03 | `2300a14` |
| T-F15-03 | Frontend: breadcrumb de Quadros, filtro de status na área, origem `?quadro=`, vazio escondido, validação de foto (formato/5 MB) e ajustes de CSS (foto/dialog/agenda) | RF-25 (ajuste), RNF-03 | `d6941fb` |
| T-F15-04 | docs: registra a F15 em `spec.md`/`design.md`/`tasks.md`, `STATE.md` e `TRACEABILITY.md` | DOC-01 | este commit |

## T-F15-01 — Backend
- [x] `WorkArea#setImage`: whitelist `image/png|jpeg|webp|gif` + magic bytes + `MAX_IMAGE_SIZE` 5 MB (`BusinessRuleException` 422).
- [x] `WorkAreaSummary` (record `id,title,imageType`, `hasImage()` derivado) + `WorkAreaRepository` com constructor expression (sem `bytea`, sem `JpaSpecificationExecutor`).
- [x] `WorkAreaService.list()` → `List<WorkAreaSummary>`; `list(title)` escapando `\ % _`.
- [x] `WorkAreaResponse.of(WorkAreaSummary)`; `GlobalExceptionHandler` mapeia `MaxUploadSizeExceededException` → 413.
- [x] `GET /areas/{id}/image` com `nosniff` + `Content-Security-Policy: default-src 'none'`.
- [x] Testes: `WorkAreaServiceTest`, `WorkAreaControllerTest`, `ErrorProbeController` + `GlobalExceptionHandlerTest`.
- Gates: `mvn test` = 333/0.

## T-F15-02 — Frontend: Tarefas
- [x] `TaskList` com filtro por título primeiro e sempre visível + prop `emptyMessage`.
- [x] `TasksAllPage` + rota `/tasks/todas`; link "Ver todas as tarefas" em `/tasks`.
- [x] Breadcrumb de `TasksPage` sem Dashboard e com origem no quadro.
- [x] Testes: `TaskList`, `TasksPage`, `TasksAllPage`; `AsyncState` respeita `emptyMessage` vazio.

## T-F15-03 — Frontend: Quadros/agenda
- [x] `AreasPage`: breadcrumb sem Dashboard, `accept` na whitelist e validação de formato/5 MB.
- [x] `AreaDetailPage`: breadcrumb, filtro de status, `?quadro=` e vazio escondido.
- [x] CSS: foto, `.dialog__acoes`, `.schedule__resumo`.
- [x] Testes: `AreasPage` (breadcrumb + recusa de foto).
- Gates: `oxlint` limpo, `tsc -b` + `vite build` exit 0, `vitest run` = 107 (15 arquivos).

## T-F15-04 — Documentação
- [x] `spec.md`, `design.md`, `tasks.md`.
- [x] `STATE.md` (Task atual) e `TRACEABILITY.md` (linha `RF-25 (F15)`).
