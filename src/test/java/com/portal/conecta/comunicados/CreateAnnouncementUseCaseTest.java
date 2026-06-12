package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.command.CreateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.CreateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementRequest;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAnnouncementUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementDestinationRepository destinationRepository;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AnnouncementPermissionValidator permissionValidator;

    @InjectMocks
    private CreateAnnouncementUseCase useCase;

    private UUID userId;
    private UUID classId;
    private RequestContext context;
    private CreateAnnouncementRequest request;
    private CreateAnnouncementCommand command;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        classId = UUID.randomUUID();
        context = new RequestContext(userId, UserType.TEACHER, List.of(new ContextClass(classId, null)));

        request = new CreateAnnouncementRequest(
                "Título do Comunicado",
                "Descrição do comunicado",
                AnnouncementOrigin.SENAI,
                AnnouncementStatus.DRAFT,
                false,
                null,
                null,
                List.of(
                        new CreateAnnouncementDestinationRequest(null, AnnouncementDestinationType.CLASS, classId),
                        new CreateAnnouncementDestinationRequest(null, AnnouncementDestinationType.GENERAL, null)
                )
        );

        command = CreateAnnouncementCommand.fromRequest(request, userId);
    }

    @Test
    void shouldCreateAnnouncementSuccessfully_WhenDocente() {
        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canCreate(UserType.TEACHER)).thenReturn(true);

        UUID announcementId = UUID.randomUUID();
        Announcement announcement = Announcement.builder()
                .id(announcementId)
                .title("Título do Comunicado")
                .description("Descrição do comunicado")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.DRAFT)
                .createdByUserId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);
        when(destinationRepository.saveAll(anyList())).thenReturn(List.of());
        when(historyRepository.save(any(AnnouncementHistory.class))).thenReturn(new AnnouncementHistory());

        Announcement result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(announcementId);
        assertThat(result.getTitle()).isEqualTo("Título do Comunicado");
        assertThat(result.getCreatedByUserId()).isEqualTo(userId);

        verify(announcementRepository, times(1)).save(any(Announcement.class));
        verify(destinationRepository, times(1)).saveAll(anyList());
        verify(historyRepository, times(1)).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldThrowException_WhenAprendiz() {
        RequestContext aprendizContext = new RequestContext(userId, UserType.STUDENT, List.of());
        when(contextProvider.getRequestContext()).thenReturn(aprendizContext);
        when(permissionValidator.canCreate(UserType.STUDENT)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AnnouncementPermissionDeniedException.class)
                .hasMessageContaining("Usuário não tem permissão para criar um comunicado.");

        verify(announcementRepository, never()).save(any());
        verify(destinationRepository, never()).saveAll(anyList());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void shouldCreateHistoryWithCREATIONAction() {
        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canCreate(UserType.TEACHER)).thenReturn(true);

        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Test")
                .description("Test")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.DRAFT)
                .createdByUserId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(announcementRepository.save(any())).thenReturn(announcement);
        when(destinationRepository.saveAll(anyList())).thenReturn(List.of());
        when(historyRepository.save(any())).thenReturn(new AnnouncementHistory());

        useCase.execute(command);

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        AnnouncementHistory history = historyCaptor.getValue();
        assertThat(history.getAction()).isEqualTo(AnnouncementHistoryAction.CREATION);
        assertThat(history.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldPersistAllDestinations() {
        when(contextProvider.getRequestContext()).thenReturn(context);
        when(permissionValidator.canCreate(UserType.TEACHER)).thenReturn(true);

        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID())
                .title("Test")
                .description("Test")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.DRAFT)
                .createdByUserId(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(announcementRepository.save(any())).thenReturn(announcement);
        when(destinationRepository.saveAll(anyList())).thenReturn(List.of());
        when(historyRepository.save(any())).thenReturn(new AnnouncementHistory());

        useCase.execute(command);

        ArgumentCaptor<List<AnnouncementDestination>> destinationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(destinationRepository).saveAll(destinationsCaptor.capture());

        List<AnnouncementDestination> destinations = destinationsCaptor.getValue();
        assertThat(destinations).hasSize(2);
        assertThat(destinations.stream().map(AnnouncementDestination::getType))
                .contains(AnnouncementDestinationType.CLASS, AnnouncementDestinationType.GENERAL);
    }
}