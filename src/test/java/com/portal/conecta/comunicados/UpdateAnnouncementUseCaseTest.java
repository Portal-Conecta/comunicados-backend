package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal.conecta.comunicados.module.comunicado.application.command.UpdateAnnouncementCommand;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.UpdateAnnouncementUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementConflictException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementNotFoundException;
import com.portal.conecta.comunicados.module.comunicado.domain.exception.AnnouncementPermissionDeniedException;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementDestination;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementHistory;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementDestinationRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementHistoryRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.tag.application.usecase.AutoLinkTagsByDestinationUseCase;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationRequest;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.UpdateAnnouncementRequest;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateAnnouncementUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementDestinationRepository destinationRepository;

    @Mock
    private AnnouncementHistoryRepository historyRepository;

    @Mock
    private AnnouncementTagRepository announcementTagRepository;

    @Mock
    private RequestContextProvider contextProvider;

    @Mock
    private AutoLinkTagsByDestinationUseCase autoLinkTagsUseCase;

    private UpdateAnnouncementUseCase useCase;
    private ObjectMapper objectMapper;
    private UUID announcementId;
    private UUID creatorId;
    private UUID actorId;
    private UUID classId;
    private Announcement announcement;
    private UpdateAnnouncementRequest request;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        useCase = new UpdateAnnouncementUseCase(
                announcementRepository,
                destinationRepository,
                historyRepository,
                announcementTagRepository,
                contextProvider,
                new AnnouncementPermissionValidator(org.mockito.Mockito.mock(HubClassPort.class)),
                new com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer(),
                autoLinkTagsUseCase
        );

        announcementId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        classId = UUID.randomUUID();

        announcement = Announcement.builder()
                .id(announcementId)
                .title("Titulo antigo")
                .description("Descricao antiga")
                .descriptionPlain("Descricao antiga")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.SCHEDULED)
                .pinned(false)
                .createdByUserId(creatorId)
                .createdByUserType(UserType.TEACHER)
                .createdAt(Instant.now().minusSeconds(120))
                .updatedAt(Instant.now().minusSeconds(60))
                .build();

        request = new UpdateAnnouncementRequest(
                "Titulo novo",
                "Descricao nova",
                AnnouncementOrigin.WEG,
                AnnouncementStatus.SCHEDULED,
                true,
                (short) 1,
                Instant.now().plusSeconds(3600),
                List.of(new CreateAnnouncementDestinationRequest(
                        null,
                        AnnouncementDestinationType.CLASS,
                        classId
                ))
        );
    }

    @Test
    void shouldUpdateOwnAnnouncementWhenTeacherIsAuthor() {
        announcement.setCreatedByUserId(actorId);
        mockContext(UserType.TEACHER, actorId);
        mockFoundAnnouncement();

        Announcement updated = useCase.execute(command());

        assertThat(updated.getTitle()).isEqualTo("Titulo novo");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
        assertThat(updated.getOrigin()).isEqualTo(AnnouncementOrigin.WEG);
        assertThat(updated.getStatus()).isEqualTo(AnnouncementStatus.SCHEDULED);
        assertThat(updated.isPinned()).isTrue();
        assertThat(updated.getPinnedOrder()).isEqualTo((short) 1);
        assertThat(updated.getUpdatedAt()).isNotNull();

        verify(destinationRepository).deleteByAnnouncementId(announcementId);

        ArgumentCaptor<List<AnnouncementDestination>> destinationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(destinationRepository).saveAll(destinationsCaptor.capture());

        assertThat(destinationsCaptor.getValue()).hasSize(1);
        assertThat(destinationsCaptor.getValue().getFirst().getAnnouncement()).isEqualTo(updated);
        assertThat(destinationsCaptor.getValue().getFirst().getType()).isEqualTo(AnnouncementDestinationType.CLASS);
        assertThat(destinationsCaptor.getValue().getFirst().getReferenceId()).isEqualTo(classId);

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        assertThat(historyCaptor.getValue().getAction()).isEqualTo(AnnouncementHistoryAction.EDIT);
        assertThat(historyCaptor.getValue().getUserId()).isEqualTo(actorId);
        assertThat(historyCaptor.getValue().getSnapshot()).isNull();
    }

    @Test
    void shouldUpdateOthersAnnouncementWhenActorIsWeg() {
        mockContext(UserType.WEG, actorId);
        mockFoundAnnouncement();

        useCase.execute(command());

        verify(announcementRepository).save(any(Announcement.class));
        verify(historyRepository).save(any(AnnouncementHistory.class));
    }

    @Test
    void shouldThrowForbiddenWhenWegEditsAnotherSenaiAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        mockContext(UserType.WEG, actorId);
        mockFoundAnnouncementWithoutPersistence();

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verifyNoMutation();
    }

    @Test
    void shouldThrowForbiddenWhenStudentTriesToUpdate() {
        mockContext(UserType.STUDENT, actorId);
        mockFoundAnnouncementWithoutPersistence();

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verifyNoMutation();
    }

    @Test
    void shouldThrowForbiddenWhenTeacherIsNotAuthor() {
        mockContext(UserType.TEACHER, actorId);
        mockFoundAnnouncementWithoutPersistence();

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verifyNoMutation();
    }

    @Test
    void shouldThrowForbiddenWhenRepresentativeIsNotAuthor() {
        mockContext(UserType.REPRESENTATIVE, actorId);
        mockFoundAnnouncementWithoutPersistence();

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementPermissionDeniedException.class);

        verifyNoMutation();
    }

    @Test
    void shouldThrowConflictWhenAnnouncementIsRemoved() {
        announcement.setRemovedAt(Instant.now());
        announcement.setStatus(AnnouncementStatus.REMOVED);

        mockContext(UserType.ADMIN, actorId);
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementConflictException.class);

        verifyNoMutation();
    }

    @Test
    void shouldThrowNotFoundWhenAnnouncementDoesNotExist() {
        mockContext(UserType.ADMIN, actorId);
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(AnnouncementNotFoundException.class);

        verifyNoMutation();
    }

    @Test
    void shouldSavePreviousStateSnapshotWhenPublishedAnnouncementIsEdited() throws Exception {
        announcement.setStatus(AnnouncementStatus.PUBLISHED);

        mockContext(UserType.ADMIN, actorId);
        mockFoundAnnouncement();

        useCase.execute(command());

        ArgumentCaptor<AnnouncementHistory> historyCaptor = ArgumentCaptor.forClass(AnnouncementHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        String snapshot = historyCaptor.getValue().getSnapshot();
        assertThat(snapshot).isNotBlank();

        JsonNode json = objectMapper.readTree(snapshot);

        assertThat(json.get("id").asText()).isEqualTo(announcementId.toString());
        assertThat(json.get("title").asText()).isEqualTo("Titulo antigo");
        assertThat(json.get("status").asText()).isEqualTo(AnnouncementStatus.PUBLISHED.name());
        assertThat(json.get("destinations").size()).isEqualTo(1);
    }

    @Test
    void shouldSetPublishedAtWhenScheduledAnnouncementIsPublishedNow() {
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant scheduledFor = Instant.now().plusSeconds(7200);
        announcement.setCreatedAt(createdAt);
        announcement.setScheduledFor(scheduledFor);
        announcement.setStatus(AnnouncementStatus.SCHEDULED);
        announcement.setCreatedByUserId(actorId);

        request = new UpdateAnnouncementRequest(
                "Titulo novo",
                "Descricao nova",
                null,
                AnnouncementStatus.PUBLISHED,
                null,
                null,
                null,
                null
        );

        mockContext(UserType.TEACHER, actorId);
        mockFoundAnnouncement();

        Announcement updated = useCase.execute(command());

        assertThat(updated.getStatus()).isEqualTo(AnnouncementStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
        assertThat(updated.getPublishedAt()).isAfter(createdAt);
        assertThat(updated.getPublishedByUserId()).isEqualTo(actorId);
        assertThat(updated.getScheduledFor()).isNull();
    }

    private UpdateAnnouncementCommand command() {
        return UpdateAnnouncementCommand.fromRequest(announcementId, request, actorId);
    }

    private void mockFoundAnnouncement() {
        mockFoundAnnouncementWithoutPersistence();

        when(destinationRepository.findByAnnouncementId(announcementId))
                .thenReturn(List.of(AnnouncementDestination.builder()
                        .id(UUID.randomUUID())
                        .announcement(announcement)
                        .type(AnnouncementDestinationType.GENERAL)
                        .build()));

        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lenient().when(destinationRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(historyRepository.save(any(AnnouncementHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void mockFoundAnnouncementWithoutPersistence() {
        when(announcementRepository.findById(announcementId)).thenReturn(Optional.of(announcement));
    }

    private void mockContext(UserType userType, UUID userId) {
        when(contextProvider.getRequestContext())
                .thenReturn(new RequestContext(userId, userType, List.of()));
    }

    private void verifyNoMutation() {
        verify(announcementRepository, never()).save(any());
        verify(destinationRepository, never()).deleteByAnnouncementId(any());
        verify(destinationRepository, never()).saveAll(anyList());
        verify(historyRepository, never()).save(any());
    }
}
