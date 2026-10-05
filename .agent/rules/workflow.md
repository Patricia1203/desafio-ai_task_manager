# workflow.md — Loop de execução de task (obrigatório)

## 4.1 Passos

1. Contexto: ler STATE.md, a task em tasks.md, o spec.md e design.md da feature. Marcar a task como in-progress (em tasks.md e STATE.md).
2. Implementar exatamente o escopo da task (sem antecipar tasks futuras).
3. Testar: escrever os testes previstos na task e rodar o Gate da task.
4. Suíte relevante: rodar a suíte completa do módulo afetado (backend/frontend). Tudo verde.
5. Revisão crítica: reler diff contra checklist de qualidade (4.2).
6. Aplicar melhorias conforme regra de autonomia (4.3). Rodar testes novamente se aplicadas.
7. Atualizar documentação viva: tasks.md (done), TRACEABILITY.md, STATE.md, README/design se necessário.
8. Commit único da task conforme padrão (seção 5).
9. Relatar em 3–6 linhas e seguir para próxima task.

## 4.2 Definition of Done
- [ ] Critérios "Pronto quando" atendidos e ligados ao spec
- [ ] Testes novos escritos; todos passam (Gate + suíte do módulo)
- [ ] Sem TODO/código morto/System.out/segredos
- [ ] Erros tratados via handler global (sem stack trace na resposta)
- [ ] Entradas validadas e saídas do LLM validadas
- [ ] Nenhuma dependência nova sem verificação e justificativa
- [ ] TRACEABILITY.md, tasks.md e STATE.md atualizados
- [ ] Mensagem de commit conforme padrão

## 4.3 Regras de autonomia
Aplicar direto (pequeno/local, sem risco): refatoração interna, renomear privado, extrair, remover duplicação, melhorar mensagem de erro, validação/teste faltante, corrigir bug, ajustar logs, UX menor, prompt sem mudar contrato. Registrar em STATE.md > Melhorias aplicadas.

PARAR e registrar em STATE.md > Melhorias propostas (perguntar ao usuário) quando: mudança de contrato público; adicionar/trocar dependência relevante, versão major, modelo/provedor; arquitetura; operação destrutiva/riscosa; segurança/privacidade (CORS, segredos, dados ao LLM); testes só passam mudando asserção para caber (investigar causa raiz); após 3 tentativas fundamentadas de corrigir o mesmo erro → Bloqueios.

Nunca: commitar com testes falhando; --no-verify; git push --force; pular/apagar testes; marcar done sem rodar Gate; commitar .env/segredos; git push sem pedido explícito.