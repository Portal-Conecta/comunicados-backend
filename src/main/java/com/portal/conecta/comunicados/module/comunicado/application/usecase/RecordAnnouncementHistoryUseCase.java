package com.portal.conecta.comunicados.module.comunicado.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.application.command.RecordAnnouncementHistoryCommand;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementHistoryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordAnnouncementHistoryUseCase {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementHistoryRepository historyRepository;

    @Transactional
    public AnnouncementHistory record(
            UUID announcementId,
            AnnouncementHistoryAction action,
            UUID userId,
            String snapshot
    ) {
        Objects.requireNonNull(announcementId, "announcementId must not be null.");
        Objects.requireNonNull(action, "action must not be null.");
        Objects.requireNonNull(userId, "userId must not be null.");

        Announcement announcement = announcementRepository.findByIdAndRemovedAtIsNull(announcementId)
                .orElseThrow(AnnouncementNotFoundException::new);

        CreateAnnouncementHistoryRequest request = new CreateAnnouncementHistoryRequest(
                announcementId,
                userId,
                action,
                snapshot
        );

        RecordAnnouncementHistoryCommand command = RecordAnnouncementHistoryCommand.fromRequest(request);

        return historyRepository.save(command.toEntity(announcement, Instant.now()));
    }

}