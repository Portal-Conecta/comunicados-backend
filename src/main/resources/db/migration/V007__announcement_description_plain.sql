-- Rich text: description passa a ser HTML sanitizado; description_plain guarda o texto sem markup
-- para mural, cards e busca. Backfill: registros atuais (texto puro) copiam description -> description_plain.

ALTER TABLE announcement
    ADD COLUMN description_plain text;

UPDATE announcement
SET description_plain = description
WHERE description_plain IS NULL;

ALTER TABLE announcement
    ALTER COLUMN description_plain SET NOT NULL;
