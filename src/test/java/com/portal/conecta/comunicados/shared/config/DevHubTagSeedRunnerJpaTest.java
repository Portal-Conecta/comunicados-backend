package com.portal.conecta.comunicados.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
class DevHubTagSeedRunnerJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TagRepository tagRepository;

    @Test
    void shouldSeedTagsWithTheSameManualIdsUsedByCore() {
        new DevHubTagSeedRunner(tagRepository, null).seedTags();
        entityManager.flush();
        entityManager.clear();

        assertEquals(32, tagRepository.count());
        assertTagId(TagEntityType.COURSE, "00000000-0000-0000-0000-000000000001");
        assertTagId(TagEntityType.CLASS, "00000000-0000-0000-0000-000000000101");
        assertTagId(TagEntityType.CLASS, "00000000-0000-0000-0000-000000000120");
    }

    private void assertTagId(TagEntityType entityType, String expectedId) {
        UUID id = UUID.fromString(expectedId);
        var tag = tagRepository.findByEntityTypeAndHubEntityId(entityType, expectedId).orElseThrow();

        assertEquals(id, tag.getId());
        assertEquals(expectedId, tag.getHubEntityId());
    }
}
