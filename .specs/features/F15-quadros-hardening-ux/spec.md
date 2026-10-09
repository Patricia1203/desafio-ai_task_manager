# spec.md — F15-quadros-hardening-ux

> Feature criada em 2026-10-09 a pedido do usuário: endurecer o upload/listagem da
> **foto dos quadros** (F14) e ajustar a **UX de Tarefas e Quadros** (filtros,
> breadcrumbs, origem da tarefa, formulário de foto e agenda).

## Objetivo
Fechar as arestas de segurança e desempenho da foto dos quadros e corrigir
pontos de usabilidade levantados na revisão: filtros das tarefas na ordem
certa e sempre visíveis, uma tela dedicada "Todas as tarefas", breadcrumbs sem
o Dashboard quando se está dentro de Tarefas/Quadros, rastreio da origem
(quadro → tarefa), formulário de foto que recusa arquivo inválido antes do
upload e o modal da agenda com layout consistente.

## Requisitos

### R1 — Hardening da foto do quadro (backend)
- O content type declarado tem de estar na **whitelist** `image/png`,
  `image/jpeg`, `image/webp`, `image/gif` **e** bater com os **magic bytes** do
  arquivo; SVG e afins ficam de fora (XSS armazenado).
- Arquivo acima de **5 MB** é recusado antes de gravar (mesmo teto do
  `spring.servlet.multipart`); erro do multipart vira **413** (`ProblemDetail`),
  não 500.
- `GET /areas/{id}/image` responde com `X-Content-Type-Options: nosniff` e
  `Content-Security-Policy: default-src 'none'`.

### R2 — Listagem de quadros sem os bytes (backend)
- `GET /areas` (com ou sem `?title=`) usa uma **projeção** `{id, title,
  imageType}` e não seleciona a coluna `bytea image` (N quadros não carregam
  N × 5 MB).

### R3 — Filtros e tela de todas as tarefas (frontend)
- `TaskList`: o filtro por **título** vem **antes** do filtro por **status** e
  fica **sempre visível**.
- `TasksPage` (`/tasks`) mantém a agenda e ganha o link **"Ver todas as
  tarefas"** para a rota nova `/tasks/todas` (lista completa: filtros + lista,
  sem agenda).
- `AreaDetailPage` lista as tarefas com **filtro por status** e, com o
  formulário de criar aberto, **não** mostra o aviso de lista vazia.

### R4 — Navegação e foto (frontend)
- Breadcrumbs de **Tarefas** e **Quadros** não começam mais em "Dashboard";
  vindo de um quadro, a tarefa mostra `Quadros / <área> / <tarefa>`.
- `AreasPage`: o campo de foto aceita apenas a whitelist e **recusa no cliente**
  formato inválido ou arquivo acima de 5 MB.

## Fora de escopo (decisões)
- Sem redimensionamento/thumbnail no servidor: continua gravando os bytes
  originais (só o tipo e o tamanho são validados).
- A agenda/calendário permanece em `/tasks`; a página "Todas as tarefas" não
  tem agenda.
- Sem paginação na tela "Todas as tarefas" (busca até 100 itens, mesmo
  comportamento do painel).

## Sucesso
1. Upload com tipo fora da whitelist ou bytes que não batem → 422; acima de
   5 MB no multipart → 413; listagem não carrega bytes; resposta de imagem com
   `nosniff` + CSP.
2. `/tasks` reordena/filtra e leva a `/tasks/todas`; quadro filtra por status e
   abre a tarefa com a origem no breadcrumb; formulário de foto valida no
   cliente.
3. Gates: `mvn test`, `npx oxlint`, `npx tsc -b`, `npx vitest run`,
   `npx vite build` verdes.
