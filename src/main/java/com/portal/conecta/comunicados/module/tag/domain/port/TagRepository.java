package com.portal.conecta.comunicados.module.tag.domain.port;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    java.util.List<Tag> findByEntityTypeAndActiveTrue(TagEntityType entityType);

    Optional<Tag> findByEntityTypeAndHubEntityId(TagEntityType entityType, UUID hubEntityId);
}
