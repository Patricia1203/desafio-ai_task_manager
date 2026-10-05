# STATE.md

## Task atual
T-F02-04 — Controller REST de tarefas — **done** (Gate: `mvn -q test -Dtest=TaskControllerTest` verde, 27 testes; suíte completa com 101 testes verdes).
Próxima: T-F02-05 — Frontend Dashboard e Tarefas (Gate `npm run lint && npm run test && npm run build` no frontend).

## Decisões
- 2026-10-05: Repositório já tinha commit inicial e branch `main` com remote. Não foi necessário `git init`.
- 2026-10-05: **Maven não está no PATH** desta máquina; o binário usado nos Gates está em
  `$MAVEN_HOME/bin/mvn` (Maven 3.9.15).
  Todos os Gates de backend devem invocar esse caminho até o Maven ser adicionado ao PATH.
- 2026-10-05: **Versões verificadas no Maven Central** (não estimadas): Spring Boot **4.1.1** (GA), Spring AI **2.0.1**, Testcontainers **2.0.5**.
  As versões de milestone usadas no primeiro commit (Boot 4.0.0-M3 / Spring AI 1.0.0-M4) estavam erradas e foram substituídas.
- 2026-10-05: `spring-boot-starter-web` foi substituído por `spring-boot-starter-webmvc` (starter renomeado/deprecado no Boot 4).
- 2026-10-05: Testcontainers 2.x renomeou os artifacts: `junit-jupiter` → `testcontainers-junit-jupiter`, `postgresql` → `testcontainers-postgresql`.
- 2026-10-05: `context-path: /api` definido no servidor, então os controllers ficam em `/tasks`, `/ai/tasks/...`, `/assistant/...` (URL pública `/api/tasks`).
- 2026-10-05: Docker Desktop instalado (29.6.2). O daemon estava parado durante a verificação de T-F01-01 e foi **iniciado pelo usuário** antes de T-F01-02 (Docker Desktop 4.84.0, Engine 29.6.2, containerd 2.2.5).

## Premissas [ASSUMPTION]
- 2026-10-05 (T-F01-02): Frontend scaffoldado com `npm create vite@latest -- --template react-ts` (create-vite 9.2.1).
  Versões resultantes: **React 19.2.8**, **Vite 8.3.x** (rolldown), **TypeScript 6.0.x**, **Vitest 4.1.x**, React Router 7.9.x, jsdom 27.
- 2026-10-05 (T-F01-02): O `vite.config.ts` importa `defineConfig` de `vitest/config` (e não de `vite`) para que o bloco `test` seja tipado. Importar de `vite` dá erro de tipo TS2769.
- 2026-10-05 (T-F01-02): Vitest teve de ser **4.x**, não 3.x — o Vitest 3 embute um Vite diferente e o conflito de tipos de `Plugin` quebrava o `tsc -b`.
- 2026-10-05 (T-F01-02): Bibliotecas extras no frontend, com justificativa (RNF-05):
  - `react-router-dom` — roteamento das telas Dashboard/Tarefas/Assistente (exigido por RF-20/RF-21/RF-22).
  - `vitest` + `@testing-library/react` + `@testing-library/jest-dom` + `@testing-library/user-event` + `jsdom` — base de testes exigida por TST-02 na parte de frontend.
  - `oxlint` (já vindo do template) — lint rápido sem configuração pesada.
  Nenhum UI kit, nenhuma lib de estado global, nenhuma lib de forms.
