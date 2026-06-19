package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.command.PinAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PinAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class PinAnnouncementUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AnnouncementPermissionValidator permissionValidator;

    @InjectMocks
    private PinAnnouncementUseCase useCase;

    @Test
    void shouldPinAnnouncementSuccessfully() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Integer pinnedOrder = 1;
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .title("Test Announcement")
                .pinned(false)
                .build();

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, pinnedOrder);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.SENAI)).thenReturn(true);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.isPinned()).isTrue();
        assertThat(result.getPinnedOrder()).isEqualTo(pinnedOrder);

        verify(announcementRepository).save(announcement);
        verify(historyRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldPinAnnouncementWithoutOrder() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .title("Test Announcement")
                .pinned(false)
                .build();

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, null);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.SENAI)).thenReturn(true);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Announcement result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.isPinned()).isTrue();
        assertThat(result.getPinnedOrder()).isNull();

        verify(announcementRepository).save(announcement);
    }

    @Test
    void shouldRecordHistoryWhenPinning() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Integer pinnedOrder = 1;
        RequestContext context = new RequestContext(userId, UserType.TEACHER, List.of());

        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .title("Test Announcement")
                .pinned(false)
                .build();

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, pinnedOrder);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.TEACHER)).thenReturn(true);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any(AnnouncementHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        ArgumentCaptor<AnnouncementHistory> historyCaptor =
                ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        AnnouncementHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getAction()).isEqualTo(AnnouncementHistoryAction.PINNED);
        assertThat(savedHistory.getUserId()).isEqualTo(userId);
        assertThat(savedHistory.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrow404_WhenAnnouncementNotFound() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Integer pinnedOrder = 1;
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, pinnedOrder);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.SENAI)).thenReturn(true);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldThrowPermissionDenied_WhenUserCannotPin() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Integer pinnedOrder = 1;
        RequestContext context = new RequestContext(userId, UserType.STUDENT, List.of());

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, pinnedOrder);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.STUDENT)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verify(announcementRepository, never()).findByIdAndRemovedAtIsNull(any());
        verify(announcementRepository, never()).save(any());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldThrow404NotRuntimeException_WhenAnnouncementNotFound() {
        UUID announcementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Integer pinnedOrder = 1;
        RequestContext context = new RequestContext(userId, UserType.SENAI, List.of());

        PinAnnouncementCommand command = new PinAnnouncementCommand(announcementId, userId, pinnedOrder);

        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canPin(UserType.SENAI)).thenReturn(true);
        when(announcementRepository.findByIdAndRemovedAtIsNull(announcementId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementNotFoundException.class)
                .isNotInstanceOf(IllegalArgumentException.class);
    }
}