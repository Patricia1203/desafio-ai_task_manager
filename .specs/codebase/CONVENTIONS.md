# CONVENTIONS.md

## Camadas e empacotamento
Pacotes por feature, camadas dentro do pacote:

```
com.desafio.taskmanager
├── task/            (feature)
│   ├── api/         controllers e DTOs records
│   ├── application/ services e use cases
│   ├── domain/      Task, TaskStatus, TaskPriority — sem Spring Data
│   └── infra/       TaskRepository
├── ai/              porta, adaptador Spring AI, prompts, DTOs
├── assistant/       porta, ferramentas somente-leitura, servico de chat
└── common/
    ├── config/      WebConfig, CorsProperties
    ├── error/       excecoes de dominio + GlobalExceptionHandler
    └── web/         HealthController
```

- **A entidade JPA nunca sai do backend.** A API responde com records de
  `task.api.dto`, convertidos por `TaskMapper`, que mora em `task.api` junto
  do controller (RNF-20, T-F02-05d).
- **`task.application` não importa nada de `task.api`.** A entrada da camada de
  aplicação é o neutro `TaskCommand` e a saída é a própria entidade; o controller
  mapeia nas duas pontas.
- **O domínio não importa Spring Data nem Spring AI.** Invariantes (status
  inicial, transições, título obrigatório) vivem nos métodos da entidade; o
  `TaskMapper` e o `TaskService` só orquestram.
- `task.domain` não conhece `task.infra`: a dependência aponta para o mesmo
  sentido de toda requisição (api → application → domain), e infra implementa.

## DTOs
- `record`, imutáveis, nunca reutilizados como entidade.
- `PageResponse<T>` é o envelope de lista paginada; `TaskResponse` não vaza
  `Task` nem `parent` (só `parentId`).

## Validação
- Bean Validation nas bordas (DTOs de entrada). Constraints com `message` em
  português, porque a mensagem vai para o usuário no ProblemDetail.
- Regras que dependem de estado (ex.: transição de status) são do domínio e
  viram `BusinessRuleException` → 422, não 400.

## Erros
- `ProblemDetail` RFC 7807 em toda resposta de erro, via
  `common.error.GlobalExceptionHandler`.
- `type` sob `https://desafio.ai-task-manager/errors/{slug}`.
- **Nada de stack trace, nome de exceção, SQL ou caminho de arquivo na
  resposta.** A causa original vai só para o log.
- 400 = entrada inválida (com a lista `errors` de campo/motivo), 404 =
  `ResourceNotFoundException`, 422 = `BusinessRuleException`, 500 = genérico
  com mensagem fixa.

## Datas
- `dueDate` é `LocalDate` (data, sem hora) serializado em ISO-8601 `yyyy-MM-dd`.
  [NEEDS CLARIFICATION] resolvido como premissa em T-F01-01; se o desafio exigir
  data-hora, o tipo e a coluna `due_date` mudam juntos na próxima migration.
- `createdAt` e `updatedAt` são `Instant` (UTC), gravados em `timestamptz`.
- **O tempo é gerado pela entidade, não por trigger do banco nem por
  `@CreationTimestamp`:** uma fonte só evita divergência entre o que o teste
  compara e o que o banco devolve.

## Banco
- Chave primária `UUID` gerada em Java (`UUID.randomUUID()` na factory da
  entidade), para o id existir antes do INSERT.
- Enums persistidos como `VARCHAR` com `CHECK`, nunca tipo `ENUM` do Postgres:
  adicionar um valor depois vira `ALTER TABLE` em vez de `ALTER TYPE`, e o
  mapeamento `EnumType.STRING` continua igual dos dois lados.
- Índices sempre explícitos na migration. O Hibernate roda com
  `ddl-auto=validate`: ele valida, nunca cria schema. A única fonte do schema
  é o Flyway.
- Mapeamento JPA sempre explícito (`@Column`, `@Enumerated`, `@JoinColumn`),
  sem depender do padrão de nome do provider.

## Nomenclatura
- Entidades e enums de domínio no singular (`Task`, `TaskStatus`).
- DTOs de entrada com o verbo (`CreateTaskRequest`, `UpdateTaskRequest`,
  `UpdateStatusRequest`); de saída com o papel (`TaskResponse`).
- Métodos de repositório por **intenção**, não por implementação: `countByStatusValue`,
  `findByDueDateLessThanAndStatusNotOrderByDueDateAsc`.

## Commits
- Um commit por task, no formato `<verbo>: <Título>` com itens ` - ` no corpo.
- **No PowerShell, escrever a mensagem em arquivo e usar `git commit -F`.**
  Com `-m` e aspas duplas as quebras de linha do corpo se perdem e tudo vira
  uma linha só (ocorreu em T-F01-03, T-F01-04 e T-F02-01).