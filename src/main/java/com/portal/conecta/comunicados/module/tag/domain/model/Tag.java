package com.portal.conecta.comunicados.module.tag.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(
        name = "tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tag_entity_hub",
                columnNames = {"entity_type", "hub_entity_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private TagEntityType entityType;

    @Column(name = "hub_entity_id", nullable = false)
    private UUID hubEntityId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
