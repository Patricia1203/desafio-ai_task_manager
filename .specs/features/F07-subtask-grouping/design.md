# design.md — F07-subtask-grouping

## Apresentação (decisão do usuário, 2026-10-07)
O enunciado §6 diz que as subtarefas "deverão poder ser apresentadas ao usuário e, preferencialmente,
adicionadas à aplicação como novas tarefas relacionadas à tarefa original", deixando a estrutura a
critério do candidato. Hoje o vínculo existe só no banco (`parent_id`), mas a listagem plana mistura
raiz e filhas e nada na UI indica o parentesco → parece que são tarefas independentes.

**Decisão (opção escolhida pelo usuário): lista só com tarefas-raiz; subtarefas agrupadas no detalhe
da original.**

Consequências:
- `GET /tasks` é a lista canônica de **tarefas-raiz**. Subtarefa não "aparece sozinha" na lista;
  ela vive dentro do detalhe da tarefa que a define (via decomposição IA).
- `GET /tasks/{id}` continua devolvendo qualquer tarefa pelo id (raiz ou filha) — é por onde o
  detalhe de uma subtarefa é aberto e de onde se navega de volta ao pai.
- `GET /tasks/{id}/subtasks` (já existente) é a fonte do bloco "Subtarefas (N)" no detalhe.

## Filtro de raízes na lista
`toSpecification` (TaskService) passa a **sempre** acrescentar `parent is null`. A lista é de raízes.
Antes o filtro só compunha status/prioridade e nenhum chamador de produção usa `findAll` além de
`GET /tasks`, então a mudança fica contida na listagem pública e em testes.

**Assimetria registrada:** o assistente (F04) consulta `TaskRepository` direto (JPQL própria), então
as ferramentas de contagem/listagem dele continuam vendo subtarefas. Mantido de propósito: mudar o
assistente reabriria o comportamento da F04 e não é o que a request pede. Fica como observação para
uma revisão futura decidir se "tarefa" no contexto do assistente deve ser raiz também.

## `subtaskCount` sem N+1
`TaskResponse` ganha `long subtaskCount` (0 quando a tarefa não tem subtarefas). Para não disparar uma
consulta por linha, o service busca a contagem agrupada numa única query
(`select t.parent.id, count(t) from Task t where t.parent is not null group by t.parent.id`) e a
injeção acontece no mapeamento da lista. O `TaskMapper.toResponse` aceita um `Map<UUID, Long>` opcional;
os demais usos (detalhe, subtasks, summary) passam nulo → conta 0.

Contrato segue inglês (decisão F06): o novo campo se chama `subtaskCount`.

## Frontend
- **Lista (TaskList):** quando `subtaskCount > 0`, a linha ganha o selo "N subtarefas" junto dos
  badges de status/prioridade.
- **Detalhe (TaskDetail):** ao abrir, carrega `GET /tasks/{id}/subtasks` (a chamada já existe no
  fluxo de exclusão) e renderiza o bloco "Subtarefas (N)" — cada item com título, status, prioridade
  e prazo, clicável para abrir a subtarefa. O aviso de exclusão passa a reusar essa lista já carregada
  (uma chamada só, sem duplicar).
- **Vínculo do pai (TaskDetail):** quando a tarefa aberta é uma subtarefa (`parentId` definido), o
  frontend busca `GET /tasks/{id}` do pai para obter o título e mostra o selo "Subtarefa de <título>",
  clicável para voltar ao pai. Uma chamada pontual, sem mudar o contrato do backend (o pai já sai como
  `parentId` no `TaskResponse`).

## Fora de escopo (decisões)
- Criar subtarefa manualmente na UI (hoje só a decomposição IA cria; o formulário não tem seletor de
  pai). Não pedido; fica como observação.
- Mudar as ferramentas do assistente para raiz-only (ver assimetria acima).