package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnnouncementTagRepository extends JpaRepository<AnnouncementTag, UUID> {

    List<AnnouncementTag> findByAnnouncementId(UUID announcementId);

    @Query("""
            SELECT at FROM AnnouncementTag at
            JOIN FETCH at.tag
            JOIN FETCH at.announcement
            WHERE at.announcement.id IN :announcementIds
            """)
    List<AnnouncementTag> findByAnnouncementIdIn(@Param("announcementIds") Collection<UUID> announcementIds);

    @Query("""
            SELECT t.hubEntityId FROM AnnouncementTag at
            JOIN at.tag t
            WHERE at.announcement.id = :announcementId
              AND t.entityType = :entityType
              AND t.active = true
            """)
    List<String> findActiveHubEntityIdsByAnnouncementIdAndEntityType(
            @Param("announcementId") UUID announcementId,
            @Param("entityType") TagEntityType entityType);

    void deleteByAnnouncementId(UUID announcementId);

    void deleteByAnnouncementIdAndTagId(UUID announcementId, UUID tagId);
}