package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListPinnedAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubShiftPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListPinnedAnnouncementsUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AnnouncementPermissionValidator permissionValidator;

    @Mock
    private HubCoursePort hubCoursePort;

    @Mock
    private HubShiftPort hubShiftPort;

    @InjectMocks
    private ListPinnedAnnouncementsUseCase useCase;

    @Test
    void shouldApplyScopeFilter_WhenRestrictedProfile() {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        RequestContext context = new RequestContext(
                userId, UserType.STUDENT, List.of(new ContextClass(classId, null)));

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of(UUID.randomUUID()));
        when(hubShiftPort.getShiftCodesForClasses(List.of(classId))).thenReturn(List.of());
        when(announcementRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        List<Announcement> result = useCase.execute();

        assertThat(result).isEmpty();
        verify(permissionValidator).canViewAll(UserType.STUDENT);
        verify(hubCoursePort).getCurrentUserCourseIds();
        verify(announcementRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void shouldNotRestrictScope_WhenViewAllProfile() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        Announcement pinned = Announcement.builder()
                .id(UUID.randomUUID())
                .pinned(true)
                .pinnedOrder((short) 1)
                .build();

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.SENAI)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(pinned));

        List<Announcement> result = useCase.execute();

        assertThat(result).containsExactly(pinned);
        verify(hubCoursePort, never()).getCurrentUserCourseIds();
    }

    @Test
    void shouldOrderByPinnedOrderAscending() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.ADMIN, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.ADMIN)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        useCase.execute();

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(announcementRepository).findAll(any(Specification.class), sortCaptor.capture());

        Sort sort = sortCaptor.getValue();
        assertThat(sort.getOrderFor("pinnedOrder")).isNotNull();
        assertThat(sort.getOrderFor("pinnedOrder").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void shouldReturnEmptyList_WhenNoPinnedAnnouncements() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.WEG, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.WEG)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        List<Announcement> result = useCase.execute();

        assertThat(result).isEmpty();
    }
}
