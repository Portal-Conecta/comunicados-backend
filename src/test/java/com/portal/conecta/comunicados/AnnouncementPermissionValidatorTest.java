package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.comunicado.domain.validator.AnnouncementPermissionValidator;
import com.portal.conecta.comunicados.shared.context.UserType;

import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementPermissionValidatorTest {

    private AnnouncementPermissionValidator validator;

    private UUID actorId;
    private UUID creatorId;
    private Announcement announcement;

    @BeforeEach
    void setUp() {
        validator = new AnnouncementPermissionValidator(mock(HubClassPort.class));
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
