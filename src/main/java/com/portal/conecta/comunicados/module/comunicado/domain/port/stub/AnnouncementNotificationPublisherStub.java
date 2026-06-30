package com.portal.conecta.comunicados.module.comunicado.domain.port.stub;

import java.util.List;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.AnnouncementNotificationPublisher;

public class AnnouncementNotificationPublisherStub implements AnnouncementNotificationPublisher {

    @Override
    public void publish(Announcement announcement, List<AnnouncementDestination> destinations) {
    }
}
