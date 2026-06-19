package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fronteira transacional de uma publicação automática (#135). Cada comunicado é publicado em sua
 * própria transação, então a falha de um não impede os demais.
 */
@Component
@RequiredArgsConstructor
public class AnnouncementPublisher {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;

    /**
     * Tenta publicar o comunicado agendado via compare-and-swap. Só grava o histórico
     * ({@code PUBLICATION}) se esta execução tiver vencido a corrida (outra réplica pode ter
     * publicado antes). A publicação é atribuída a quem agendou ({@code createdByUserId}): o job
     * apenas executa, no horário marcado, a decisão que aquele usuário já tomou no {@code POST /schedule}.
     *
     * @return {@code true} se este processo efetuou a publicação; {@code false} caso já estivesse publicado.
     */
    @Transactional
    public boolean publish(Announcement announcement, Instant now) {
        int updated = announcementRepository.markScheduledAsPublished(announcement.getId(), now);
        if (updated == 0) {
            return false;
        }

        AnnouncementHistory history = AnnouncementHistory.builder()
                .announcement(announcementRepository.getReferenceById(announcement.getId()))
                .userId(announcement.getCreatedByUserId())
                .action(AnnouncementHistoryAction.PUBLICATION)
                .snapshot("Comunicado publicado automaticamente: " + announcement.getTitle())
                .createdAt(now)
                .build();
        announcementHistoryRepository.save(history);

        return true;
    }
}
