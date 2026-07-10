-- V002 — tag singleton GENERAL ("Geral") e suporte a hub_entity_id nulo (#151)
--
-- A tag GENERAL representa o público "todos" e não corresponde a nenhuma entidade
-- central do Core (curso/turma). Por isso seu `hub_entity_id` é nulo e ela é semeada
-- localmente, sem depender de evento do Core.
--
-- Por que cada passo:
--   1. hub_entity_id passa a aceitar NULL — GENERAL não tem entidade no Hub.
--   2. uk_tag_general garante o SINGLETON: a UK (entity_type, hub_entity_id) de V001
--      não cobre isso, pois no Postgres NULL é distinto de NULL e permitiria N GENERAL.
--   3. seed idempotente — Flyway já roda a versão uma vez; o WHERE NOT EXISTS protege
--      contra re-baseline/execução manual, mantendo a tag única em qualquer cenário.

ALTER TABLE tag ALTER COLUMN hub_entity_id DROP NOT NULL;

CREATE UNIQUE INDEX uk_tag_general ON tag (entity_type) WHERE entity_type = 'GENERAL';

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000001', 'Geral', 'GENERAL', NULL, true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'GENERAL');
