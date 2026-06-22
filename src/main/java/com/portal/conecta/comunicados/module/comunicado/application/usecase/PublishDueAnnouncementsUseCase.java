package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;

import lombok.RequiredArgsConstructor;

/**
 * Publica automaticamente os comunicados agendados cujo horário já chegou (#135).
 *
 * <p>Orquestra a busca e delega cada publicação ao {@link AnnouncementPublisher}, que abre uma
 * transação por comunicado. A leitura não é transacional de propósito: a transição real é feita
 * pelo {@code UPDATE} condicional do publisher, idempotente e seguro entre réplicas.</p>
 */
@Service
@RequiredArgsConstructor
public class PublishDueAnnouncementsUseCase {

    private static final Logger log = LoggerFactory.getLogger(PublishDueAnnouncementsUseCase.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementPublisher announcementPublisher;

    /**
     * @return quantidade de comunicados efetivamente publicados nesta execução.
     */
    public int execute() {
        Instant now = Instant.now();
        List<Announcement> due = announcementRepository.findDueForPublication(now);

        int published = 0;
        for (Announcement announcement : due) {
            try {
                if (announcementPublisher.publish(announcement, now)) {
                    published++;
                }
            } catch (RuntimeException ex) {
                log.error("Falha ao publicar automaticamente o comunicado {}", announcement.getId(), ex);
            }
        }
        return published;
    }
}
