package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.module.comunicado.presentation.dto.request.CreateAnnouncementDestinationInput;
import com.portal.conecta.comunicados.shared.context.ClassRole;
import com.portal.conecta.comunicados.shared.context.ContextClass;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.UserType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementPermissionValidatorTest {

    private AnnouncementPermissionValidator validator;
    private HubClassPort hubClassPort;

    private UUID actorId;
    private UUID creatorId;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        hubClassPort = mock(HubClassPort.class);
        validator = new AnnouncementPermissionValidator(hubClassPort);
        actorId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        announcement = activeAnnouncement(creatorId);
    }

    @Test
    void shouldAllowAdminToDeleteAnyAnnouncement() {
        assertThat(validator.canDelete(UserType.ADMIN, actorId, announcement, UserType.WEG)).isTrue();
    }

    @Test
    void shouldAllowSenaiToDeleteTeacherAnnouncement() {
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement, UserType.TEACHER)).isTrue();
    }

    @Test
    void shouldAllowSenaiToDeleteRepresentativeAnnouncement() {
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement, UserType.REPRESENTATIVE)).isTrue();
    }

    @Test
    void shouldDenySenaiDeletingWegAnnouncement() {
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement, UserType.WEG)).isFalse();
    }

    @Test
    void shouldDenySenaiDeletingAnotherSenaiAnnouncement() {
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement, UserType.SENAI)).isFalse();
    }

    @Test
    void shouldAllowWegToDeleteTeacherAnnouncement() {
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement, UserType.TEACHER)).isTrue();
    }

    @Test
    void shouldDenyWegDeletingSenaiAnnouncement() {
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement, UserType.SENAI)).isFalse();
    }

    @Test
    void shouldDenyWegDeletingAnotherWegAnnouncement() {
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement, UserType.WEG)).isFalse();
    }

    @Test
    void shouldAllowTeacherToDeleteOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId);
        assertThat(validator.canDelete(UserType.TEACHER, actorId, own, UserType.TEACHER)).isTrue();
    }

    @Test
    void shouldDenyTeacherDeletingOthersAnnouncement() {
        assertThat(validator.canDelete(UserType.TEACHER, actorId, announcement, UserType.TEACHER)).isFalse();
    }

    @Test
    void shouldAllowRepresentativeToDeleteOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId);
        assertThat(validator.canDelete(UserType.REPRESENTATIVE, actorId, own, UserType.REPRESENTATIVE)).isTrue();
    }

    @Test
    void shouldDenyRepresentativeDeletingOthersAnnouncement() {
        assertThat(validator.canDelete(UserType.REPRESENTATIVE, actorId, announcement, UserType.REPRESENTATIVE)).isFalse();
    }

    @Test
    void shouldAllowSenaiToDeleteOwnAnnouncementEvenWithoutCreatorTypeFromHub() {
        Announcement own = activeAnnouncement(actorId);
        assertThat(validator.canDelete(UserType.SENAI, actorId, own, null)).isTrue();
    }

    @Test
    void shouldDenySenaiDeletingOthersWhenCreatorTypeIsUnknown() {
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement, null)).isFalse();
    }

    @Test
    void shouldDenyStudentDeletingAnnouncement() {
        assertThat(validator.canDelete(UserType.STUDENT, actorId, announcement, UserType.TEACHER)).isFalse();
    }

    @Test
    void shouldDenyDeletingAlreadyRemovedAnnouncement() {
        announcement.setStatus(AnnouncementStatus.REMOVED);
        assertThat(validator.canDelete(UserType.ADMIN, actorId, announcement, UserType.TEACHER)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UserType.class, names = {"ADMIN", "SENAI", "WEG", "TEACHER", "REPRESENTATIVE"})
    void shouldDenyDeleteWhenActorIdIsNull(UserType userType) {
        assertThat(validator.canDelete(userType, null, announcement, UserType.TEACHER)).isFalse();
    }

    // --- canCreateForDestinations ---

    @ParameterizedTest
    @EnumSource(value = UserType.class, names = {"ADMIN", "SENAI", "WEG"})
    void shouldAllowManageAnyScopeForAnyDestination(UserType userType) {
        RequestContext context = context(userType);

        assertThat(validator.canCreateForDestinations(context, List.of(generalDestination()))).isTrue();
        assertThat(validator.canCreateForDestinations(context, List.of(courseDestination(UUID.randomUUID())))).isTrue();
        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(UUID.randomUUID())))).isTrue();
        assertThat(validator.canCreateForDestinations(context, List.of(userDestination(UUID.randomUUID())))).isTrue();
    }

    @Test
    void shouldAllowTeacherWhenClassDestinationIsLinked() {
        UUID classId = UUID.randomUUID();
        RequestContext context = context(UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(classId)))).isTrue();
    }

    @Test
    void shouldDenyTeacherWhenClassDestinationIsNotLinked() {
        RequestContext context = context(UserType.TEACHER, new ContextClass(UUID.randomUUID(), ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldAllowTeacherWhenUserDestinationBelongsToLinkedClass() {
        UUID classId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(hubClassPort.getClassIdForUser(studentId)).thenReturn(classId);

        RequestContext context = context(UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(userDestination(studentId)))).isTrue();
    }

    @Test
    void shouldDenyTeacherWhenUserDestinationBelongsToOtherClass() {
        UUID studentId = UUID.randomUUID();
        when(hubClassPort.getClassIdForUser(studentId)).thenReturn(UUID.randomUUID());

        RequestContext context = context(UserType.TEACHER, new ContextClass(UUID.randomUUID(), ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(userDestination(studentId)))).isFalse();
    }

    @Test
    void shouldDenyTeacherWhenHubHasNoClassForUser() {
        UUID studentId = UUID.randomUUID();
        when(hubClassPort.getClassIdForUser(studentId)).thenReturn(null);

        RequestContext context = context(UserType.TEACHER, new ContextClass(UUID.randomUUID(), ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(userDestination(studentId)))).isFalse();
    }

    @Test
    void shouldDenyTeacherForGeneralOrCourseDestination() {
        UUID classId = UUID.randomUUID();
        RequestContext context = context(UserType.TEACHER, new ContextClass(classId, ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(generalDestination()))).isFalse();
        assertThat(validator.canCreateForDestinations(context, List.of(courseDestination(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldDenyTeacherWhenAnyDestinationIsOutOfScope() {
        UUID linkedClassId = UUID.randomUUID();
        RequestContext context = context(UserType.TEACHER, new ContextClass(linkedClassId, ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(
                context, List.of(classDestination(linkedClassId), classDestination(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldDenyTeacherWithoutClassesInContext() {
        assertThat(validator.canCreateForDestinations(
                context(UserType.TEACHER), List.of(classDestination(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldDenyWhenDestinationsAreEmpty() {
        RequestContext context = context(UserType.TEACHER, new ContextClass(UUID.randomUUID(), ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of())).isFalse();
    }

    @Test
    void shouldAllowRepresentativeWhenClassDestinationIsLinked() {
        UUID classId = UUID.randomUUID();
        RequestContext context = context(UserType.REPRESENTATIVE, new ContextClass(classId, ClassRole.REPRESENTATIVE));

        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(classId)))).isTrue();
    }

    @Test
    void shouldDenyRepresentativeWhenClassRoleDoesNotMatch() {
        UUID classId = UUID.randomUUID();
        RequestContext context = context(UserType.REPRESENTATIVE, new ContextClass(classId, ClassRole.TEACHER));

        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(classId)))).isFalse();
    }

    @Test
    void shouldDenyStudent() {
        RequestContext context = context(UserType.STUDENT, new ContextClass(UUID.randomUUID(), ClassRole.STUDENT));

        assertThat(validator.canCreateForDestinations(context, List.of(classDestination(UUID.randomUUID())))).isFalse();
    }

    @Test
    void shouldDenyWhenContextOrUserTypeIsNull() {
        assertThat(validator.canCreateForDestinations(null, List.of(generalDestination()))).isFalse();
        assertThat(validator.canCreateForDestinations(
                new RequestContext(actorId, null, List.of()), List.of(generalDestination()))).isFalse();
    }

    private RequestContext context(UserType userType, ContextClass... classes) {
        return new RequestContext(actorId, userType, List.of(classes));
    }

    private CreateAnnouncementDestinationInput generalDestination() {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.GENERAL, null);
    }

    private CreateAnnouncementDestinationInput courseDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.COURSE, referenceId);
    }

    private CreateAnnouncementDestinationInput classDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.CLASS, referenceId);
    }

    private CreateAnnouncementDestinationInput userDestination(UUID referenceId) {
        return new CreateAnnouncementDestinationInput(AnnouncementDestinationType.USER, referenceId);
    }

    private Announcement activeAnnouncement(UUID createdByUserId) {
        return Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .createdByUserId(createdByUserId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
