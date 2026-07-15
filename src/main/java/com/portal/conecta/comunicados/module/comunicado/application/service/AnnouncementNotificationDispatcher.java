package com.portal.conecta.comunicados.module.comunicado.application.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.AnnouncementNotificationPublisher;
import com.portal.conecta.comunicados.shared.context.UserType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara notificação após commit da transação de publicação, para não notificar
 * se o commit falhar e para não amarrar o broker à TX do JPA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementNotificationDispatcher {

    private final AnnouncementNotificationPublisher notificationPublisher;

    public void dispatchAfterCommit(
            Announcement announcement,
            List<AnnouncementDestination> destinations,
            List<UserType> roles
    ) {
        Runnable publish = () -> {
            try {
                notificationPublisher.publish(announcement, destinations, roles);
            } catch (Exception e) {
                log.error(
                        "Falha ao publicar notificação do comunicado {}. A publicação do post não será revertida.",
                        announcement.getId(),
                        e
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            publish.run();
        }
    }
}
