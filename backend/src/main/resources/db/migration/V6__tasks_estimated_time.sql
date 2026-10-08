-- V6__tasks_estimated_time.sql
-- Tempo estimado para realizar a tarefa (F12): valor numerico + unidade
-- (HOURS | DAYS). Colunas nulas juntas = sem estimativa. O valor segue o
-- mesmo teto das horas estimadas da IA, (0, 200], na unidade escolhida pelo
-- usuario — o analise/IA sempre grava HOURS.

ALTER TABLE tasks
    ADD COLUMN estimated_time DOUBLE PRECISION,
    ADD COLUMN estimated_unit VARCHAR(10);

COMMENT ON COLUMN tasks.estimated_time IS 'Tempo estimado para realizar a tarefa, na unidade informada (0, 200]';
COMMENT ON COLUMN tasks.estimated_unit IS 'Unidade do tempo estimado: HOURS ou DAYS';