package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.portal.conecta.comunicados.module.comunicado.domain.AnnouncementRoleAudience;
import com.portal.conecta.comunicados.shared.context.UserType;

class AnnouncementRoleAudienceTest {

    @Test
    void representativeViewer_coversStudentRestriction() {
        assertThat(AnnouncementRoleAudience.matchesRestriction(UserType.REPRESENTATIVE, List.of("STUDENT")))
                .isTrue();
        assertThat(AnnouncementRoleAudience.matchesRestriction(UserType.TEACHER, List.of("STUDENT")))
                .isFalse();
        assertThat(AnnouncementRoleAudience.matchesRestriction(UserType.STUDENT, List.of("STUDENT")))
                .isTrue();
    }

    @Test
    void expandForNotification_addsRepresentativeWhenStudentPresent() {
        assertThat(AnnouncementRoleAudience.expandForNotification(List.of(UserType.STUDENT)))
                .containsExactly(UserType.STUDENT, UserType.REPRESENTATIVE);
        assertThat(AnnouncementRoleAudience.expandForNotification(List.of(UserType.TEACHER)))
                .containsExactly(UserType.TEACHER);
    }
}
