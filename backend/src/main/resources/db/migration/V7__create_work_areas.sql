-- V7__create_work_areas.sql
-- Areas de trabalho (F14): agrupamento de tarefas no estilo "mural" (Trello),
-- com nome e foto opcional. A imagem vai para o banco (BYTEA) junto com o
-- content type; a API nunca serializa os bytes na listagem — so via
-- GET /areas/{id}/image.

CREATE TABLE work_areas (
    id         UUID         PRIMARY KEY,
    title      VARCHAR(100) NOT NULL,
    image      BYTEA,
    image_type VARCHAR(50),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_work_areas_title CHECK (length(btrim(title)) > 0)
);

-- A area da tarefa e opcional: excluir a area preserva as tarefas, que voltam
-- a ficar sem area (ON DELETE SET NULL) — uma area nao e dona das tarefas.
ALTER TABLE tasks
    ADD COLUMN area_id UUID REFERENCES work_areas (id) ON DELETE SET NULL;

CREATE INDEX idx_tasks_area_id ON tasks (area_id);

COMMENT ON TABLE  work_areas            IS 'Areas de trabalho para agrupar tarefas';
COMMENT ON COLUMN work_areas.image      IS 'Foto da area (bytes), opcional';
COMMENT ON COLUMN work_areas.image_type IS 'Content-Type da foto (ex.: image/png), nulo junto com image';
COMMENT ON COLUMN tasks.area_id         IS 'Area de trabalho da tarefa (opcional); excluir a area zera esta coluna';