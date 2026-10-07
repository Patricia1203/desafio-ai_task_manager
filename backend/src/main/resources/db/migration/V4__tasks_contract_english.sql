-- V4__tasks_contract_english.sql
-- Revisao F06: o contrato (valores de status e priority) volta para ingles,
-- seguindo a documentacao do desafio. V2 usava valores em portugues
-- (decisao anterior); esta migration converte dados e constraints sem tocar na
-- V2 (historico imutavel).
--
-- Os valores sao os mesmos que o Java expoe nas enums TaskStatus e
-- TaskPriority. Alterar um valor aqui exige alterar a enum correspondente e
-- vice-versa.

-- 1) Remove as constraints antigas antes de converter os valores.
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS ck_tasks_status;
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS ck_tasks_priority;

-- 2) Converte os dados ja persistidos.
UPDATE tasks SET status = 'TODO'         WHERE status = 'A_FAZER';
UPDATE tasks SET status = 'IN_PROGRESS'  WHERE status = 'EM_ANDAMENTO';
UPDATE tasks SET status = 'DONE'         WHERE status = 'CONCLUIDA';
UPDATE tasks SET priority = 'LOW'        WHERE priority = 'BAIXA';
UPDATE tasks SET priority = 'MEDIUM'     WHERE priority = 'MEDIA';
UPDATE tasks SET priority = 'HIGH'       WHERE priority = 'ALTA';

-- 3) Alinha os defaults dos inserts futuros.
ALTER TABLE tasks ALTER COLUMN status   SET DEFAULT 'TODO';
ALTER TABLE tasks ALTER COLUMN priority SET DEFAULT 'MEDIUM';

-- 4) Recria as constraints com os valores em ingles.
ALTER TABLE tasks ADD CONSTRAINT ck_tasks_status   CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));
ALTER TABLE tasks ADD CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

COMMENT ON COLUMN tasks.status   IS 'Ciclo de vida: TODO -> IN_PROGRESS -> DONE (DONE e terminal; reabrir exige TODO)';
COMMENT ON COLUMN tasks.priority IS 'Prioridade LOW, MEDIUM ou HIGH (default MEDIUM)';