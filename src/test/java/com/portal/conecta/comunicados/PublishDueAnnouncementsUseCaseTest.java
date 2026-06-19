package com.portal.conecta.comunicados;

import com.portal.conecta.comunicados.module.comunicado.application.usecase.AnnouncementPublisher;
import com.portal.conecta.comunicados.module.comunicado.application.usecase.PublishDueAnnouncementsUseCase;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishDueAnnouncementsUseCaseTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private AnnouncementPublisher announcementPublisher;

    @InjectMocks
    private PublishDueAnnouncementsUseCase useCase;

    private Announcement due() {
        return Announcement.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void shouldCountOnlyEffectivePublications() {
        Announcement a = due();
        Announcement b = due();
        Announcement c = due();

        when(announcementRepository.findDueForPublication(any(Instant.class)))
                .thenReturn(List.of(a, b, c));
        when(announcementPublisher.publish(eq(a), any(Instant.class))).thenReturn(true);
        when(announcementPublisher.publish(eq(b), any(Instant.class))).thenReturn(false); // outra réplica venceu
        when(announcementPublisher.publish(eq(c), any(Instant.class))).thenReturn(true);

        int published = useCase.execute();

        assertThat(published).isEqualTo(2);
        verify(announcementPublisher, times(3)).publish(any(Announcement.class), any(Instant.class));
    }

    @Test
    void shouldContinue_WhenOneItemFails() {
        Announcement a = due();
        Announcement b = due();

        when(announcementRepository.findDueForPublication(any(Instant.class)))
                .thenReturn(List.of(a, b));
        when(announcementPublisher.publish(eq(a), any(Instant.class)))
                .thenThrow(new RuntimeException("erro pontual"));
        when(announcementPublisher.publish(eq(b), any(Instant.class))).thenReturn(true);

        int published = useCase.execute();

        assertThat(published).isEqualTo(1);
        verify(announcementPublisher).publish(eq(b), any(Instant.class));
    }

    @Test
    void shouldReturnZero_WhenNothingDue() {
        when(announcementRepository.findDueForPublication(any(Instant.class)))
                .thenReturn(List.of());

        assertThat(useCase.execute()).isZero();
    }
}
