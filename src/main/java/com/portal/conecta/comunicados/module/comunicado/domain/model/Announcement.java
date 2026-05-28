package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementOrigin;
import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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

    @Column(name = "origin", nullable = false)
    private AnnouncementOrigin origin;

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
    private LocalDateTime scheduledFor;

    @Column(name = "published_at", nullable = true)
    private LocalDateTime publishedAt;

    @Column(name = "removed_at", nullable = true)
    private LocalDateTime removedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    @OneToMany(mappedBy = "announcement")
    private List<AnnouncementIndividualNotice> individualNotices;

    @OneToMany(mappedBy = "announcement")
    private List<IndividualNoticeCategory> individualNoticeCategories;
}