- [ASSUMPTION] O modelo padrão `qwen2.5:7b` suporta tool calling no Ollama. Não verificado (Ollama indisponível). Confirmar em T-F03-02/T-F04-02; se não suportar, trocar para `llama3.1:8b` ou implementar fallback de contexto injetado (registrar decisão).
- [ASSUMPTION] `prazo` (coluna `due_date`, atributo `dueDate`) será `LocalDate` (data sem hora) com formato ISO-8601 `yyyy-MM-dd`. Se o desafio exigir data-hora, ajustar em F02.
- [ASSUMPTION] `BusinessRuleException` responde **422** e não 400. O `design.md` da F02 lista "ERR-02 400" sem distinguir; se o desafio exigir 400 para regra de negócio, o handler muda em um único ponto.
- [ASSUMPTION] **CONCLUIDA é terminal** — reabrir uma tarefa concluída exige passar por A_FAZER, e ir direto para EM_ANDAMENTO é recusado com 422. O `spec.md` da F02 só diz "altera entre A_FAZER, EM_ANDAMENTO, CONCLUIDA", sem definir transições inválidas. Se o desafio exigir transição livre, remover o bloco em `Task#changeStatus`.
- ~~[NEEDS CLARIFICATION] O que fazer ao excluir uma tarefa que tem subtarefas.~~ **Resolvido em T-F02-03: cascata.** Ver "Decisões — T-F02-03".

- 2026-10-05 (T-F01-03): **A imagem `eclipse-temurin:21-jdk-alpine` não contém o Maven.** O build do backend falhou com `mvn: not found`. Corrigido usando `maven:3.9-eclipse-temurin-21-alpine` no estágio de build e `eclipse-temurin:21-jre-alpine` no runtime.
- 2026-10-05 (T-F01-03): Tags de imagem **verificadas** com `docker manifest inspect` antes de fixar: maven 3.9-eclipse-temurin-21-alpine, eclipse-temurin 21-jre-alpine, node 24-alpine, nginx 1.29-alpine, postgres 17-alpine, ollama/ollama latest.
- 2026-10-05 (T-F01-03): O healthcheck do backend usa `wget`, que existe na imagem JRE alpine (busybox) — verificado executando `command -v wget` no container.
- 2026-10-05 (T-F01-03): O healthcheck do backend aponta para `/api/health`, que **ainda não existe** (entra em T-F01-04). O compose só sobe o frontend depois do backend ficar healthy, então essa task precisa criar o endpoint. Registrado como dependência.
- 2026-10-05 (T-F01-03): Dockerfiles multi-stage com usuário não-root no backend; cache de Maven e de npm em camadas separadas; nginx fazendo proxy de `/api` com timeout de 300s para as chamadas de IA.

## Decisões — T-F01-04
- 2026-10-05: **`V1__schema_base.sql` não cria tabela de negócio.** Cada feature é dona do seu DDL: `V2__create_tasks.sql` em F02, `V3` para o chat em F04. A V1 fixa as convenções (BIGSERIAL, `timestamptz`, enums como VARCHAR + CHECK, índices explícitos) para que `ddl-auto=validate` e as entidades JPA não entrem em conflito com o Flyway. Alternativa descartada: criar `tasks` já na V1 e prender a F02 a ALTER TABLE.
- 2026-10-05: **O Boot 4 exige `org.springframework.boot:spring-boot-flyway`.** Com apenas `flyway-core` o `FlywayAutoConfiguration` não existe, nenhuma migration roda e a tabela `flyway_schema_history` nem é criada — o app subia "verde" com o banco vazio. Detectado pelo smoke test.
- 2026-10-05: **`@WebMvcTest` passou a `org.springframework.boot.webmvc.test.autoconfigure`** e vem do módulo `spring-boot-webmvc-test`, que o `spring-boot-starter-test` não traz. Adicionado ao POM.
- 2026-10-05: **`PostgreSQLContainer` da Testcontainers 2.0.5 não é genérico.** O diamond operator não compila; o tipo usable é `org.testcontainers.postgresql.PostgreSQLContainer`.
- 2026-10-05: O handler global mapeia **422** (não 400) para `BusinessRuleException`, separando "entrada inválida" (400) de "regra de negócio violada" (422). 404 fica para `ResourceNotFoundException`.
- 2026-10-05: `HealthController` devolve `/api/health` com `status`, `timestamp` e o resultado de um `SELECT 1`. O compose depende dele para subir o frontend, então ele responde `DOWN` em vez de mentir `UP` quando o banco cai.
- 2026-10-05: CORS **falha fechado**: `app.cors.allowed-origins` vazio não registra mapeamento nenhum, em vez de liberalizar com `*`. `allowedOrigins` explícito (nunca `allowedOriginPatterns`) impede o eco de origens tipo `http://localhost:5173.evil.example.com`.
- 2026-10-05: O `pom.xml` agora exclui a tag `llm` do build padrão (`excludedGroups=llm`), como previsto no TESTING.md.

