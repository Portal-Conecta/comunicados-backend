package com.portal.conecta.comunicados.module.tag.domain.port;

import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByEntityTypeAndActiveTrue(TagEntityType entityType);

    Optional<Tag> findByEntityTypeAndHubEntityId(TagEntityType entityType, String hubEntityId);

    Optional<Tag> findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType entityType, String hubEntityId);

    Optional<Tag> findFirstByEntityTypeAndActiveTrueOrderByCreatedAtAsc(TagEntityType entityType);

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE tag SET id = :id WHERE entity_type = :entityType AND hub_entity_id = :hubEntityId", nativeQuery = true)
    int updateIdByEntityTypeAndHubEntityId(
            @Param("id") UUID id,
            @Param("entityType") String entityType,
            @Param("hubEntityId") String hubEntityId
    );

}
