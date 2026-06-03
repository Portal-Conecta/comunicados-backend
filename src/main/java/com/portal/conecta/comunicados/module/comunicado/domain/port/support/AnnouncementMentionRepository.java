package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementMention;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementMentionRepository extends JpaRepository<AnnouncementMention, UUID> {

    List<AnnouncementMention> findByAnnouncementId(UUID announcementId);
}