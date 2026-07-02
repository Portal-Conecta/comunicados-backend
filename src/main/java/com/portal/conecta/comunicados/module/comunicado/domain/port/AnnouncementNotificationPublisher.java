package com.portal.conecta.comunicados.module.comunicado.domain.port;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;

public interface AnnouncementNotificationPublisher {

    void publish(Announcement announcement, List<AnnouncementDestination> destinations);
}
