package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import jakarta.persistence.*;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Announcement announcement;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AnnouncementDestinationType type;

    @Column(name = "reference_id", nullable = true)
    private UUID referenceId;

}
