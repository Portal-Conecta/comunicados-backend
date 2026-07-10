-- V006 — tags de turno (SHIFT) sem evento Core (#195/#199)
--
-- Turnos são enum fixo no Core; semeados localmente como a tag GENERAL (V002).
-- hub_entity_id armazena o código do enum (FULL_AM_PM, FULL_PM_NT).

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Manhã e tarde', 'SHIFT', 'FULL_AM_PM', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'SHIFT' AND hub_entity_id = 'FULL_AM_PM');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Tarde e noite', 'SHIFT', 'FULL_PM_NT', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'SHIFT' AND hub_entity_id = 'FULL_PM_NT');
