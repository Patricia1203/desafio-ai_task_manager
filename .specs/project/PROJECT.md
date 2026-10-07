# PROJECT.md
Visão: implementar AI Task Manager (Spring Boot 4 + Spring AI + React).
Objetivo: atender todos os requisitos obrigatórios [OBR] do desafio, com desenvolvimento spec-driven, rastreabilidade completa e qualidade.
Público: avaliadores do desafio.
Restrições: seguir princípios (não inventar, uma fonte por assunto, escopo fechado), um commit por task, testes verdes antes de done, manter STATE.md e TRACEABILITY.md.
Fora de escopo: ideias extras vão para ROADMAP.md seção 'Futuro/Fora de escopo'.
Critérios de sucesso:
- Todos [OBR] com task, teste/gate, commit e status done
- docker compose up sobe backend/frontend/banco/LLM e fluxo funciona
- Nenhum stack trace em erros; ERR-01..06 tratados
- Trocar provedor de LLM só em ai.adapter + config
- README completo (7 seções)

## Requisitos

**Fonte única: `Desafio de Programação — AI Task Manager.pdf`, na raiz do projeto** (21 seções). O arquivo fica **fora do git por decisão do usuário** — versioná-lo não é necessário; esta tabela é o registro consultável.

**Os IDs são deste projeto, não do enunciado.** O PDF não usa `RF-`/`RNF-` (zero ocorrências): ele traz seções numeradas. Cada ID abaixo foi criado pelo projeto para rastrear cobertura e aponta a seção do PDF que o origina. `Obrigatório` = o PDF cobra; `Diferencial`/`Recomendado` = o PDF trata como opcional.

| ID | Origem no enunciado | Exigência | Status |
|---|---|---|---|
| RF-01 | §3.1 criar tarefas | Obrigatório | done |
| RF-02 | §3.1 listar e visualizar | Obrigatório | done |
| RF-03 | §3.1 editar | Obrigatório | done |
| RF-04 | §3.1 editar (PUT não mexe em status) | Obrigatório | done |
| RF-05 | §3.1 excluir | Obrigatório | done |
| RF-06 | §3.1 alterar status | Obrigatório | done |
| RF-07 | §3.1 campos mínimos da tarefa | Obrigatório | done |
| RF-08 | §3.1 status `TODO`/`IN_PROGRESS`/`DONE` | Obrigatório | done |
| RF-09 | §3.1 prioridade `LOW`/`MEDIUM`/`HIGH` | Obrigatório | done |
| RF-10 | §4 melhorar tarefa | Obrigatório | done |
| RF-11 | §5 análise (prioridade, complexidade, esforço, justificativa) | Obrigatório | done |
| RF-12 | §5 estrutura de dados adequada, não texto livre | Obrigatório | done |
| RF-13 | §6 decompor em subtarefas | Obrigatório | done |
| RF-14 | §6 subtarefas adicionadas como tarefas relacionadas | Obrigatório | done |
| RF-15 | §7 assistente responde sobre as tarefas | Obrigatório | done |
| RF-16 | §7 responder só com o contexto fornecido | Obrigatório | done |
| RF-17 | §8 histórico da conversa persistido | Diferencial | done |
| RF-18 | §8 histórico em ordem cronológica | Diferencial | done |
| RF-19 | §9 tool calling (pendentes, atrasadas, por id, por prioridade) | Diferencial (não obrigatório) | done |
| RF-20 | §11 Dashboard com os 5 indicadores | Obrigatório | done |
| RF-21 | §11 tela de tarefas com CRUD, status e IA | Obrigatório | done |
| RF-22 | §11 tela de conversa do assistente | Obrigatório | done |
| RF-23 | §12 API REST de tarefas | Obrigatório | done |
| RF-24 | §12 endpoints de IA também na API | Obrigatório | done |
| RNF-01 | §2 stack (Java 21+, Boot 4, Spring AI, Spring Web, JPA, PostgreSQL) | Obrigatório | done |
| RNF-02 | §10 respostas estruturadas (o sistema interpreta o retorno do LLM) | Obrigatório | done |
| RNF-03 | §11 rotas e navegação da interface | Obrigatório | done |
| RNF-04 | §17 execução com Docker | Recomendado | done |
| RNF-05 | §2 libs extras justificadas no README | Obrigatório | done |
| RNF-10 | §10 qualidade dos prompts e validação das respostas | Obrigatório | done |
| RNF-11 | §10 controle do que é enviado ao modelo | Obrigatório | done |
| RNF-12 | §10/§13 separação entre regra de negócio e integração com LLM | Obrigatório | done |
| RNF-13 | §10 tratamento de resposta inválida e de erro do LLM | Obrigatório | done |
| RNF-14 | §13 baixo acoplamento com o provedor | Obrigatório | done |
| RNF-15 | §10 não confiar cegamente no modelo | Obrigatório | done |
| RNF-20 | §13 separação de camadas | Obrigatório | done |
| RNF-21 | §14 PostgreSQL com modelagem e índices | Obrigatório | done |
| ERR-01 | §15 tarefa inexistente | Obrigatório | done |
| ERR-02 | §15 dados inválidos | Obrigatório | done |
| ERR-03 | §15 erro de comunicação com o LLM | Obrigatório | done |
| ERR-04 | §15 resposta inválida do LLM | Obrigatório | done |
| ERR-05 | §15 indisponibilidade do Ollama | Obrigatório | done |
| ERR-06 | §15 erro de persistência | Obrigatório | done |
| TST-01 | §16 testes de regras de negócio | Obrigatório | done |
| TST-02 | §16 testes dos endpoints | Obrigatório | done |
| TST-03 | §16 testes dos serviços de IA | Obrigatório | done |
| TST-04 | §16 testes de integração | Obrigatório | done |
| DOC-01 | §18 README com as 7 seções | Obrigatório | done |
| DEL-01 | §19 estrutura de entrega | Obrigatório | done |
| DEL-02 | §19 código necessário para executar e avaliar | Obrigatório | done |

Observações:
- **§3.1 dá liberdade de apresentação:** "a forma de apresentação dessas informações na interface fica a critério do candidato" — por isso os rótulos da UI são em português e os valores enviados ao backend em inglês.
- **Buraco de numeração:** `RNF-16`, `RNF-17`, `RNF-18` e `RNF-19` não existem — as specs citam "RNF-01..RNF-21", mas a série salta de RNF-15 para RNF-20. Nenhum requisito perdido; só IDs que nunca foram criados.
- **§20 (prazo) e §21 (apresentação)** não geraram ID: são condições de entrega, não itens de implementação.
- Cobertura por requisito, com código, testes e commit, está em `TRACEABILITY.md`.
