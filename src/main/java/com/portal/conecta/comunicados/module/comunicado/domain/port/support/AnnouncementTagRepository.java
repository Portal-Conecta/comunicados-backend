package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementTagRepository extends JpaRepository<AnnouncementTag, UUID> {

    List<AnnouncementTag> findByAnnouncementId(UUID announcementId);

    void deleteByAnnouncementIdAndTagId(UUID announcementId, UUID tagId);
}