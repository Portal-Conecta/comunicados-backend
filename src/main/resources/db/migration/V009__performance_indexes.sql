-- V009 — indices de performance validados com EXPLAIN ANALYZE em base sintetica de 200k comunicados
--
-- 1) Fixados: a listagem roda "WHERE removed_at IS NULL AND pinned = true" toda vez que a home
--    carrega, e antes disso era sequential scan da tabela inteira (~12ms/200k linhas) so pra achar
--    uma dezena de fixados. Indice parcial deixa isso em ~0.03ms.
CREATE INDEX idx_announcement_pinned
    ON announcement (pinned_order)
    WHERE pinned = true AND removed_at IS NULL;

-- 2) Busca por texto (?search=): hoje e um LIKE '%termo%' em title/description_plain, que nao usa
--    indice btree e degrada linearmente com o tamanho da tabela (sequential scan). Trigram GIN
--    resolve LIKE parcial e escala muito melhor conforme a tabela cresce.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_announcement_title_trgm
    ON announcement USING gin (lower(title) gin_trgm_ops);

CREATE INDEX idx_announcement_description_plain_trgm
    ON announcement USING gin (lower(description_plain) gin_trgm_ops);
