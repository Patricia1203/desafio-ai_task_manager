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

### T-F08-09 — Card de criar/editar mais largo (vazio proporcional menor)
- **Status:** done (commit `66fa92c`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-07
- **Arquivos (alterar):** somente `frontend/src/index.css` (regra `.task-form`)
- **O que fazer:** o card centralizado de 560px ainda deixa muito vazio nas laterais. Aumentar proporcionalmente para os lados: `max-width: 820px` (mantendo `width: 100%` e `margin-inline: auto`), reduzindo o vazio de cada lado para ~12% da seção em 1440px e ~7% em 1280px; em telas menores continua preenchendo a largura disponível.
- **Pronto quando:** formulário de criar/editar ocupa ~76% da seção em desktop com margens laterais simétricas e pequenas.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.

### T-F08-11 — Voltar em criar/editar e breadcrumb na página de tarefas
- **Status:** done (commit `9dfcffe`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-01
- **Arquivos (alterar):** `frontend/src/pages/TasksPage.tsx` (breadcrumb com `Link`, botão Voltar acima do form) e `frontend/src/index.css` (regras `.breadcrumb*`)
- **O que fazer:** em criar/editar, o usuário pediu um botão para voltar (o "Cancelar" fica só no fim do form): acrescentar botão "Voltar" acima do form, que volta para o detalhe em edição ou para a lista em criação. Adicionar breadcrumb na página de tarefas: `Dashboard / Tarefas` (com link ao Dashboard); em criando/edição mostram `Tarefas` como link para a lista e o sub-nível atual (`Nova tarefa`/`Editar tarefa`/título da tarefa) precisa de `aria-current="page"`. Só na página de tarefas (dashboard e assistente não ganham breadcrumb).
- **Pronto quando:** navegar criar/editar e voltar funciona; breadcrumb aparece só em `/tasks` com vínculo ao Dashboard e ao sub-estado; testes verdes.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.

### T-F08-13 — Seta de voltar ao lado do título (no lugar do botão Voltar)
- **Status:** done (commit `664bb49`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-11
- **Arquivos (alterar):** `frontend/src/pages/TasksPage.tsx` (cabeçalho) e `frontend/src/index.css` (`.tasks__titulo`, `.tasks__voltar--seta`)
- **O que fazer:** o usuário pediu seta em vez do botão: remover o botão "Voltar" acima do form e a seta `←` ao lado do título "Tarefas" (`.tasks__voltar--seta`, 44px com aria-label "Voltar"), agrupando seta + título em `.tasks__titulo` (flex) para ficarem juntos à esquerda; remover também o "Voltar para a lista" do detalhe (a seta cobre o retorno). O clique da seta usa o mesmo `voltar()`.
- **Pronto quando:** seta colada ao título em criar/editar/detalhe e o clique retorna ao destino certo; sem botões de texto duplicados.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.

### T-F08-15 — Seta de voltar um pouco menor (44px → 36px)
- **Status:** done (commit `0c53b71`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-13
- **Arquivos (alterar):** somente `frontend/src/index.css` (`.tasks__voltar--seta`)
- **O que fazer:** o usuário pediu o botão da seta um pouco menor: reduzir `width`/`min-height` de 44px para 36px e fonte de 1.5rem para 1.25rem, mantendo o azul accent e a seta branca.
- **Pronto quando:** seta ~36px visivelmente menor, ainda azul, clicável e colada ao título.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.

### T-F08-16 — Breadcrumb "Tarefas" volta para a lista ao clicar dentro de uma tarefa
- **Status:** done (commit `f2edb5a`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-11
- **Arquivos (alterar):** somente `frontend/src/pages/TasksPage.tsx` (link "Tarefas" do breadcrumb)
- **O que fazer:** dentro de uma tarefa (detalhe) o link `Tarefas` do breadcrumb (`Link to="/tasks"`) não voltava para a lista porque, já estando em `/tasks`, o React Router não remonta a página e o estado `modo` continuava `'detalhe'`. Adicionar `onClick={() => setModo('lista')}` ao link.
- **Pronto quando:** em detalhe/criar/editar, clicar no breadcrumb "Tarefas" mostra a lista (e muda o estado interno do modo).
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.

### T-F08-07 — Card do formulário de criar/editar centralizado
- **Status:** done (commit `6ead194`, 2026-10-08)
- **Reqs:** RF-21, RF-22
- **Depends on:** T-F08-01
- **Arquivos (alterar):** somente `frontend/src/index.css` (regra `.task-form`)
- **O que fazer:** o card de 560px fica colado à esquerda deixando um vazio grande à direita. Centralizar o card com `width: 100%; max-width: 560px; margin-inline: auto`, mantendo o tamanho compacto da prévia mas simétrico na tela (em telas ≤760px continua ocupando toda a largura, sem efeito).
- **Pronto quando:** o formulário de criar/editar aparece centralizado; sem alteração de comportamento/testes.
- **Gate:** `npx vitest run` verde; `npx oxlint` limpo; `npx tsc -b && npx vite build` OK.