# commit-convention.md — Padrão de commit (um commit por task)

Formato:
<verbo>: <Título curto no imperativo, sem ponto final>
 - <Descrição importante>
 - <Descrição importante>
 - Task: T-<ID> | Reqs: RF-XX,...

Verbos: add, fix, update, remove, refactor, test, docs, configure, chore.
`feat` não é usado: qualquer entrega que altera o produto usa `add`.

Regras: um commit por task, coeso, multilinha real, cita task e requisitos, não
genérico.

Processo:
- Reescrever o histórico (rebase, amend, force-push) só com pedido explícito do
  usuário — nunca por iniciativa do agente.
- Não existe commit dedicado "Registra o hash": os hashes entram na verificação
  final (TRACEABILITY.md assim que a task fecha).