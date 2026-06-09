package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcement_destination")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;
    
    @Column(name = "type", nullable = false)
    private AnnouncementDestinationType type;

    @Column(name = "reference_id", nullable = true)
    private UUID referenceId;

}
