# spec.md — F05-docs-hardening

## Objetivo
Documentação completa, hardening, revisão de erros ponta a ponta, preparação de entrega e compose com Ollama.

## Requisitos cobertos
RNF-04, RNF-05, ERR-01..ERR-06, DOC-01, DEL-01, DEL-02

## User stories

US-040: README (DOC-01)
- 7 seções: Descrição, Tecnologias, Arquitetura (com diagrama), Configuração do LLM (modelo, provider, config, como rodar), Execução (backend, frontend, banco, LLM), Recursos de IA (prompts, funcionalidades, structured output, Tool Calling), Decisões técnicas. Bibliotecas extras justificadas.

US-041: Entrega (DEL-01, DEL-02)
- Estrutura backend/, frontend/, README.md, docker-compose.yml. Roteiro de demonstração preparado.

US-042: Hardening (ERR-01..ERR-06)
- Revisar todos cenários de erro ponta a ponta com ProblemDetail sem stack trace.

US-043: Ollama compose (RNF-04)
- Serviço ollama + pull automático do modelo + healthcheck; instruções se rodar fora do Docker.

## Casos de borda
Primeiro pull demorado, sem GPU.

## Erros esperados
Todos.

## Fora de escopo
Funcionalidades novas.

## Perguntas em aberto
Nenhuma crítica.
