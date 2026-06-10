package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_individual_notice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementIndividualNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private IndividualNoticeCategory category;

    @Column(name = "resolved_at", nullable = true)
    private Instant resolvedAt;
    
}
