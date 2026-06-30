package com.portal.conecta.comunicados.module.comunicado.domain.port.stub;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.AnnouncementNotificationPublisher;

@Component
@ConditionalOnProperty(prefix = "app.messaging", name = "enabled", havingValue = "false", matchIfMissing = true)
public class AnnouncementNotificationPublisherStub implements AnnouncementNotificationPublisher {

    @Override
    public void publish(Announcement announcement, List<AnnouncementDestination> destinations) {
    }
}
