package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnnouncementTagRepository extends JpaRepository<AnnouncementTag, UUID> {

    List<AnnouncementTag> findByAnnouncementId(UUID announcementId);

    @Query("""
            SELECT t.hubEntityId FROM AnnouncementTag at
            JOIN at.tag t
            WHERE at.announcement.id = :announcementId
              AND t.entityType = com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType.SHIFT
              AND t.active = true
            """)
    List<String> findActiveShiftHubEntityIdsByAnnouncementId(@Param("announcementId") UUID announcementId);

    void deleteByAnnouncementId(UUID announcementId);

    void deleteByAnnouncementIdAndTagId(UUID announcementId, UUID tagId);
}