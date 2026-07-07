-- V005 — descrição do comunicado como texto livre (sem limite de 255 caracteres)
--
-- O título permanece varchar(255). Apenas description passa a aceitar conteúdo longo.

ALTER TABLE announcement
    ALTER COLUMN description TYPE text;
