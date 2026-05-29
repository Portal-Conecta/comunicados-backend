package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_mention")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementMention {

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
