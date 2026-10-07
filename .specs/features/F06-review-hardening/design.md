# design.md — F06-review-hardening

Decisões de contrato/arquitetura para a correção das pendências. Referências ao código atual estão com `arquivo:linha`.

## 1. Contrato em inglês (decidido pelo usuário na revisão; substitui T-F02-05e)
- **Valores de enum:** `TaskStatus` `A_FAZER/EM_ANDAMENTO/CONCLUIDA` → `TODO/IN_PROGRESS/DONE`; `TaskPriority` `BAIXA/MEDIA/ALTA` → `LOW/MEDIUM/HIGH` (iguala `TaskComplexity`, já em `LOW/MEDIUM/HIGH`). O valor é o mesmo em código, banco e JSON (sem camada de tradução).
- **Campos JSON públicos:**
  - tasks: `titulo/descricao/status/prioridade/prazo/idTarefaPai/criadoEm/atualizadoEm` → `title/description/status/priority/dueDate/parentId/createdAt/updatedAt`; `PageResponse` → `content/page/size/totalItems/totalPages/first/last`; `TaskSummary` indicadores para inglês; filtros `?status=`/`?priority=` mantêm o nome (já inglês) mas passam a aceitar os valores novos.
  - IA: `prioridade/complexidade/horasEstimadas/justificativa` → `priority/complexity/estimatedHours/reason`; `subtarefas` → `subtasks`; `titulo/descricao/horasEstimadas` dos rascunhos → `title/description/estimatedHours`.
  - assistente: `conversationId` (mantém), `mensagem` → `message`, `resposta` → `response`; `TaskToolResult` → `id/title/status/priority/dueDate`.
  - Problema da `errors` do `ProblemDetail` continua mapeando o **nome do campo JSON** (agora inglês); `type/status/title/detail/traceId/timestamp` (RFC 7807) inalterados.
- **Banco:** migration **nova `V4`** (refatora a `V2` NÃO é editada de novo): `DROP CONSTRAINT` dos CHECKs, `UPDATE` dos valores existentes (`A_FAZER→TODO`, etc.), ajuste de `DEFAULT` nas colunas `status`/`priority`, recriação dos CHECKs com valores inglês. Comentário da `V2` atualizado por consistência (não re-executa).
- **Frontend:** tipos/`api/` passam a usar os campos inglês; **rótulos da UI permanecem em português** (decisão do usuário: português só na UI).
- **Efeito colateral corrigido junto:** `AssistantToolCallbacks:57` descreve `"BAIXA, MEDIA, ALTA ou CRITICA"` — `CRITICA` não existe no enum; a descrição passa a listar `LOW, MEDIUM, HIGH`.

## 2. Assistente
- **Transação só nas gravações:** remover `@Transactional` de classe (`AssistantService.java:41`); a leitura da conversa/histórico e a chamada à porta ficam fora de transação; os dois `save` do turno vão para um método `@Transactional` próprio (gravação atômica, conexão liberada durante o LLM).
- **Janela sem carregar tudo:** `historicoNaJanela` (`AssistantService.java:79`) lê a conversa inteira e corta em memória → nova query devolve as últimas `JANELA_HISTORICO` mensagens (ordem desc + `Pageable`, invertida depois).
- **Ferramentas devolvem `{total, itens}`** e ordenam por urgência: `get_pending_tasks` e `get_tasks_by_priority` passam a ordenar por `dueDate ASC (nulls last)` + prioridade (`HIGH>MEDIUM>LOW`) + `createdAt DESC`, e o resultado vira envelope com `total` (remove o corte silencioso em `maxToolResults` sem informar o total). `TaskToolResult` intransitivo vira `ToolResultPage {total, itens}`.
- **GET mensagens:** novo endpoint `GET /assistant/conversations/{id}/messages` (404 se a conversa não existe; lista role/content/createdAt em ordem cronológica). Frontend: `conversationId` passa para `sessionStorage` e é restaurado no `mount` via esse GET.

## 3. Robusteza de IA
- **Prompt injection:** envolver `title`/`description`/`priority` (atual) em delimitadores `<tarefa>...</tarefa>` nos `.st` de tarefas; no fallback do assistente (`SpringAiAssistantAdapter.contextoOpcoes`) os dados injetados entram com delimitador e instrução de "dado não confiável". `PromptsTest` atualizado.
- **Timeout:** default `AI_TIMEOUT` 60s → **180s** em `application.yml:37`, `docker-compose.yml:65` e `.env.example:26`.
- **Fuso:** `LocalDate.now()` substituído por `LocalDate.now(clock)` com bean `Clock` (fuso de `app.timezone`, default `America/Sao_Paulo`) em `TaskQueryTools` e `SpringAiAssistantAdapter` (data atual do prompt).
- **Fonte única de limites:** `LlmResponseValidator` abandona os 5 `@Value` e recebe `AiProperties` (e `AssistantLimitsProperties` já é usado em F04).
- **Retry sem mascarar bug:** `SpringAiTaskAiAdapter.java:108` troca `catch (RuntimeException)` por captura das exceções esperadas de validação/mapeamento; exceção não mapeada se propaga (não vira retry).

## 4. Ambiente/documentação/processo
- **compose** repassa `ASSISTANT_TOOL_CALLING` e `ASSISTANT_MAX_TOOL_RESULTS` ao `backend`; imagem `ollama/ollama:latest` (duas ocorrências) fixada em tag estável verificada na implementação (não inventar número).
- **`.env.example`**: adiciona `ASSISTANT_TOOL_CALLING`/`ASSISTANT_MAX_TOOL_RESULTS`, corrige o comentário de `VITE_BACKEND_URL` (é usado como target do proxy em `frontend/vite.config.ts:10`) e documenta `VITE_API_BASE_URL` (usado em `frontend/src/api/client.ts:1`, ausente do exemplo).
- **README**: seção "Sem Docker" exige Docker e precisa de título/descrição corretos; "Prompts utilizados" ganha descrição do conteúdo de cada `.st`; corrigir typo "Ia valida sempre" → "A IA valida sempre"; comandos de teste documentam a estratégia Maven via env var (`MAVEN_HOME`/PATH) com descoberta automática do binário quando ausente — **nenhum caminho de máquina é documentado**.
- **Processo de commits** (decisão a registrar em `commit-convention.md` e STATE): reescrever histórico **só com pedido explícito do usuário**; verbos restritos à lista (`docs` passa a ser permitido por decisão do usuário; `feat` não pertence, usar `add`); reduzir commits dedicados a "registrar hash" — os hashes entram na matriz/STATE na verificação final, não em commit separado.

## Fora de escopo (não fazer nas tasks)
- Voltar a editar a `V2` (fonte de verdade imutável; mudanças sempre por migration nova).
- Traduzir o envelope RFC 7807 (`ProblemDetail`) para português.
- Redesenhar o tool calling (modo default continua toggle `app.assistant.tool-calling`).