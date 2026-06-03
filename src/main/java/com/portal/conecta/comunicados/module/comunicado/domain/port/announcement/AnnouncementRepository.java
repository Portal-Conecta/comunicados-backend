package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByIdAndRemovedAtIsNull(UUID id);

    List<Announcement> findByRemovedAtIsNullOrderByPublishedAtDesc();

    List<Announcement> findByPinnedTrueAndRemovedAtIsNullOrderByPinnedOrderAsc();
}
