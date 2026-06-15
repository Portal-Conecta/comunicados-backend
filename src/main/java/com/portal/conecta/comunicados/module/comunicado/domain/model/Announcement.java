package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private AnnouncementOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AnnouncementStatus status;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "pinned_order", nullable = true)
    private Short pinnedOrder;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "published_by_user_id", nullable = true)
    private UUID publishedByUserId;

    @Column(name = "scheduled_for", nullable = true)
    private Instant scheduledFor;

    @Column(name = "published_at", nullable = true)
    private Instant publishedAt;

    @Column(name = "removed_at", nullable = true)
    private Instant removedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementDestination> destinations;

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementFile> files;

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementHistory> histories;

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementTag> tags;

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementMention> mentions;
}
