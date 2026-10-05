# tasks.md — F02-task-management

### T-F02-01 — Entidade, migração e repositório Task
- Status: pending
- Reqs: RF-07,RNF-21
- Depende de: T-F01-04
- Arquivos: task/domain/Task.java, task/infra/TaskRepository.java, resources/db/migration/V2__create_tasks.sql
- O que fazer: Modelar Task com enums, parentTaskId, índices.
- Pronto quando: Migração aplicada em teste, entidade mapeada.
- Testes: TST-04 integração repositório.
- Gate: cd backend && mvn -q test -Dtest=*TaskRepository*Test* 2>&1 | tail -2
- Commit: dd: Entidade e migração de Task

### T-F02-02 — DTOs, mapper, validações
- Status: pending
- Reqs: RF-01,RF-07,ERR-02
- Depende de: T-F02-01
- Arquivos: task/application/dto/*, task/application/mapper/*
- O que fazer: Records, Bean Validation, mapeamentos.
- Pronto quando: DTOs validados.
- Testes: TST-01 unitário mapper/validação.
- Gate: cd backend && mvn -q test -Dtest=*TaskMapper*Test* 2>&1 | tail -1
- Commit: dd: DTOs e mapper de Task

### T-F02-03 — Service CRUD + status + summary
- Status: pending
- Reqs: RF-01..RF-09,RF-20
- Depende de: T-F02-02
- Arquivos: task/application/TaskService.java
- O que fazer: Regras de negócio, transições, summary.
- Pronto quando: Regras atendem critérios.
- Testes: TST-01 TaskServiceTest.
- Gate: cd backend && mvn -q test -Dtest=*TaskService*Test* 2>&1 | tail -5
- Commit: dd: Service de tarefas (CRUD, status e summary)

### T-F02-04 — Controller REST + erro 404/400/500
- Status: pending
- Reqs: RF-23,ERR-01,ERR-02,ERR-06
- Depende de: T-F02-03
- Arquivos: task/api/TaskController.java
- O que fazer: Endpoints conforme design, mapear erros.
- Pronto quando: Contratos respeitados.
- Testes: TST-02 TaskControllerTest (@WebMvcTest).
- Gate: cd backend && mvn -q test -Dtest=*TaskController*Test* 2>&1 | tail -5
- Commit: dd: Controller REST de tarefas

### T-F02-05 — Frontend Dashboard + Tarefas (CRUD + status + IA)
- Status: pending
- Reqs: RF-20,RF-21
- Depende de: T-F02-04
- Arquivos: frontend/src/api/tasks.ts, frontend/src/pages/*, frontend/src/components/task/*
- O que fazer: Listar, criar/editar, alterar status, excluir, dashboard, botão IA.
- Pronto quando: Build passa; fluxos básicos.
- Testes: Vitest + Testing Library componentes críticos.
- Gate: cd frontend && npm run build && npx vitest run --reporter=basic 2>&1 | tail -10
- Commit: dd: Frontend Dashboard e Tarefas (CRUD + status)
