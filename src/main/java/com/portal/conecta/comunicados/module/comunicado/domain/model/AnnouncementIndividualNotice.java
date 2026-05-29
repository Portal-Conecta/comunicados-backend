package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    private LocalDateTime resolvedAt;
    
}
