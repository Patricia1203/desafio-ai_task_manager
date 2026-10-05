# Testing — Estratégia e comandos

## Backend (Maven, módulo `backend/`)

Maven não está no PATH nesta máquina. Usar o binário direto:

```
MAVEN=$MAVEN_HOME/bin/mvn
```

| Nível | Ferramenta | Quando |
|---|---|---|
| Unitário | JUnit 5 + Mockito + AssertJ | Regras de negócio, validação de saída do LLM, ferramentas do assistente (a porta `TaskAiPort` / `ChatModel` é mockada) |
| Slice web | `@WebMvcTest` | Status codes, Bean Validation, formato `ProblemDetail` |
| Integração | `@SpringBootTest` + Testcontainers (`testcontainers-postgresql` 2.0.5) | Repositórios, migrations Flyway, fluxos ponta a ponta com porta de IA falsa |

Comandos:

```
& $MAVEN -f backend/pom.xml -q compile
& $MAVEN -f backend/pom.xml -q test
& $MAVEN -f backend/pom.xml -q test -Dtest=TaskServiceTest
& $MAVEN -f backend/pom.xml test -Dgroups=llm   # opcional: testes com LLM real (tag "llm")
```

Testes com LLM real são opcionais, marcados com `@Tag("llm")` e **excluidos do build padrão** (configurar exclusão no `pom.xml` em T-F03-02).

## Frontend (Vite + Vitest, módulo `frontend/`)

| Nível | Ferramenta | Quando |
|---|---|---|
| Componente/fluxo | Vitest 4 + Testing Library + jsdom | Formulário de tarefa, painel de IA com API mockada, chat do assistente |

Comandos (npm precisa ser chamado como `npm.cmd` no PowerShell deste ambiente):

```
& "C:\Program Files\nodejs\npm.cmd" run lint
& "C:\Program Files\nodejs\npm.cmd" run test
& "C:\Program Files\nodejs\npm.cmd" run build
```

## Regras

- Todo teste deriva dos critérios de aceite do `spec.md` da feature, não da implementação.
- Proibido enfraquecer asserção, remover caso de teste ou usar `skip`/`only` para passar.
- Nenhum teste depende de LLM real, exceto os marcados com `@Tag("llm")`.
- Testes de integração exigem o Docker daemon em execução (Testcontainers).