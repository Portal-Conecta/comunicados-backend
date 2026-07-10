package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnnouncementHistoryRepository extends JpaRepository<AnnouncementHistory, UUID> {

    Page<AnnouncementHistory> findByAnnouncementIdOrderByCreatedAtDesc(UUID announcementId, Pageable pageable);

}