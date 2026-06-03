package com.portal.conecta.comunicados.module.comunicado.domain.port;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementHistoryRepository extends JpaRepository<AnnouncementHistory, UUID> {

    List<AnnouncementHistory> findByAnnouncementIdOrderByCreatedAtDesc(UUID announcementId);
}