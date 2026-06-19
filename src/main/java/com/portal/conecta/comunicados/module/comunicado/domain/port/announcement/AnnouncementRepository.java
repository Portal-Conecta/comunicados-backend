package com.portal.conecta.comunicados.module.comunicado.domain.port.announcement;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;

import java.lang.ScopedValue;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {
    Optional<Announcement> findById(UUID id);
    Announcement save(Announcement announcement);

    ScopedValue<Object> findByIdAndRemovedAtIsNull(UUID uuid);
}