package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID>, JpaSpecificationExecutor<Announcement> {

    Optional<Announcement> findByIdAndRemovedAtIsNull(UUID id);

    List<Announcement> findByRemovedAtIsNullOrderByPublishedAtDesc();

    List<Announcement> findByPinnedTrueAndRemovedAtIsNullOrderByPinnedOrderAsc();

    /**
     * Comunicados agendados cujo horário já chegou e que ainda não foram publicados/removidos.
     * Base da publicação automática (#135).
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status = com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus.SCHEDULED
              AND a.removedAt IS NULL
              AND a.scheduledFor <= :now
            """)
    List<Announcement> findDueForPublication(@Param("now") Instant now);

    /**
     * Marca um agendado como publicado de forma atômica (compare-and-swap): só atualiza se ainda
     * estiver {@code SCHEDULED} e vencido. Retorna 1 se esta execução fez a transição e 0 caso
     * contrário — torna a publicação idempotente e segura entre múltiplas réplicas, sem lock externo.
     */
    @Modifying
    @Query("""
            UPDATE Announcement a
            SET a.status = com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus.PUBLISHED,
                a.publishedAt = :now,
                a.publishedByUserId = a.createdByUserId,
                a.updatedAt = :now
            WHERE a.id = :id
              AND a.status = com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus.SCHEDULED
              AND a.removedAt IS NULL
              AND a.scheduledFor <= :now
            """)
    int markScheduledAsPublished(@Param("id") UUID id, @Param("now") Instant now);
}
