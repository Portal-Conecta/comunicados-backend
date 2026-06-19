package com.portal.conecta.comunicados.module.comunicado.infrastructure.persistence;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementDestinationJpaRepository
        extends JpaRepository<AnnouncementDestination, UUID>, AnnouncementDestinationRepository {

    List<AnnouncementDestination> findByAnnouncementId(UUID announcementId);
    void deleteByAnnouncementId(UUID announcementId);
}