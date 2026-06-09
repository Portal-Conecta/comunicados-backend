package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.PublishAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublishAnnouncementUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository announcementHistoryRepository;

    public PublishAnnouncementUseCase(
            AnnouncementRepository announcementRepository,
            AnnouncementHistoryRepository announcementHistoryRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.announcementHistoryRepository = announcementHistoryRepository;
    }

    @Transactional
    public Announcement execute(PublishAnnouncementCommand command) {
        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(command.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comunicado não encontrado!"));

        validateCanPublish(announcement, command);

        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(Instant.now());
        announcement.setPublishedByUserId(command.publishedByUserId());

        Announcement savedAnnouncement = announcementRepository.save(announcement);
        saveHistory(savedAnnouncement, command);

        return savedAnnouncement;
    }

    private void validateCanPublish(Announcement announcement, PublishAnnouncementCommand command) {
        validatePublisher(command);
        validateStatus(announcement);
        validateRequiredFields(announcement);
    }

    private void validatePublisher(PublishAnnouncementCommand command) {
        if (command.publishedByUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário responsável pela publicação é obrigatório!");
        }
    }

    private void validateStatus(Announcement announcement) {
        if (announcement.getStatus() != AnnouncementStatus.DRAFT && announcement.getStatus() != AnnouncementStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente comunicados em rascunho ou agendados podem ser publicados!");
        }
    }

    private void validateRequiredFields(Announcement announcement) {
        if (isBlank(announcement.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório para publicar o comunicado!");
        }

        if (isBlank(announcement.getDescription())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição é obrigatória para publicar o comunicado!");
        }

        if (announcement.getDestinations() == null || announcement.getDestinations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pelo menos um destino é obrigatório para publicar o comunicado!");
        }
    }

    private void saveHistory(Announcement announcement, PublishAnnouncementCommand command) {
        AnnouncementHistory history = new AnnouncementHistory();

        history.setAnnouncement(announcement);
        history.setUserId(command.publishedByUserId());
        history.setAction(AnnouncementHistoryAction.PUBLICATION);
        history.setSnapshot(createSnapshot(announcement));
        history.setCreatedAt(Instant.now());

        announcementHistoryRepository.save(history);
    }

    private String createSnapshot(Announcement announcement) {
        return "Comunicado publicado: "
                + announcement.getTitle()
                + " | status: "
                + announcement.getStatus();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}