## Decisões — T-F02-01
- 2026-10-05: **`V2__create_tasks.sql` cria a tabela `tasks`.** Colunas: `id UUID`, `title`, `description`, `status`, `priority`, `due_date DATE`, `parent_id UUID` (auto-relacionamento, `ON DELETE CASCADE`), `created_at`/`updated_at` em `timestamptz`. Índices em `status`, `priority`, `due_date` e `parent_id`, como pede o design da F02.
- 2026-10-05: **O `id` é `UUID` gerado em Java**, não no banco. Assim a entidade já tem identidade antes do INSERT e a F03 pode criar a tarefa pai e as subtarefas na mesma transação referenciando o pai.
- 2026-10-05: **CONCLUIDA é estado terminal.** `Task#changeStatus` recusa CONCLUIDA → EM_ANDAMENTO e lança `BusinessRuleException` (→ 422); reabrir exige ir a A_FAZER. Ver [ASSUMPTION] abaixo.
- 2026-10-05: **O `design.md` fala em `parentTaskId UUID`; implementado como `Task parent` com `@ManyToOne(fetch = LAZY)` e coluna `parent_id`.** Motivo: o design pede subtarefas por `parentTaskId`, e uma referência fraca basta para a listagem; o `fetch = LAZY` evita carregar a árvore inteira em toda listagem. O `TaskResponse` vai expor `idTarefaPai` (ver T-F02-05e), então o contrato HTTP não muda.
- 2026-10-05: **`ON DELETE CASCADE` em `parent_id`.** Excluir a tarefa pai remove as subtarefas; não deixar órfãos pendurados. A alternativa (bloquear a exclusão) fica como pergunta em aberto para F02-03, que decide a regra de negócio do `DELETE`.
- 2026-10-05: **Valores de `status` gravados como `VARCHAR` com `CHECK`, não como tipo ENUM do Postgres.** Adicionar um valor depois seria `ALTER TYPE`; com `VARCHAR` + `CHECK` é um `ALTER TABLE` e o mapeamento `EnumType.STRING` continua igual dos dois lados.
- 2026-10-05: **`TaskRepository` estende `JpaSpecificationExecutor`** para os filtros compostos de RF-02 (status + prioridade + paginação); as consultas de summary e as ferramentas somente-leitura do assistente (F04) são `@Query` nomeadas.
- 2026-10-05: **Timestamp de criação/leitura na entidade, não em trigger do banco.** Uma fonte só de tempo evita divergência entre o que o teste compara e o que o banco devolve.

