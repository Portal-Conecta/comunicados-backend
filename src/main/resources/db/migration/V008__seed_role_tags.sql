-- V007 — tags de papel de usuário (ROLE) sem evento Core (#somente-professores)
--
-- Papel do usuário é enum fixo local (UserType), sem entidade correspondente no Core;
-- semeadas localmente como as tags GENERAL (V002) e SHIFT (V006).
-- hub_entity_id armazena o nome do enum UserType (STUDENT, REPRESENTATIVE, TEACHER, SENAI, WEG, ADMIN).

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Alunos', 'ROLE', 'STUDENT', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'STUDENT');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Responsáveis', 'ROLE', 'REPRESENTATIVE', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'REPRESENTATIVE');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Professores', 'ROLE', 'TEACHER', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'TEACHER');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'SENAI', 'ROLE', 'SENAI', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'SENAI');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'WEG', 'ROLE', 'WEG', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'WEG');

INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
SELECT gen.uuid(), 'Administradores', 'ROLE', 'ADMIN', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM tag WHERE entity_type = 'ROLE' AND hub_entity_id = 'ADMIN');
