package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;

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
