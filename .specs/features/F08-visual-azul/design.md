# design.md — F08-visual-azul

Decisões de implementação a partir do mockup `Prévia · AI Task Manager.html` (referência visual,
não versionado). Fonte: `<link/fontes/cores>` e classes do arquivo local.

## 1. Tokens (novos valores em `index.css`)

| Token | Valor | Uso |
| --- | --- | --- |
| `--color-bg` | `#edf3fb` | fundo da página |
| `--color-surface` | `#f8fbff` | cartões, formulários, bolhas |
| `--color-surface-2` | `#e3edf9` | hover de botões, fundo de badges neutros |
| `--color-border` | `#cfdcf0` | bordas suaves |
| `--color-border-strong` | `#aebfe0` | botões/campos |
| `--color-text` | `#0b1f4d` | texto |
| `--color-muted` | `#475b8a` | texto secundário |
| `--color-accent` | `#0b4fd6` | primária (hero, submit, botão do usuário) |
| `--color-accent-hover` | `#0a3fae` | hover |
| `--color-danger` | `#c62f2f` | perigo/erros/alta |
| `--color-success` | `#0b7a55` | concluída/baixa |
| `--sky` | `#5cc8ff` | marca do logo, foco na sidebar, ativo |
| `--color-ai` | `#0a74a0` | IA/assistente (azul-céu) |
| `--color-ai-text` | `#075f83` | títulos/IA |
| `--color-ai-hover` | `#075f83` | hover IA |
| `--color-ai-soft` | `#e0f3fb` | fundo de bolha/resultado IA |
| `--color-ai-border` | `#a9dcf0` | borda IA |
| `--navy` | `#0a2b7a` | sidebar |
| `--navy-2` | `#123a9c` | hover/ativo da sidebar |
| `--navy-line` | `#214bb5` | borda da sidebar |
| `--navy-text` | `#eef3ff` | texto na sidebar |
| `--navy-muted` | `#bccaf3` | link inativo da sidebar |
| `--radius` | `8px` | arredondamento base |
| espaços | `--space-1..6` (0.25rem…3rem) | escala |

## 2. Tipografia

- **Nunito** (400–800) via Google Fonts no `index.html` (`<link rel="stylesheet" href="...Nunito...">`),
  com fallback `system-ui...` no `body` — idêntico à prévia. Sem bundle, sem dependency extra.
- `--font` centralizado no `:root`.

## 3. Estrutura do layout

- `.app` = grid `240px minmax(0,1fr)`; `main` com `max-width: 1180px`, padding `2rem 3rem 4rem`
  (≤1000px: `1rem 1rem 4rem`).
- Sidebar `.app__header` (mantida a tag `<header>` do `AppLayout`): sticky, coluna, logo
  `.app__title` com quadrado `skewX(-12deg)` em `--sky`; nav com `min-height: 48px`, `.active`
  com `border-left: 4px solid var(--sky)` e fundo `--navy-2`.
- ≤760px: sidebar fixa na base (`inset: auto 0 0 0`), row, logo oculto, links centrais com
  `border-top`, `padding-bottom: env(safe-area-inset-bottom)`.
- Acessibilidade: `skip-link` (topo `-4rem`, foco traz ao `top: 1rem`), `:focus-visible`
  `3px` accent (e `--sky` dentro da sidebar), `prefers-reduced-motion`.

## 4. Botões, campos e badges

- Botões/campos `min-height: 44px`, raio `var(--radius)`, borda `--color-border-strong`,
  fundo `--color-surface`; `button:disabled { opacity: .6; cursor: not-allowed; }`.
- Primário: `.tasks__header > button` e `button[type='submit']` (accent, texto branco,
  hover `--color-accent-hover`); `.button--danger` vermelho.
- Badges: raio 4px, `font-size: .875rem`, peso 700; variantes
  `badge--in_progress`, `badge--done`, `badge--subtarefas` e
  `badge--prioridade-{low,medium,high}` (novas classes aplicadas nos componentes).
- `:has(.badge--done)` risca o título da linha concluída.

## 5. Dashboard

- Dados: `getSummary()` (indicadores, hero, pilha por status) + `listTasks({size:50})`
  (prioridades entre abertas e próximos prazos).
- Frase do hero: `Você tem {pending+inProgress} tarefas em aberto, {highPriority} com alta
  prioridade. {atrasadas} {estão/está} atrasadas.` — `atrasadas` = abertas com `dueDate < hoje`
  (data local `Brazil`/`America/Sao_Paulo`, mesma intenção do mockup via `TODAY`).
- `% concluído` = `round(done/total*100)`, `0` quando `total === 0`.
- "Próximos prazos": 5 abertas com `dueDate` (null por último), ordem crescente; linha atrasada
  ganha "Atrasada · " em vermelho.
- Atalhos do hero usam `Link` de `react-router-dom` para `/tasks` e `/assistente`.
- Cards dos indicadores unificados em grade de 1px (borda do container como divisor), quinto
  card com valor em danger (alta prioridade).

## 6. Lista e detalhe

- `formatarDataBR(iso)`: `iso.slice(0,10).split('-').reverse().join('/')` → `dd/mm/aaaa` ou `''`
  se nulo (novo `frontend/src/utils/date.ts`, usado em `TaskList` e `DashboardPage`).
- Item da lista: `min-height: 64px`, coluna, gap pequeno; meta com badge de status, prioridade
  (com cor), subtarefas e `Prazo: dd/mm/aaaa`.
- Detalhe: estrutura atual preservada (meta com Prioridade/Prazo/Criada em, status, Editar,
  Excluir com diálogo, bloco de subtarefas e vínculo do pai). Sem mudança de dom.

## 7. IA (painel) e assistente

- `.ai-panel`: `border-left: 4px solid var(--color-ai)`; título em `--color-ai-text`.
- Botões do painel contornados em `--color-ai`; primeiro botão das ações do resultado
  (`Aplicar à tarefa`) sólido `--color-ai`.
- `.ai-resultado`: fundo `--color-ai-soft`, borda `--color-ai-border`, cabeçalho `h5` sem peso
  extra; itens da lista em surface; `input[type=checkbox]` com `accent-color: var(--color-ai)`.
- Assistente: `.assistant__bolha` (assistente) soft azul com `border-left: 4px solid --color-ai`
  e raio `0 var(--radius) var(--radius) 0`; `.assistant__bolha--usuario` accent sólido; autoria
  `0.875rem` peso 700 (sem uppercase); bolhas com `max-width: min(80%, 72ch)`, lista com
  `max-height: min(60vh, 640px)`; `Digitando...` em bolha azul-céu; botão Enviar em `--color-ai`.

## 8. Testes

- `DashboardPage.test.tsx`: mock ganha `listTasks`; KPI verificados dentro do seu card (ex.
  `getByText('Pendentes').closest('li')` → contém "5"), evitando colisão com número igual em
  pilha/prioridade/prazos; novos asserts para hero, distribuição, prioridades e próximos prazos.
- Demais testes: sem fixture textual nova relevante (CSS não quebra vitest); conferir
  `TaskList`/`TaskDetail`/`AiPanel`/`AssistantPage` verdes no gate.

## 9. Gatilho de verdade

- A prévia mostra **dados de exemplo** (nomes como "Definir roadmap do trimestre"); o front
  continua com dados reais da API. O parecido é de *visual*, não de *conteúdo*.