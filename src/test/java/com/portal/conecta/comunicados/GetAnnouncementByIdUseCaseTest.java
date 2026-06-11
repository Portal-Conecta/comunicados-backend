package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.query.GetAnnouncementByIdQuery;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.GetAnnouncementByIdUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAnnouncementByIdUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AnnouncementPermissionValidator permissionValidator;

    @Mock
    private HubCoursePort hubCoursePort;

    @InjectMocks
    private GetAnnouncementByIdUseCase useCase;

    private Announcement announcementWithDestination(AnnouncementDestinationType type, UUID referenceId) {
        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado")
                .files(List.of())
                .tags(List.of())
                .mentions(List.of())
                .build();

        AnnouncementDestination destination = AnnouncementDestination.builder()
                .id(UUID.randomUUID())
                .announcement(announcement)
                .type(type)
                .referenceId(referenceId)
                .build();

        announcement.setDestinations(List.of(destination));
        return announcement;
    }

    @Test
    void shouldReturnAnnouncement_WhenViewAllProfile() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        Announcement announcement = announcementWithDestination(AnnouncementDestinationType.CLASS, UUID.randomUUID());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.of(announcement));
        when(permissionValidator.canViewAll(UserType.SENAI)).thenReturn(true);

        Announcement result = useCase.execute(new GetAnnouncementByIdQuery(id, userId));

        assertThat(result).isSameAs(announcement);
    }

    @Test
    void shouldThrowNotFound_WhenRemovedOrMissing() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.STUDENT, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new GetAnnouncementByIdQuery(id, userId)))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldThrowNotFound_WhenRestrictedProfileOutOfScope() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherClassId = UUID.randomUUID();
        UUID myClassId = UUID.randomUUID();
        RequestContext context = new RequestContext(
                userId, UserType.STUDENT, List.of(new ContextClass(myClassId, null)));

        Announcement announcement = announcementWithDestination(AnnouncementDestinationType.CLASS, otherClassId);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.of(announcement));
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(new GetAnnouncementByIdQuery(id, userId)))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldReturnAnnouncement_WhenRestrictedProfileInOwnClass() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID myClassId = UUID.randomUUID();
        RequestContext context = new RequestContext(
                userId, UserType.STUDENT, List.of(new ContextClass(myClassId, null)));

        Announcement announcement = announcementWithDestination(AnnouncementDestinationType.CLASS, myClassId);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.of(announcement));
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of());

        Announcement result = useCase.execute(new GetAnnouncementByIdQuery(id, userId));

        assertThat(result).isSameAs(announcement);
    }

    @Test
    void shouldReturnAnnouncement_WhenRestrictedProfileInOwnCourse() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID myCourseId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.STUDENT, List.of());

        Announcement announcement = announcementWithDestination(AnnouncementDestinationType.COURSE, myCourseId);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.of(announcement));
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of(myCourseId));

        Announcement result = useCase.execute(new GetAnnouncementByIdQuery(id, userId));

        assertThat(result).isSameAs(announcement);
    }

    @Test
    void shouldReturnAnnouncement_WhenGeneralDestination() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.STUDENT, List.of());

        Announcement announcement = announcementWithDestination(AnnouncementDestinationType.GENERAL, null);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(announcementRepository.findByIdAndRemovedAtIsNull(id)).thenReturn(Optional.of(announcement));
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of());

        Announcement result = useCase.execute(new GetAnnouncementByIdQuery(id, userId));

        assertThat(result).isSameAs(announcement);
    }
}