## Decisões — T-F02-02
- 2026-10-05: **Mapper escrito à mão, sem MapStruct.** Um record com nove campos tem mapeamento trivial e um `@Mapper` gerado custaria um plugin de build e esconderia o contrato. Todos os campos de `TaskResponse` aparecem explicitamente no `TaskMapper#toResponse`, e o teste cobre o round-trip.
- 2026-10-05: **`TaskResponse` expõe o id do pai (UUID, campo JSON `idTarefaPai` desde T-F02-05e), nunca a referência `Task parent`.** Como o pai é `LAZY`, serializar a entidade arrastaria proxy ou exigiria sessão aberta; o contrato HTTP fica com um UUID.
- 2026-10-05: **`UpdateTaskRequest` não tem campo `status`.** PUT substitui conteúdo; status tem endpoint próprio (`PATCH /tasks/{id}/status`). Aceitar status no PUT daria ao mapper um caminho que burla `Task#changeStatus` e deixaria a invariante CONCLUIDA-terminal sem dono. Se o desafio exigir PUT completo, o mapper passa a chamar `changeStatus` em vez de setar o campo.
- 2026-10-05: **Limites de validação: título 200 e descrição 5000.** O 200 é o mesmo do `VARCHAR(200)` de `V2__create_tasks.sql`, para a constraint do banco e a do Bean Validation dizerem a mesma coisa; 5000 é a capacidade do `TEXT` e evita payload ilimitado.
- 2026-10-05: **Mensagens de constraint em português, sem acento.** O `GlobalExceptionHandler` de T-F01-04 joga `getMessage()` direto na propriedade `errors` do ProblemDetail, então a string é contrato visível ao usuário e fica escrita no DTO.
- 2026-10-05: **`PageResponse<T>` é genérico e é o único ponto que conhece `Page<T>` do Spring Data.** Serve para a lista de tarefas e para as mensagens do assistente em F04 sem formato de paginação duplicado.
- 2026-10-05: **`toSubtask` é separado de `toDomain`.** A decomposição por IA (RF-14, F03) exige pai; deixar o pai explícito na assinatura impede um caminho da IA criando tarefa de topo por engano.
- 2026-10-05: **O `trim()` do título fica no DTO (`normalizedTitle()`), não no mapper.** O DTO é a borda: normalizar na entrada evita repetir isso no service e na IA, que vai montar DTOs também.

## Decisões — T-F02-03
- 2026-10-05: **Excluir uma tarefa remove as subtarefas em cascata. Decisão fechada nesta task.** O `spec.md` empurrava isso ("definir comportamento depois se necessário") e o `ON DELETE CASCADE` já estava no schema; o service apenas não bloqueia. A alternativa (recusar com 422 e obrigar a tratar as filhas antes) exigiria contar as filhas dentro da transação do delete. Se o desafio preferir bloquear, o caminho é remover o `ON DELETE CASCADE` na próxima migration e trocar o `delete`.
- 2026-10-05: **`TaskFilter` não carrega `Pageable`.** `TaskService#list(filter, pageable)` e `#findAll(filter)` são métodos separados: a tela usa o primeiro, as ferramentas somente-leitura do assistente (F04) usam o segundo. Um filtro com paginação embutida obrigaria a F04 a fabricar um pageable falso.
- 2026-10-05: **`TaskSummary` é record nomeado, não `Map<String, Long>`.** `summary.pendentes()` é verificável em tempo de compilação e o JSON sai com os campos nomeados que a tela consome.
- 2026-10-05: **`@Transactional(readOnly = true)` na classe, `@Transactional` nos métodos de escrita.** `findById` seguido de alteração e `save` no mesmo método de leitura seria transação implícita por chamada.
- 2026-10-05: **O `delete` verifica existência antes de apagar.** `repository.delete(id)` aceitaria id inexistente e responderia 204, furando ERR-01.

