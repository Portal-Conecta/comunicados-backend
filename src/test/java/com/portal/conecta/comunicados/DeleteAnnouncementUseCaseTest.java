package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.DeleteAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubUserPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAnnouncementUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private HubUserPort hubUserPort;

    @Spy
    private AnnouncementPermissionValidator permissionValidator = new AnnouncementPermissionValidator();

    @InjectMocks
    private DeleteAnnouncementUseCase useCase;

    private UUID announcementId;
    private UUID creatorId;
    private UUID actorId;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        announcementId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        announcement = Announcement.builder()
                .id(announcementId)
                .title("Comunicado")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .pinned(false)
                .createdByUserId(creatorId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldSoftDeleteWhenAdminDeletesAnyAnnouncement() {
        mockContext(UserType.ADMIN, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.WEG));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(announcementId);

        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());
        Announcement saved = announcementCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AnnouncementStatus.REMOVED);
        assertThat(saved.getRemovedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());
        AnnouncementHistory history = historyCaptor.getValue();
        assertThat(history.getAction()).isEqualTo(AnnouncementHistoryAction.REMOVAL);
        assertThat(history.getUserId()).isEqualTo(actorId);
        assertThat(history.getAnnouncement()).isEqualTo(announcement);
    }

    @Test
    void shouldSoftDeleteWhenSenaiDeletesTeacherAnnouncement() {
        mockContext(UserType.SENAI, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.TEACHER));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(announcementId);

        verify(announcementRepository).save(any(Announcement.class));
        verify(historyRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldThrowPermissionDeniedWhenSenaiDeletesWegAnnouncement() {
        mockContext(UserType.SENAI, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.WEG));

        assertThatThrownBy(() -> useCase.execute(announcementId))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldThrowPermissionDeniedWhenWegDeletesSenaiAnnouncement() {
        mockContext(UserType.WEG, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.SENAI));

        assertThatThrownBy(() -> useCase.execute(announcementId))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);
    }

    @Test
    void shouldSoftDeleteWhenTeacherDeletesOwnAnnouncement() {
        announcement.setCreatedByUserId(actorId);
        mockContext(UserType.TEACHER, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(actorId)).thenReturn(Optional.of(UserType.TEACHER));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(announcementId);

        verify(announcementRepository).save(any(Announcement.class));
    }

    @Test
    void shouldThrowPermissionDeniedWhenTeacherDeletesOthersAnnouncement() {
        mockContext(UserType.TEACHER, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.TEACHER));

        assertThatThrownBy(() -> useCase.execute(announcementId))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);
    }

    @Test
    void shouldThrowNotFoundWhenAnnouncementDoesNotExist() {
        mockContext(UserType.ADMIN, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(announcementId))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void shouldResolveCreatorTypeFromHubBeforeValidatingPermission() {
        mockContext(UserType.SENAI, actorId);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId)).thenReturn(Optional.of(announcement));
        when(hubUserPort.findUserTypeById(creatorId)).thenReturn(Optional.of(UserType.REPRESENTATIVE));
        when(announcementRepository.save(any(Announcement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(announcementId);

        verify(hubUserPort).findUserTypeById(creatorId);
    }

    private void mockContext(UserType userType, UUID userId) {
        when(contextProvider.getRequestContext()).thenReturn(new RequestContext(userId, userType, List.of()));
    }
}
