-- #170: status de processamento assíncrono em announcement_file.
-- file_status: PENDING (aguardando processamento) / READY / FAILED.
-- processed_s3: chave do arquivo já processado no bucket de destino (B).

ALTER TABLE announcement_file
    ADD COLUMN file_status  varchar(255),
    ADD COLUMN processed_s3 varchar(255);

-- Registros pré-existentes vieram do fluxo síncrono e já estavam disponíveis;
-- marca como READY para não aparecerem como "em processamento".
UPDATE announcement_file
SET file_status = 'READY'
WHERE file_status IS NULL;

ALTER TABLE announcement_file
    ALTER COLUMN file_status SET NOT NULL;
