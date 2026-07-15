package com.portal.conecta.comunicados.module.comunicado.domain.port;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.shared.context.UserType;

public interface AnnouncementNotificationPublisher {

    void publish(Announcement announcement, List<AnnouncementDestination> destinations, List<UserType> roles);
}
