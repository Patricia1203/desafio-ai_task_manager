# tasks.md — F08-visual-azul

> Feature criada em 2026-10-07 a pedido do usuário ("quero o front muito parecido" com a prévia
> `Prévia · AI Task Manager.html`, na raiz, fora do git). Escopo: somente frontend. O usuário
> também pediu **menos commits de documentação** — a F08 tem **um único commit de docs** no final
> (T-F08-06), com só os pontos importantes, em vez de um commit de docs por task.

### T-F08-01 — Tokens, tipografia e formulários azuis
- **Status:** done (commit `83277b3`, 2026-10-07)
- **Reqs:** RF-21, RF-22
- **Depends on:** —
- **Arquivos (alterar):** `frontend/src/index.css` (tokens `:root`, base, botões, campos, badges, focus, skip-link), `frontend/index.html` (link do Google Fonts Nunito)
- **O que fazer:** substituir os tokens atuais pelos da prévia (seção 1 do design.md), raio 8px, fonte Nunito com fallback, `min-height: 44px` em botões/campos, badges com raio 4px e variantes de prioridade/status, `:focus-visible` 3px (e `sky` na sidebar), `prefers-reduced-motion`, skip-link base. O CSS por tela entra na task dela (menu → T-F08-02, dashboard → T-F08-03, lista/detalhe → T-F08-04, IA/assistente → T-F08-05).
- **Pronto quando:** tokens da prévia aplicados; fonte Nunito no `<head>`; botões/campos com 44px; nada quebrado.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde; `npx tsc -b && npx vite build` OK.

### T-F08-02 — Navegação em sidebar (com barra inferior no mobile)
- **Status:** done (commit `63d1454`, 2026-10-07)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-01
- **Arquivos (alterar):** `frontend/src/components/layout/AppLayout.tsx` (acrescenta `skip-link` e organiza logo/nav dentro da sidebar; classes existentes `.app__header/.app__title/.app__nav` reutilizadas), `frontend/src/index.css` (grid `.app` com 240px, navbar navy, ativo com `border-left: sky`, mobile ≤760px com barra inferior e `safe-area`)
- **O que fazer:** estrutura do mockup: logo + nav verticais na esquerda; `NavLink.active` ganha a borda `sky`; ≤760px o menu vira barra fixa embaixo com borda superior. Adicionar `skip-link` focado no `main`.
- **Pronto quando:** sidebar visível em desktop, barra inferior em mobile; link ativo destacado; skip-link funciona.
- **Gate:** `npx oxlint` limpo; `npx vitest run` verde; build OK.

### T-F08-03 — Dashboard azul (hero + indicadores + distribuição + próximos prazos)
- **Status:** done (commit `acf9859`, 2026-10-07)
- **Reqs:** RF-20, RF-21
- **Depends on:** T-F08-01
- **Arquivos (alterar):** `frontend/src/pages/DashboardPage.tsx`, `frontend/src/utils/date.ts` (novo `formatarDataBR`, usado aqui e na lista), `frontend/src/pages/DashboardPage.test.tsx`, `frontend/src/index.css` (regras `.dashboard__*`)
- **O que fazer:** hero card com frase calculada (`getSummary` + `listTasks({size:50})`), `% concluído` com `role=progressbar`, atalhos `Link` para as rotas; 5 indicadores; bloco "Distribuição por status" (pilha com 3 segmentos + legenda, a partir do summary); "Abertas por prioridade" (barras a partir das abertas em `listTasks`); "Próximos prazos" (5 abertas com prazo, ordem crescente, "Atrasada" em vermelho). Sem divisão por zero. Atualizar o teste para mockar `listTasks` e usar asserts à prova de colisão de número.
- **Pronto quando:** herói e os três blocos renderizam com dados reais; atalhos navegam; teste verde.
- **Gate:** `npx vitest run` verde com o teste de dashboard atualizado; `npx oxlint` limpo; build OK.

### T-F08-04 — Lista: prazo formatado e badges de prioridade coloridos
- **Status:** done (commit `3adc520`, 2026-10-07)
- **Reqs:** RF-21
- **Depends on:** T-F08-01
- **Arquivos (alterar):** `frontend/src/components/task/TaskList.tsx`, `frontend/src/components/task/TaskDetail.tsx` (badge de prioridade das subtarefas), `frontend/src/utils/date.ts`, `frontend/src/index.css` (regras `.task-list__item`, `.task-list__prazo`, risco via `:has(.badge--done)`)
- **O que fazer:** usar `formatarDataBR` no `Prazo:` da linha e das subtarefas; acrescentar a classe `badge--prioridade-{low,medium,high}` nas linhas (lista e subtarefas do detalhe); garantir que o título riscado funcione (CSS).
- **Pronto quando:** prazo exibido `dd/mm/aaaa`; badges de prioridade coloridos; testes existentes verdes.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; build OK.

### T-F08-05 — IA azul-céu e assistente (bolhas, autoria, Digitando...)
- **Status:** done (commit `5a8b653`, 2026-10-07)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-01
- **Arquivos (alterar):** somente `frontend/src/index.css` (regras `.ai-*`, `.assistant__*`); rever comportamento se algum teste depender de classe
- **O que fazer:** painel/botões/resultados de IA em azul-céu; bolha do assistente soft com borda esquerda `sky` e raio `0 r r 0`; bolha do usuário accent com raio `r r 0 r`; autoria sem uppercase; `Digitando...` na bolha azul-céu; botão Enviar em `--color-ai`; checkbox com `accent-color` ai. Nenhum texto muda; componentes não são tocados (só CSS).
- **Pronto quando:** visual dos três painéis/bubbles segue a prévia; `AssistantPage.test`/`AiPanel.test` verdes.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; build OK.

### T-F08-06 — Docs em um único commit (resumo)
- **Status:** done (2026-10-07)
- **Reqs:** DOC-01
- **Depends on:** T-F08-01, T-F08-05
- **Arquivos (alterar):** `.specs/project/STATE.md` (entrada única da F08), `.specs/project/TRACEABILITY.md` (RF-21/RF-22 com o resumo e último SHA), este `tasks.md` (status done dos itens)
- **O que fazer:** **um único commit** `docs: ...` cobrindo toda a F08 com apenas os pontos importantes (visual seguindo a prévia; sidebar/mobile; dashboard rico; prazo formatado; núcleo azul-céu da IA/assistente; fonte Nunito; gates finais). Sem commits de docs por task.
- **Pronto quando:** um só commit de docs detalha a F08 em poucas linhas; STATE/TRACEABILITY/tasks.md coerentes.
- **Gate:** `git log --oneline` com 1 commit `docs:` referente à F08.

### T-F08-07 — Card do formulário de criar/editar centralizado
- **Status:** done (commit `6ead194`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-01
- **Arquivos (alterar):** somente `frontend/src/index.css` (regra `.task-form`)
- **O que fazer:** o card de 560px fica colado à esquerda deixando um vazio grande à direita. Centralizar o card com `width: 100%; max-width: 560px; margin-inline: auto`, mantendo o tamanho compacto da prévia mas simétrico na tela (em telas ≤760px continua ocupando toda a largura, sem efeito).
- **Pronto quando:** o formulário de criar/editar aparece centralizado; sem alteração de comportamento/testes.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.