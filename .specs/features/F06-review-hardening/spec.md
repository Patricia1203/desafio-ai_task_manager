# spec.md — F06-review-hardening

## Objetivo
Corrigir as pendências apontadas na revisão pós-entrega do desafio (revisão do usuário em 2026-10-07), a parte delas que se confirmou verdadeira contra o código e as specs. Nada é implementado antes da aprovação das tasks pelo usuário.

## Escopo
Pendências de quatro frentes, na prioridade reportada:

1. **Alta** — reversão do contrato para inglês (enums/campos/JSON e migration V4 no lugar de editar a V2), ferramenta `get_tasks_by_priority` citando prioridade inexistente (`CRITICA`), transação segurando conexão durante a chamada ao LLM, e ferramentas de consulta cortadas sem `total` e fora de ordem de urgência.
2. **Média** — timeout padrão curto para CPU, fuso horário fixo em UTC (`LocalDate.now()`), histórico não consultável (sem GET de mensagens e `conversationId` não persistido na UI), prompt injection só por instrução (sem delimitadores), configuração solta (compose não repassa variáveis, `.env.example` desatualizado, imagem `ollama:latest` sem tag).
3. **Baixa** — limites lidos por duas fontes (`@Value` + `AiProperties`), `catch (RuntimeException)` no retry mascarando bug, janela de 20 mensagens carregando a conversa inteira, linhas `done (parcial)` na matriz, 2 warnings `set-state-in-effect` do oxlint.
4. **Docs/processo** — README (`Sem Docker` exige Docker, falta descrever conteúdo dos prompts, typo), estratégia Maven (env var + auto-descoberta, sem caminho de máquina na doc), `docs/desafio.pdf` fora do repositório, verbos de commit fora do padrão (10 `docs:` + 1 `feat:`; decisão do usuário: `docs` passa a ser permitido, `feat` não), regra de reescrita de histórico (só com pedido explícito do usuário), requisitos do prompt ausentes de `.specs` (RNF-02/12/13/15 e TST-03 preenchidos sem o texto).

## Requisitos cobertos
Reused de features existentes (sem texto individual disponível no repo; ver [NEEDS CLARIFICATION] no STATE.md):
RF-01, RF-02, RF-04, RF-05, RF-06, RF-07, RF-08, RF-09, RF-10, RF-11, RF-12, RF-15..RF-19, RF-20, RF-22, RF-23, RF-24, RNF-02, RNF-03, RNF-10, RNF-12, RNF-13, RNF-14, RNF-15, RNF-21, ERR-01..ERR-06, TST-01..TST-04, DOC-01.

Itens de manutenção sem ID de requisito próprio (decisões do usuário na revisão) são rastreados como "review" nas tasks.

## Perguntas em aberto
- [NEEDS CLARIFICATION] **Texto do RF-24 e demais requisitos.** Nenhum arquivo de `.specs` contém o texto dos requisitos; os IDs só aparecem atribuídos em `spec.md`/`tasks.md`/matriz. O usuário deve colar a tabela de requisitos do prompt (T-F06-01) para reavaliar RNF-02/12/13/15 e TST-03.
- [CLARIFICADO] **Contrato em português foi aprovado pelo usuário na 1ª vez** (T-F02-05e, tasks.md:133); a **documentação do desafio exige inglês**, então foi decidido reverter para inglês (campos e valores), mantendo português apenas nos rótulos da UI (T-F06-02..05).
- [DECIDED by user] Timeout padrão: **180s** (aprovado), configurável via `AI_TIMEOUT`.
- [DECIDED by user] Fuso padrão: **America/Sao_Paulo** (aprovado), configurável via nova propriedade `app.timezone`.