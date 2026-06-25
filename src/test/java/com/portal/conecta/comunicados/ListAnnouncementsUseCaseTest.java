package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.query.ListAnnouncementsQuery;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.ListAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.PostFilterRequest;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAnnouncementsUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AnnouncementPermissionValidator permissionValidator;

    @Mock
    private HubCoursePort hubCoursePort;

    @Mock
    private HubUserPort hubUserPort;

    @InjectMocks
    private ListAnnouncementsUseCase useCase;

    private PostFilterRequest filter() {
        return PostFilterRequest.defaults();
    }

    @Test
    void shouldApplyScopeFilter_WhenRestrictedProfile() {
        UUID userId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        RequestContext context = new RequestContext(
                userId, UserType.STUDENT, List.of(new ContextClass(classId, null)));

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.STUDENT)).thenReturn(false);
        when(hubCoursePort.getCurrentUserCourseIds()).thenReturn(List.of(UUID.randomUUID()));
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Announcement> result = useCase.execute(new ListAnnouncementsQuery(filter(), userId));

        assertThat(result).isNotNull();
        verify(permissionValidator).canViewAll(UserType.STUDENT);
        verify(hubCoursePort).getCurrentUserCourseIds();
        verify(announcementRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldNotRestrictScope_WhenViewAllProfile() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.SENAI)).thenReturn(true);

        Announcement announcement = Announcement.builder().id(UUID.randomUUID()).build();
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(announcement)));

        Page<Announcement> result = useCase.execute(new ListAnnouncementsQuery(filter(), userId));

        assertThat(result.getContent()).hasSize(1);
        verify(permissionValidator).canViewAll(UserType.SENAI);
        verify(hubCoursePort, never()).getCurrentUserCourseIds();
        verify(announcementRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldReturnEmptyPage_WhenNoResults() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.WEG, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.WEG)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PostFilterRequest filterWithOrigin = PostFilterRequest.withOrigin(AnnouncementOrigin.WEG);

        Page<Announcement> result = useCase.execute(new ListAnnouncementsQuery(filterWithOrigin, userId));

        assertThat(result.getContent()).isEmpty();
        verify(announcementRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldPassPageRequestWithConfiguredSize() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.ADMIN, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.ADMIN)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PostFilterRequest pagedFilter = PostFilterRequest.withPaging(2, 5);

        useCase.execute(new ListAnnouncementsQuery(pagedFilter, userId));

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(announcementRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        verify(permissionValidator, never()).canViewAll(UserType.STUDENT);
    }

    @Test
    void shouldResolveHubUsers_WhenSearchIsProvided() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.SENAI)).thenReturn(true);
        when(hubUserPort.findUserIdsByNameContaining("joão", context)).thenReturn(List.of(UUID.randomUUID()));
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PostFilterRequest searchFilter = PostFilterRequest.withSearch("joão");

        useCase.execute(new ListAnnouncementsQuery(searchFilter, userId));

        verify(hubUserPort).findUserIdsByNameContaining("joão", context);
        verify(announcementRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldNotResolveHubUsers_WhenSearchIsBlank() {
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.ADMIN, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.ADMIN)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PostFilterRequest blankSearchFilter = PostFilterRequest.withSearch("   ");

        useCase.execute(new ListAnnouncementsQuery(blankSearchFilter, userId));

        verifyNoInteractions(hubUserPort);
    }

    @Test
    void shouldApplyTagFilter_WhenTagIdIsProvided() {
        UUID userId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.ADMIN, List.of());

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canViewAll(UserType.ADMIN)).thenReturn(true);
        when(announcementRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PostFilterRequest tagFilter = PostFilterRequest.withTagId(tagId);

        useCase.execute(new ListAnnouncementsQuery(tagFilter, userId));

        verify(announcementRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldMergeTagIdAndTagIds_WhenBothAreProvided() {
        UUID tagId = UUID.randomUUID();
        UUID otherTagId = UUID.randomUUID();

        PostFilterRequest filter = new PostFilterRequest(
                null, null, null, null, null, null, tagId, List.of(otherTagId, tagId), null, null);

        assertThat(filter.resolvedTagIds()).containsExactly(tagId, otherTagId);
    }
}