## Decisões — T-F02-04
- 2026-10-05: **`TaskController` sem `@Validated`.** A anotação troca a validação nativa do Spring 7 nos parâmetros do controller (`HandlerMethodValidationException`, já mapeada em 400) pelo proxy AOP (`ConstraintViolationException`, sem mapeamento). O sintoma era `GET /tasks?page=-1` respondendo **500** em vez de 400, com o log mostrando a exceção Original. Se outro controller precisar de validação de parâmetro, usar a nativa e nunca reintroduzir `@Validated` sem antes mapear `ConstraintViolationException`.
- 2026-10-05: **`/tasks/summary` antes de `/tasks/{id}`.** `/{id}` com `UUID` casa qualquer segmento, então a ordem de declaração decide se o summary existe. Não é estilo, é correção.
- 2026-10-05: **`size` limitado a 100 e `page` a 0 ou mais.** Parâmetro de query sem teto vira consultas que derrubam o Postgres; o limite entra como `400` com campo, não como 500.
- 2026-10-05: **Ordenação fixa em `createdAt desc`, sem parâmetro de sort no filtro.** Mais recentes primeiro é decisão de produto; aceitar sort do cliente abre espaço para ordenação sobre coluna não indexada.
- 2026-10-05: **A confirmação da cascata é responsabilidade da tela, não do backend.** Decisão do usuário: a interface lista as subtarefas antes de excluir. O backend não ganha `?confirm=true` — o `design.md` define "remove ou 404", e `GET /tasks/{id}/subtasks` já entrega a lista. Implementado em T-F02-05.
- 2026-10-05: **`CorsConfigTest` fixado em `@WebMvcTest(HealthController.class)`.** Um `@WebMvcTest` sem atributo carrega todos os controllers; ao existir `TaskController` (que depende de `TaskService`), o contexto do teste de CORS passou a falhar por causa de um controller que ele não exercita. A correção é fixar o slice, não injetar beans falsos.

## Decisões — T-F02-05a..T-F02-05i (revisão do backend)
- 2026-10-05 (T-F02-05a): **O `GlobalExceptionHandler` trata 404, 405 e 415 explicitamente**, em vez de deixar o `catch (Exception)` genérico inventar `internal-server-error`. Sem isso, `NoResourceFoundException` (URL errada) e `HttpRequestMethodNotSupportedException` (verbo errado) respondiam 500 — erro do cliente reportado como falha do servidor.
- 2026-10-05 (T-F02-05a): **`DataAccessException` tem tipo próprio `banco-indisponivel` e mensagem fixa.** A mensagem original do driver (`password authentication failed`, nome de usuário, SQL) ia para o cliente; é informação interna.
- 2026-10-05 (T-F02-05b): **`/health` responde 503 quando o banco está fora.** Um health check que responde 200 com o banco caído deixa o compose subir o frontend contra uma API que não serve nada.
- 2026-10-05 (T-F02-05c): **O `Location` do POST é montado com `ServletUriComponentsBuilder`.** Com `context-path: /api`, um Location sem o prefixo aponta para um caminho inexistente e o cliente toma 404 ao seguir o próprio header do 201. O MockMvc não aplica `context-path` sozinho, então o teste declara `/api` explicitamente na requisição.
- 2026-10-05 (T-F02-05f): **Ordenação estável `createdAt desc, id asc`.** Só `createdAt` deixa empates a cargo do banco e a página pode repetir ou omitir item entre requisições; o desempate por id torna o resultado determinístico.
- 2026-10-05 (T-F02-05g): **Agrupamento de `spring-boot-dependencies` removido do `pom.xml`.** O parent já é `spring-boot-starter-parent`, que importa o BOM; declarar de novo gera aviso de modelo duplicado.
- 2026-10-05 (T-F02-05h): **O teste de schema usa `contains`, não `containsExactly`.** O teste afirma que o Flyway criou o que ele devia criar; a lista de tabelas do projeto cresce a cada feature, e uma asserção exata faz a F04 quebrar por tabelas novas — não por defeito.
- 2026-10-05 (T-F02-05i): **Um container PostgreSQL compartilhado por todos os testes de integração.** Um container por classe custava ~12 s de boot cada; o total da suíte caiu para menos de 60 s.

