-- V2__create_tasks.sql
-- Tabela de tarefas. Dona da F02 (task-management).
-- Colunas em snake_case; a entidade Task mapeia explicitamente cada uma.
--
-- Os valores de status e priority sao os mesmos que o Java expoe (decisao do
-- usuario: contrato em portugues). Alterar um valor aqui exige alterar a enum
-- correspondente e vice-versa.

CREATE TABLE tasks (
    id           UUID         PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'A_FAZER',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIA',
    due_date     DATE,
    parent_id    UUID         REFERENCES tasks (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_tasks_title    CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_tasks_status   CHECK (status IN ('A_FAZER', 'EM_ANDAMENTO', 'CONCLUIDA')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('BAIXA', 'MEDIA', 'ALTA')),
    -- uma tarefa nao pode ser pai de si mesma
    CONSTRAINT ck_tasks_parent   CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX idx_tasks_status     ON tasks (status);
CREATE INDEX idx_tasks_priority   ON tasks (priority);
CREATE INDEX idx_tasks_due_date   ON tasks (due_date);
CREATE INDEX idx_tasks_parent_id  ON tasks (parent_id);

COMMENT ON TABLE  tasks             IS 'Tarefas do AI Task Manager';
COMMENT ON COLUMN tasks.parent_id   IS 'Tarefa pai quando esta linha e uma subtarefa criada por decomposicao de IA';
COMMENT ON COLUMN tasks.due_date    IS 'Prazo em data (sem hora), ISO-8601 yyyy-MM-dd';
