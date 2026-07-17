package com.portal.conecta.comunicados.module.comunicado.domain.model;

import java.util.UUID;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "announcement_tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementTag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Announcement announcement;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Tag tag;

}