## Decisões — T-F02-05e (contrato em português)
- 2026-10-05: **O contrato JSON das tarefas está em português, com values em português.** `TaskResponse` usa `id, titulo, descricao, status, prioridade, prazo, idTarefaPai, criadoEm, atualizadoEm`; `CreateTaskRequest`/`UpdateTaskRequest` usam `titulo, descricao, prioridade, prazo`; `PageResponse` usa `conteudo, pagina, tamanho, totalItens, totalPaginas, primeira, ultima`. Os enums gravados passam a `A_FAZER/EM_ANDAMENTO/CONCLUIDA` e `BAIXA/MEDIA/ALTA`.
- 2026-10-05: **A tradução vale para o corpo e a resposta JSON, não para a URL.** Os query params seguem em inglês (`status`, `priority`, `page`, `size`): renomeá-los junto mudaria a URL da API sem pedido do usuário, e `?status=A_FAZER` já é legível.
- 2026-10-05: **`ProblemDetail` não foi traduzido.** `type`, `title`, `status` e `detail` são o padrão da RFC 7807;quem consome o corpo depende desses nomes.
- 2026-10-05: **O modelo JPA e as colunas continuam em inglês** (`title`, `priority`, `due_date`). Traduzir a coluna exigiria uma migration nova e reescrever `@Entity` sem mudar o contrato HTTP, que é o ponto do desafio.
- 2026-10-05: **A `V2__create_tasks.sql` foi editada, não criada uma `V3`.** Os valores default e os `CHECK` do banco precisam casar com o enum novo; `CREATE TABLE IF NOT EXISTS` esconde o banco velho, então quem já aplicou a V2 anterior precisa limpar/recriar o schema (checksum divergente quebra o boot do Flyway).

## Dúvidas [NEEDS CLARIFICATION]
- [NEEDS CLARIFICATION] `docs/desafio.pdf` não existe no repositório. Se o usuário fornecê-lo, comparar com a tabela de requisitos do prompt e registrar divergências aqui.

## Bloqueios
- Nenhum. Docker daemon em execução (verificado em T-F01-02).

## Melhorias aplicadas automaticamente
- O `TaskSummary`/endpoint de health não dependem de actuator, evitando mais uma dependência de runtime só para o healthcheck do compose.
- Removido do Dockerfile do backend o `COPY` da pasta `prompts` no estágio de runtime: o `.jar` já embute `src/main/resources`, então a cópia era redundante e criava uma segunda fonte de prompts em disco.
- `.dockerignore` em `backend/` e `frontend/` para não enviar `target/`, `node_modules/` e `dist/` no contexto de build.
- Healthcheck do compose com `start_period` para dar tempo do backend subir (Flyway + JPA + Ollama na inicialização).
- Corrigido BOM UTF-8 (`\ufeff`) gerado por `Set-Content -Encoding UTF8` do PowerShell, que quebrava a compilação Java. Regra: usar a ferramenta de escrita de arquivo (sem BOM) para código Java/XML.
- `application.yml` reescrito com as propriedades corretas do Spring AI 2.x (`spring.ai.model.chat=ollama`, `spring.ai.ollama.chat.model`) e com `server.error.include-*` desabilitados para garantir que nenhuma mensagem interna vaze nas respostas.
- Adicionado `flyway-database-postgresql` (exigido pelo Flyway moderno para dialeto Postgres).
- Adicionado bloco `app.*` com CORS, timeout/retry de IA e limites de subtarefas, para que a F03 não precise mexer em contrato de config depois.

## Melhorias propostas (aguardam o usuário)
- [Proposta] Adicionar Maven Wrapper (`mvnw`) ao repositório para que os Gates funcionem em qualquer máquina sem depender do caminho local do Maven. Impacto: adicionar arquivos ao repo (baixo risco), mas muda o comando de todos os Gates de backend. Aguardando aprovação.
- [Proposta] `context-path: /api` vs. prefixo `/api` em cada controller. Mantido `context-path` (menos repetição), mas isso torna os testes `@WebMvcTest` ligeiramente diferentes. Se preferir o padrão mais explícito, é uma mudança de contrato público — requer sua decisão.

