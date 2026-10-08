# spec.md — F08-visual-azul

> Feature criada em 2026-10-07 a pedido do usuário: "quero o front muito parecido" com o mockup
> `Prévia · AI Task Manager.html` (arquivo local na raiz, **fora do git** por decisão do usuário).
> A imagem de referência usa a família azul ("estilo Azul"), navegação lateral e dashboards ricos.
> Esta feature só toca o frontend — **nenhum contrato de API muda**; os dados vêm do backend real.

## Objetivo

Deixar o frontend visualmente muito próximo do mockup, preservando comportamento e textos.
A prévia é interativa (HTML/CSS/JS) e serve de referência de visual, não de funcionalidade: aqui o
app usa dados reais e os componentes React existentes.

## Mapeamento: seção do mockup → quebra a reproduzir

| Seção da prévia | Hoje (F07) | Mudança |
| --- | --- | --- |
| Tokens e tipografia | fundo cinza `#f5f6f8`, sem Nunito, raio 6px | família azul (`#edf3fb`/`#f8fbff`/`#cfdcf0`/`#0b1f4d`/`#475b8a`), fonte **Nunito** (Google Fonts + fallback), `--radius: 8px`, espaços `--space-5/6`, tons novos (`--color-ai-*`, `--navy*`, `--sky`, `--color-success`) |
| Navegação | cabeçalho topo (logo à esquerda + links) | **sidebar navy** 240px com logo (quadrado skew azul-céu), links Dashboard/Tarefas/Assistente, ativo com borda esquerda `sky`; em telas ≤760px vira **barra inferior fixa** com borda superior; `skip-link` "Pular para o conteúdo" |
| Botões e campos | sem `min-height` | `min-height: 44px`; ação principal (Nova tarefa, submeter) em accent; `.button--danger` vermelho; campos largura cheia, fundo surface |
| Badges | pill (999px), prioridade sem cor própria | raio 4px, peso 700; status: `todo` muted/surface-2, `in_progress` accent/`#e0e8ff`, `done` success/`#dcf5ea`; prioridade vira `badge--prioridade-{low,medium,high}` (low success, medium `#3b4f86`/`#e3eaf8`, high danger/`#fde7e7`); `subtaskCount` com rótulo (já existe) |
| Dashboard | só os 5 indicadores | **hero card** (frase "Você tem X em aberto, Y com alta prioridade, Z atrasadas", % concluído + barra com `role=progressbar`, atalhos "Ver tarefas" e "Perguntar ao assistente"), indicadores em cartões sem gap (grade 1px), colunas: **Distribuição por status** (pilha + legenda), **Abertas por prioridade** (barras), **Próximos prazos** (lista com data `dd/mm/aaaa` e "Atrasada" em vermelho) |
| Lista de tarefas | prazo em ISO cru, sem formatação | linha com `Prazo: dd/mm/aaaa` (helper `formatarDataBR`); item com `min-height: 64px`, hover `#eaf1fc`; risco em título das concluídas (`:has(.badge--done)`); lista como cartão unificado com divisor entre itens |
| Detalhe | já tem meta, subtarefas e diálogo | sem mudança estrutural; somente ganha o prazo formatado se exibir cru e as cores novas |
| Painel IA | borda padrão | painel com `border-left: 4px solid sky` e título em `--color-ai-text`; botões contornados em azul-céu; resultado com fundo `--color-ai-soft` + borda `--color-ai-border`; primeiro botão das ações sólido azul-céu ("Aplicar à tarefa") |
| Assistente | bolha do assistente cinza com borda | bolha do assistente em `--color-ai-soft` com `border-left: 4px solid --color-ai` e raio `0 var(--radius) var(--radius) 0`; bolha do usuário em accent com raio `var(--radius) var(--radius) 0 var(--radius)`; autoria sem uppercase, peso 700; "Digitando..." em bolha azul-céu; botão Enviar em `--color-ai` |

## Critérios de aceite (por task)

- **T-F08-01 (base/tokens)**: tokens da prévia aplicados; Nunito carregado (`index.html` link do Google Fonts); botões/campos com `min-height: 44px`; badges com raio 4px e as cores da prévia; `prefers-reduced-motion` e `:focus-visible` (3px accent, e `sky` dentro da sidebar); nada quebrado no build.
- **T-F08-02 (navegação)**: sidebar navy à esquerda com logo; link ativo com borda `sky`; em ≤760px o menu vira barra inferior fixa com `safe-area-inset-bottom`; `skip-link` presente e focado ao tab; rotas seguem `/`, `/tasks`, `/assistente`.
- **T-F08-03 (dashboard)**: hero com frase calculada dos dados reais, % concluído e barra acessível, atalhos que navegam (Link para `/tasks` e `/assistente`); 5 indicadores; bloco "Distribuição por status" (pilha + legenda); "Abertas por prioridade" (barras); "Próximos prazos" (5 abertas com prazo, ordenadas, com "Atrasada" em vermelho quando aplicável); sem divisão por zero com total 0; teste atualizado e verde.
- **T-F08-04 (lista)**: prazo formatado `dd/mm/aaaa` com "Prazo:"; badges de prioridade coloridos; título de concluída riscado; dicionário `formatarDataBR` usado também no dashboard.
- **T-F08-05 (IA/assistente)**: cores azul-céu nos três painéis e nas bolhas; raios e autoria conforme a prévia; testes existentes seguem verdes.
- **T-F08-06 (docs)**: **um único commit de docs** resumindo a F08 (sem docs por task), atualizando STATE, matriz RF-21/RF-22 e este `tasks.md`; sem crescimentos enormes — só pontos importantes.

## Fora de escopo

- Mudar contratos da API, backend, textos de UI em português (mantidos), os tipos `Task`/`TaskSummary` (mantidos — `highPriority` = contagem do backend, ≠ "HIGH entre as abertas" do mockup).
- Reproduzir os **dados de exemplo** do mockup (nomes/mensagens fictícias) — o app usa dados reais.
- Versionar a prévia (`.gitignore` já cobre `Prévia*.html`).