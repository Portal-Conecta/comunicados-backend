-- #174: índice de suporte à limpeza de AnnouncementFile PENDING expirados.
-- CleanOrphanedFilesUseCase roda de hora em hora via findByFileStatusAndCreatedAtBefore;
-- sem índice, cada execução faz full scan de announcement_file.

CREATE INDEX idx_announcement_file_status_created_at
    ON announcement_file (file_status, created_at);
