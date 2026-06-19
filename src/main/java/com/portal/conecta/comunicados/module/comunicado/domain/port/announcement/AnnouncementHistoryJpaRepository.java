package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnnouncementHistoryJpaRepository extends JpaRepository<AnnouncementHistory, UUID>, AnnouncementHistoryRepository {
}