# .agent/README.md

## Índice e ordem de leitura
Leia sempre nesta ordem antes de qualquer task:
1. .specs/project/STATE.md — memória viva, bloqueios, dúvidas ([NEEDS CLARIFICATION], [ASSUMPTION])
2. Task atual em .specs/features/<feature>/tasks.md (status in-progress)
3. .specs/features/<feature>/spec.md — requisitos e critérios de aceite
4. .specs/features/<feature>/design.md — contratos e arquitetura
5. .agent/rules/workflow.md — loop de execução (seção 4)

## Mapa
- ules/ — regras inegociáveis (workflow, commit, autonomia, anti-hallucination, estilo, IA, DoD)
- workflows/ — passos operacionais (new-task, resume-session)
