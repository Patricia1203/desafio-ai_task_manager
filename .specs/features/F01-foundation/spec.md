# spec.md — F01-foundation

## Objetivo
Estabelecer base do projeto (estrutura, infraestrutura mínima, padrões, configuração e saúde da aplicação) para suportar tarefas e IA.

## Requisitos cobertos
RNF-01, RNF-03, RNF-04, RNF-05, RNF-20, RNF-21, TST-04

## User stories

US-001: Como desenvolvedor, quero skeleton backend Spring Boot 4 (Java 21) com camadas organizadas para garantir separação de responsabilidades.
Critérios de aceite (verificáveis):
- Given repositório vazio, When inicializado backend, Then projeto compila com Java 21 e Boot 4.
- Given estrutura, When inspecionada, Then segue pacotes por feature com camadas (api/application/domain/infra/common) conforme design.

US-002: Como desenvolvedor, quero skeleton frontend React (Vite) para interface.
- Given backend presente, When criado frontend, Then build Vite passa.
- Given estrutura, Then separação api/ componentes/ páginas com tipagem.

US-003: Como devops, quero docker-compose com postgres, backend, frontend (e Ollama preparado) para execução simplificada.
- Given docker-compose.yml, When validado (docker compose config), Then válido.
- Given compose, Then serviços com healthchecks e volumes definidos.

US-004: Como desenvolvedor, quero Flyway + schema base + JPA ddl-auto=validate e handler global ProblemDetail.
- Given migração base, When aplicada, Then schema válido.
- Given erro de negócio/validação/LLM, When ocorrido, Then resposta ProblemDetail sem stack trace (ERRO base).
- Given config por env, Then application.yml lê variáveis; .env.example existe.

## Casos de borda
- Config ausente com valores padrão seguros.
- Erros mapeados sem vazamento.

## Erros esperados
ERR-06 (genérico persistência) parcialmente coberto por base.

## Fora de escopo
Funcionalidades de IA nesta feature.

## Perguntas em aberto
[NEEDS CLARIFICATION] Versões exatas Boot 4.x e Spring AI compatível? (verificar)
