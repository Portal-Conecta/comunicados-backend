package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementHistoryAction;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "action", nullable = false)
    private AnnouncementHistoryAction action;

    @Column(name = "snapshot", nullable = true)
    private String snapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
