-- Issue #113: sincronização de tags via eventos do Core (RabbitMQ)

ALTER TABLE tag ADD COLUMN IF NOT EXISTS hub_entity_id UUID;
ALTER TABLE tag ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE tag SET hub_entity_id = id WHERE hub_entity_id IS NULL;
UPDATE tag SET updated_at = created_at WHERE updated_at IS NULL;

ALTER TABLE tag ALTER COLUMN hub_entity_id SET NOT NULL;
ALTER TABLE tag ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE tag DROP CONSTRAINT IF EXISTS uk_tag_entity_hub;
ALTER TABLE tag ADD CONSTRAINT uk_tag_entity_hub UNIQUE (entity_type, hub_entity_id);

CREATE TABLE IF NOT EXISTS processed_event (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);
