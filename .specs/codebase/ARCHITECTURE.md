# ARCHITECTURE.md
Camadas: api, application, domain, infra, common. Port/adaptador para IA (business não depende de Spring AI). Pacotes por feature.
Diagrama (mermaid): TBD após bootstrap.
Fluxo IA: requisição → controller → application service → ai.port → ai.adapter (Spring AI) → validação pós-LLM → resposta.