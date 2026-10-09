# design.md — F15-quadros-hardening-ux

Decisões técnicas e alternativas descartadas.

## 1. Validação da foto: whitelist + magic bytes

`WorkArea#setImage(byte[], String)` concentra a regra (mesmo lugar de
`requireTitle`), na fronteira da entidade, antes de qualquer persistência:
1. `image == null` → `clearImage()` (PUT sem upload).
2. `image.length > MAX_IMAGE_SIZE` (5 MB) → `BusinessRuleException` (422).
3. O `contentType` é normalizado (trim/lowercase) e precisa estar na whitelist
   **e** bater com `detectImageType(bytes)` — assinaturas PNG
   (`89 50 4E 47 0D 0A 1A 0A`), JPEG (`FF D8 FF`), GIF (`GIF87a`/`GIF89a`) e
   WEBP (RIFF + `WEBP` nos bytes 8..11).

Por que magic bytes e não só o tipo declarado: o `Content-Type` do multipart é
escolhido pelo cliente; confiar só nele deixaria passar um `.html`/`.svg`
renomeado. Por que whitelist e não `image/*`: o `image/svg+xml` é `image/*` e
pode carregar script; como a foto é servida inline na mesma origem, um SVG
malicioso viraria XSS armazenado. O SVG fica de fora de propósito.

## 2. Magic bytes na entidade (e não num `Validator` à parte)

As regras de título já vivem em `WorkArea`; manter a imagem no mesmo lugar
evita um serviço que "esquece" de validar em um caminho novo e permite testar
sem subir contexto Spring. O teste de integração cobre o caminho HTTP.

## 3. 413 no `GlobalExceptionHandler`

O multipart estoura antes do controller (`MaxUploadSizeExceededException`);
sem um `@ExceptionHandler` próprio o `Exception.class` catch-all transformaria
um erro de cliente em 500. O handler devolve `ProblemDetail` com
`type=.../arquivo-muito-grande` e mensagem fixa (sem vazar o tamanho bruto).

## 4. Listar quadros sem os bytes: projeção fechada

`GET /areas` só precisa de `{id, title, imageType}`. Materializar a entidade
carrega a coluna `bytea image` (até 5 MB por quadro) sem necessidade. A solução
é um **record de projeção** (`area/infra/WorkAreaSummary`) preenchido por JPQL
com *constructor expression*:

```
select new ...WorkAreaSummary(a.id, a.title, a.imageType)
from WorkArea a [where lower(a.title) like :pattern escape '\'] order by a.title asc
```

O `hasImage()` é derivado de `imageType != null` — imagem e tipo são gravados
juntos (`setImage`/`clearImage`), então não é preciso expor um booleano no
banco. O `WorkAreaResponse` ganha um overload `of(WorkAreaSummary)`. A busca
por título mantém o escape `\ % _` em `patternDe`, agora aplicado no JPQL (o
`JpaSpecificationExecutor` deixou de ser necessário e foi removido).

Alternativa descartada: `@ElementCollection`/DTO com `@Query` nativo — o
constructor expression é suficiente e não acopla a nomes de coluna.

## 5. Headers de segurança na imagem

`X-Content-Type-Options: nosniff` impede o browser de "adivinhar" outro tipo e
executar; `Content-Security-Policy: default-src 'none'` bloqueia scripts e
recursos embutidos caso o conteúdo escape da whitelist. Mantidos o
`Cache-Control` de 1 dia e o `Content-Disposition: inline`.

## 6. Filtros de tarefas e a tela "Todas as tarefas"

O filtro por **título** é client-side (já existia no `TaskList`); o de
**status** é server-side via `listTasks({status})`. A ordem passou a ser título
→ status e o título ficou sempre visível (com a lista vazia ainda ajuda a
confirmar o filtro ativo). A rota `/tasks/todas` reaproveita `TaskList` e, ao
clicar, navega para `/tasks?tarefa=<id>` para abrir o mesmo `TaskDetail` — sem
duplicar o detalhe. A agenda continua só em `/tasks`.

## 7. Origem no breadcrumb

`AreaDetailPage` navega para `/tasks?tarefa=<id>&quadro=<areaId>`. O `TasksPage`
lê `quadro`, acha a área no mapa já carregado (`listAreas`) e troca a raiz do
breadcrumb de "Tarefas" para `Quadros / <área>`, preservando o restante
(pai/subtarefa). Breadcrumb de Tarefas/Quadros não começa mais em "Dashboard"
(o link do Dashboard segue na navegação lateral).

## 8. Campo de foto no cliente

`accept` lista a whitelist e `selecionarFoto` valida `type` e `size` antes de
enviar, limpando o input e mostrando a mensagem — poupa um round-trip que
terminaria em 422/413. O backend continua sendo a fonte da verdade.

## 9. Modal da agenda

Sem mudança de estrutura: `.dialog__acoes` vira `flex` com `gap` (alinhado aos
demais diálogos) e `.schedule__resumo` ganha `max-height`/`scroll` para dias
cheios não estourarem o dialog.
