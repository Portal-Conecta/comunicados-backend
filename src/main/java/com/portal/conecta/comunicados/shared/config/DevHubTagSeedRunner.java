package com.portal.conecta.comunicados.shared.config;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

/** Cria no ambiente de desenvolvimento as tags de cursos e turmas da massa do Core. */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-seed.tags", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevHubTagSeedRunner implements ApplicationRunner {

    private static final List<TagSeed> TAGS = List.of(
            course("00000000-0000-0000-0000-000000000001", "Aprendizagem Industrial em Desenvolvimento de Sistemas"),
            course("00000000-0000-0000-0000-000000000002", "Aprendizagem Técnica em Eletrotécnica"),
            course("00000000-0000-0000-0000-000000000003", "Aprendizagem Industrial em Operador de Usinagem"),
            course("00000000-0000-0000-0000-000000000004", "Aprendizagem Técnica em Eletrônica"),
            course("00000000-0000-0000-0000-000000000005", "Aprendizagem Técnica em Cibersistemas para Automação"),
            course("00000000-0000-0000-0000-000000000006", "Aprendizagem Técnica em Manutenção de Máquinas Industriais"),
            course("00000000-0000-0000-0000-000000000007", "Aprendizagem Industrial de Operador em Montagem de Produtos Eletroeletrônicos"),
            course("00000000-0000-0000-0000-000000000008", "Aprendizagem Industrial de Operador em Tintas e Vernizes"),
            course("00000000-0000-0000-0000-000000000009", "Aprendizagem Técnica em Mecânica"),
            course("00000000-0000-0000-0000-000000000010", "Aprendizagem Técnica em Química"),
            course("00000000-0000-0000-0000-000000000011", "Aprendizagem Industrial de Operador em Eletromecânica"),
            course("00000000-0000-0000-0000-000000000012", "Aprendizagem Industrial em Assistente de Análise de Dados"),
            classroom("00000000-0000-0000-0000-000000000101", "MI78"),
            classroom("00000000-0000-0000-0000-000000000102", "MI77"),
            classroom("00000000-0000-0000-0000-000000000103", "MT78"),
            classroom("00000000-0000-0000-0000-000000000104", "MT77"),
            classroom("00000000-0000-0000-0000-000000000105", "WU79"),
            classroom("00000000-0000-0000-0000-000000000106", "WU78"),
            classroom("00000000-0000-0000-0000-000000000107", "ME78"),
            classroom("00000000-0000-0000-0000-000000000108", "ME77"),
            classroom("00000000-0000-0000-0000-000000000109", "MA78"),
            classroom("00000000-0000-0000-0000-000000000110", "MA77"),
            classroom("00000000-0000-0000-0000-000000000111", "MM78"),
            classroom("00000000-0000-0000-0000-000000000112", "MM77"),
            classroom("00000000-0000-0000-0000-000000000113", "WME78"),
            classroom("00000000-0000-0000-0000-000000000114", "WME77"),
            classroom("00000000-0000-0000-0000-000000000115", "WQ77"),
            classroom("00000000-0000-0000-0000-000000000116", "MF78"),
            classroom("00000000-0000-0000-0000-000000000117", "MF77"),
            classroom("00000000-0000-0000-0000-000000000118", "MQ78"),
            classroom("00000000-0000-0000-0000-000000000119", "WM77"),
            classroom("00000000-0000-0000-0000-000000000120", "WA77")
    );

    private final TagRepository tagRepository;
    private final TransactionTemplate transactionTemplate;

    public DevHubTagSeedRunner(TagRepository tagRepository, TransactionTemplate transactionTemplate) {
        this.tagRepository = tagRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> seedTags());
    }

    void seedTags() {
        TAGS.forEach(this::findOrCreateTag);
    }

    private void findOrCreateTag(TagSeed seed) {
        Instant now = Instant.now();
        tagRepository.findByEntityTypeAndHubEntityId(seed.entityType(), seed.id().toString())
                .ifPresentOrElse(existing -> {
                    existing.setName(seed.name());
                    existing.setActive(true);
                    existing.setUpdatedAt(now);
                    tagRepository.save(existing);
                }, () -> tagRepository.save(Tag.builder()
                        .name(seed.name())
                        .entityType(seed.entityType())
                        .hubEntityId(seed.id().toString())
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()));
        tagRepository.updateIdByEntityTypeAndHubEntityId(seed.id(), seed.entityType().name(), seed.id().toString());
    }

    private static TagSeed course(String id, String name) {
        return new TagSeed(UUID.fromString(id), TagEntityType.COURSE, name);
    }

    private static TagSeed classroom(String id, String name) {
        return new TagSeed(UUID.fromString(id), TagEntityType.CLASS, name);
    }

    private record TagSeed(UUID id, TagEntityType entityType, String name) {
    }
}
