# design.md — F01-foundation

## Visão geral
Skeleton backend Spring Boot 4 + Java 21, skeleton frontend React (Vite), Docker compose, Flyway, ProblemDetail global, CORS, config por env.

## Componentes e responsabilidades
- Backend: config, common.error, infra.flyway, web.advice, health? (mínimo). Pacotes por feature (task/ai/assistant/common).
- Frontend: api/client, components/layout, pages/dashboard? inicial vazio funcional.
- Infra: docker-compose, .env.example.

## Contratos
Endpoints mínimos: GET /actuator/health (se usar actuator) ou GET /api/health. Erros: ProblemDetail RFC7807.

## Fluxos
Sem fluxos complexos.

## Decisões e alternativas
[ASSUMPTION] Usar Maven (mais comum). [NEEDS CLARIFICATION] Spring Boot 4.x exato? Spring AI compatível?

## Riscos e mitigação
Versões novas → verificar antes de fixar.

## Estratégia de teste
TST-04: @SpringBootTest + Testcontainers (Postgres) smoke; @WebMvcTest p/ health/erros.
