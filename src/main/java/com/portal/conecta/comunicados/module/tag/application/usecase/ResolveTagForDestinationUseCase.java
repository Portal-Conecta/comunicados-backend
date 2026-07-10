package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.port.hub.HubCoursePort;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.HubClassPort;
import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResolveTagForDestinationUseCase {

    private final TagRepository tagRepository;
    private final UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;
    private final HubClassPort hubClassPort;
    private final HubCoursePort hubCoursePort;

    public Optional<Tag> resolve(TagEntityType type, String hubEntityId) {
        Optional<Tag> existing = tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(type, hubEntityId);
        if (existing.isPresent()) {
            return existing;
        }

        return upsertFromHub(type, hubEntityId);
    }

    private Optional<Tag> upsertFromHub(TagEntityType type, String hubEntityId) {
        Optional<String> name = switch (type) {
            case COURSE -> hubCoursePort.findCourseNameById(UUID.fromString(hubEntityId));
            case CLASS -> hubClassPort.findClassById(UUID.fromString(hubEntityId)).map(info -> info.name());
            default -> Optional.empty();
        };

        if (name.isEmpty()) {
            log.warn("Tag ausente e Hub não retornou entidade para upsert on-demand: type={} hubEntityId={}",
                    type, hubEntityId);
            return Optional.empty();
        }

        Tag tag = upsertTagFromCoreUseCase.execute(
                UpsertTagFromCoreCommand.forHubEntity(type, hubEntityId, name.get()));
        return Optional.of(tag);
    }
}