## Lições
- No PowerShell 5.1 deste ambiente, `&&` não é válido, `cat`/`head`/`tail` não existem, `npm` precisa ser chamado via `npm.cmd`, e `Set-Content -Encoding UTF8` grava BOM. Usar `;`, `Select-Object -First/-Last`, `& "C:\...\npm.cmd"` e a ferramenta de escrita de arquivos.
- **Boot 4 fragmentou os starters em módulos por tecnologia.** `spring-boot-starter-test` não traz `@WebMvcTest`, `flyway-core` não traz o autoconfiguration do Flyway. Antes de confiar numa configuração, confirmar que o módulo está no POM — o sintoma é "subiu sem erro mas nada aconteceu" (`spring.ai.ollama.chat.enabled` no Boot 2, Flyway silencioso no Boot 4).
- **Teste que passa com banco vazio é teste que não testa nada.** O smoke test consultar `flyway_schema_history` foi o que revelou o Flyway desligado; um simples "contexto carregou" teria passado.
- Spring 7 removeu `HandlerMethodValidationException#getAllValidationResults()`: usar `getParameterValidationResults()`.
- Um método de teste com espaço no nome (`void segundaOrigemConfigurada TambemEhLiberada()`) não compila; o erro do compilador aparece como `'(' expected` na linha seguinte, o que confunde a leitura.
- **`git commit -m` no PowerShell 5.1 achata o corpo da mensagem numa linha só.** Escrever a mensagem num arquivo e usar `git commit -F <arquivo>`. Ocorre em T-F01-03, T-F01-04 e T-F02-01.
- **Teste de integração sem `@Transactional` deixa a entidade detached:** depois de `saveAndFlush`, chamar `flush()` não gera UPDATE. Use `saveAndFlush` a cada mudança, ou coloque `@Transactional` no teste.
- **`@Validated` no controller troca a exceção de validação e quebra o 400.** Com a anotação, `ConstraintViolationException` (não mapeada) cai no 500 genérico; sem ela, `HandlerMethodValidationException` (mapeada) vira 400 com a lista de campos. Teste que só olha `isOk()` não pega isso; precisa checar o status e o corpo do erro.
- **`@WebMvcTest` sem atributo carrega todos os controllers e quebra testes que não são dele.** Passou a falhar com `NoSuchBeanDefinitionException` ao entrar o primeiro controller com dependência própria. Sempre fixar `controllers = ...` ou `controllers = Classe.class`.
- **`timestamptz` guarda microssegundos, `Instant` guarda nanossegundos.** Comparar o valor lido em memória com o relido do banco falha por precisão (`...Z` com 9 dígitos contra 6). Relê do banco antes de usar como referência, ou compare truncado.
- **Sem `@Container` + `@ServiceConnection`, o `@SpringBootTest` cai no `application-test.yml`** e tenta o Postgres da máquina: a falha aparece como `BeanCreationException` no `entityManagerFactory` por "autenticação do tipo senha falhou", sem nenhuma pista de que faltou o container. Copiar o bloco de container de outro teste de integração pronto.
- Nomes de variável que colidem com classe do mesmo pacote (`Task hoje`) causam erro de compilação confuso tipo `cannot be converted to Task`; usar o nome do tipo (`LocalDate hoje`).
- **`Validator.validate` devolve `Set`, e `Set` não tem índice.** Guardar o resultado em `List` e chamar `.get(0)` não compila, com o erro "no instance(s) of type variable(s) T exist so that Set<...> conforms to List<...>", que não aponta para a causa. Usar um helper que valida o tamanho e devolve `iterator().next()`.
- **Asserção tautológica passa no teste e não testa nada.** Escrever `assertThat(x).isEqualTo(condicao ? x : x)` nunca falha. Quando a intenção era comparar com o valor de antes da operação, capturar o valor em variável local antes de chamá-la.
- A ferramenta de escrita/edição introduziu um caractere CJK (`缚`) em texto português já escrito. Vale varrer os arquivos de spec com uma regex de `\u3000-\u9FFF` depois de edições longas antes de commitar.