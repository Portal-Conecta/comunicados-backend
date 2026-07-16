-- V010 — renomeia tag ROLE REPRESENTATIVE: Responsáveis → Representantes

UPDATE tag
SET name = 'Representantes',
    updated_at = now()
WHERE entity_type = 'ROLE'
  AND hub_entity_id = 'REPRESENTATIVE'
  AND name <> 'Representantes';
