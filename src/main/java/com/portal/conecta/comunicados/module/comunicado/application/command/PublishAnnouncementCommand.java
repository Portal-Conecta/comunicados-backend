package com.portal.conecta.comunicados.module.comunicado.application.command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PublishAnnouncementRequest;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

/**
 * Dados de criação + publicação atômica (#107). O autor e o publicador são o usuário autenticado.
 */
public record PublishAnnouncementCommand(

    String title,
    String description,
    AnnouncementOrigin origin,
    boolean pinned,
    UUID authorUserId,
    UserType authorUserType,
    List<CreateAnnouncementDestinationInput> destinations,
    List<UUID> tagIds,
    List<ShiftCode> shiftCodes,
    List<UserType> roles

) {

    public static PublishAnnouncementCommand from(PublishAnnouncementRequest request, RequestContext context) {
        return new PublishAnnouncementCommand(
                request.title(),
                request.description(),
                request.origin(),
                request.isPinned(),
                context.userId(),
                context.userType(),
                request.destinations(),
                request.tagIds(),
                request.resolvedShiftCodes(),
                request.resolvedRoles()
        );
    }

    public Announcement toEntity(Instant now) {
        return Announcement.builder()
                .title(title)
                .description(description)
                .origin(origin)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(pinned)
                .createdByUserId(authorUserId)
                .createdByUserType(authorUserType)
                .publishedByUserId(authorUserId)
                .publishedAt(now)
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

    public AnnouncementHistory toPublicationHistory(Announcement announcement, Instant now) {
        return history(announcement, AnnouncementHistoryAction.PUBLICATION,
                "Comunicado publicado: " + announcement.getTitle(), now);
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
