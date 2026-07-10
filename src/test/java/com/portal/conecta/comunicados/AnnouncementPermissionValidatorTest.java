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
        announcement = activeAnnouncement(creatorId, UserType.TEACHER);
    }

    @Test
    void shouldAllowAdminToDeleteAnyAnnouncement() {
        announcement.setCreatedByUserType(UserType.WEG);
        assertThat(validator.canDelete(UserType.ADMIN, actorId, announcement)).isTrue();
    }

    @Test
    void shouldAllowSenaiToDeleteTeacherAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement)).isTrue();
    }

    @Test
    void shouldAllowSenaiToDeleteRepresentativeAnnouncement() {
        announcement.setCreatedByUserType(UserType.REPRESENTATIVE);
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement)).isTrue();
    }

    @Test
    void shouldDenySenaiDeletingWegAnnouncement() {
        announcement.setCreatedByUserType(UserType.WEG);
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenySenaiDeletingAnotherSenaiAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement)).isFalse();
    }

    @Test
    void shouldAllowWegToDeleteTeacherAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement)).isTrue();
    }

    @Test
    void shouldDenyWegDeletingSenaiAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenyWegDeletingAnotherWegAnnouncement() {
        announcement.setCreatedByUserType(UserType.WEG);
        assertThat(validator.canDelete(UserType.WEG, actorId, announcement)).isFalse();
    }

    @Test
    void shouldAllowTeacherToDeleteOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId, UserType.TEACHER);
        assertThat(validator.canDelete(UserType.TEACHER, actorId, own)).isTrue();
    }

    @Test
    void shouldDenyTeacherDeletingOthersAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canDelete(UserType.TEACHER, actorId, announcement)).isFalse();
    }

    @Test
    void shouldAllowRepresentativeToDeleteOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId, UserType.REPRESENTATIVE);
        assertThat(validator.canDelete(UserType.REPRESENTATIVE, actorId, own)).isTrue();
    }

    @Test
    void shouldDenyRepresentativeDeletingOthersAnnouncement() {
        announcement.setCreatedByUserType(UserType.REPRESENTATIVE);
        assertThat(validator.canDelete(UserType.REPRESENTATIVE, actorId, announcement)).isFalse();
    }

    @Test
    void shouldAllowSenaiToDeleteOwnAnnouncementEvenWithoutCreatorType() {
        Announcement own = activeAnnouncement(actorId, null);
        assertThat(validator.canDelete(UserType.SENAI, actorId, own)).isTrue();
    }

    @Test
    void shouldDenySenaiDeletingOthersWhenCreatorTypeIsUnknown() {
        announcement.setCreatedByUserType(null);
        assertThat(validator.canDelete(UserType.SENAI, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenyStudentDeletingAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canDelete(UserType.STUDENT, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenyDeletingAlreadyRemovedAnnouncement() {
        announcement.setStatus(AnnouncementStatus.REMOVED);
        assertThat(validator.canDelete(UserType.ADMIN, actorId, announcement)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = UserType.class, names = {"ADMIN", "SENAI", "WEG", "TEACHER", "REPRESENTATIVE"})
    void shouldDenyDeleteWhenActorIdIsNull(UserType userType) {
        assertThat(validator.canDelete(userType, null, announcement)).isFalse();
    }

    // --- canUpdate (#125): mesma matriz da remoção, baseada no perfil do autor ---

    @Test
    void shouldAllowSenaiToUpdateOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId, UserType.SENAI);
        assertThat(validator.canUpdate(UserType.SENAI, actorId, own)).isTrue();
    }

    @Test
    void shouldDenySenaiUpdatingAnotherSenaiAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        assertThat(validator.canUpdate(UserType.SENAI, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenyWegUpdatingSenaiAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        assertThat(validator.canUpdate(UserType.WEG, actorId, announcement)).isFalse();
    }

    @Test
    void shouldAllowSenaiToUpdateTeacherAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canUpdate(UserType.SENAI, actorId, announcement)).isTrue();
    }

    @Test
    void shouldAllowAdminToUpdateAnyAnnouncement() {
        announcement.setCreatedByUserType(UserType.SENAI);
        assertThat(validator.canUpdate(UserType.ADMIN, actorId, announcement)).isTrue();
    }

    @Test
    void shouldAllowTeacherToUpdateOwnAnnouncement() {
        Announcement own = activeAnnouncement(actorId, UserType.TEACHER);
        assertThat(validator.canUpdate(UserType.TEACHER, actorId, own)).isTrue();
    }

    @Test
    void shouldDenyTeacherUpdatingOthersAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        assertThat(validator.canUpdate(UserType.TEACHER, actorId, announcement)).isFalse();
    }

    @Test
    void shouldDenyUpdatingRemovedAnnouncement() {
        announcement.setCreatedByUserType(UserType.TEACHER);
        announcement.setRemovedAt(Instant.now());
        assertThat(validator.canUpdate(UserType.ADMIN, actorId, announcement)).isFalse();
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

    private Announcement activeAnnouncement(UUID createdByUserId, UserType createdByUserType) {
        return Announcement.builder()
                .id(UUID.randomUUID())
                .title("Comunicado")
                .description("Descrição")
                .origin(AnnouncementOrigin.SENAI)
                .status(AnnouncementStatus.PUBLISHED)
                .createdByUserId(createdByUserId)
                .createdByUserType(createdByUserType)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
