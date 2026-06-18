package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.ScheduleAnnouncementRequest;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Dados de criação + agendamento atômico (#108). O comunicado nasce {@code SCHEDULED} com
 * {@code scheduledFor} futuro; o autor é o usuário autenticado.
 */
public record ScheduleAnnouncementCommand(

    String title,
    String description,
    AnnouncementOrigin origin,
    boolean pinned,
    Instant scheduledFor,
    UUID authorUserId,
    UserType authorUserType,
    List<CreateAnnouncementDestinationInput> destinations

) {

    public static ScheduleAnnouncementCommand from(ScheduleAnnouncementRequest request, RequestContext context) {
        return new ScheduleAnnouncementCommand(
                request.title(),
                request.description(),
                request.origin(),
                request.isPinned(),
                request.scheduledFor(),
                context.userId(),
                context.userType(),
                request.destinations()
        );
    }

    public Announcement toEntity(Instant now) {
        return Announcement.builder()
                .title(title)
                .description(description)
                .origin(origin)
                .status(AnnouncementStatus.SCHEDULED)
                .pinned(pinned)
                .createdByUserId(authorUserId)
                .createdByUserType(authorUserType)
                .scheduledFor(scheduledFor)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public List<AnnouncementDestination> toDestinations(Announcement announcement) {
        return destinations.stream()
                .map(destination -> AnnouncementDestination.builder()
                        .announcement(announcement)
                        .type(destination.type())
                        .referenceId(destination.referenceId())
                        .build())
                .toList();
    }

    public AnnouncementHistory toCreationHistory(Announcement announcement, Instant now) {
        return history(announcement, AnnouncementHistoryAction.CREATION,
                "Comunicado criado: " + announcement.getTitle(), now);
    }

    public AnnouncementHistory toScheduleHistory(Announcement announcement, Instant now) {
        return history(announcement, AnnouncementHistoryAction.SCHEDULED,
                "Comunicado agendado: " + announcement.getTitle() + " | Agendado para: " + scheduledFor, now);
    }

    private AnnouncementHistory history(
            Announcement announcement,
            AnnouncementHistoryAction action,
            String snapshot,
            Instant now
    ) {
        return AnnouncementHistory.builder()
                .announcement(announcement)
                .userId(authorUserId)
                .action(action)
                .snapshot(snapshot)
                .createdAt(now)
                .build();
    }
}
