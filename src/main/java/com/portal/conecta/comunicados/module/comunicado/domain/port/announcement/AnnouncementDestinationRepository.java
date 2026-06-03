package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementDestinationRepository extends JpaRepository<AnnouncementDestination, UUID> {

    List<AnnouncementDestination> findByAnnouncementId(UUID announcementId);

    void deleteByAnnouncementId(UUID announcementId);
}
