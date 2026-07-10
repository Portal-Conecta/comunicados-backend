package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import jakarta.persistence.*;

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


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
