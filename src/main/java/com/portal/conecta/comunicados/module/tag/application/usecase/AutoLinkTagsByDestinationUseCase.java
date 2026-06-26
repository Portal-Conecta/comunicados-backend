package com.portal.conecta.comunicados.module.tag.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.AnnouncementDestinationType;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.dto.TagLinkDestinationCommand;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoLinkTagsByDestinationUseCase {

    private static final Map<AnnouncementDestinationType, TagEntityType> DESTINATION_TO_TAG_TYPE = Map.of(
            AnnouncementDestinationType.CLASS, TagEntityType.CLASS,
            AnnouncementDestinationType.USER, TagEntityType.USER,
            AnnouncementDestinationType.COURSE, TagEntityType.COURSE,
            AnnouncementDestinationType.GENERAL, TagEntityType.GENERAL
    );

    private final TagRepository tagRepository;
    private final AnnouncementTagRepository announcementTagRepository;

    public void execute(Announcement announcement, List<TagLinkDestinationCommand> destinations, List<UUID> explicitTagIds) {

        Set<UUID> alreadyLinked = announcementTagRepository
                .findByAnnouncementId(announcement.getId())
                .stream()
                .map(at -> at.getTag().getId())
                .collect(Collectors.toSet());

        Map<UUID, Tag> uniqueTagsToLink = new HashMap<>();

        destinations.stream()
                .map(this::resolveTag)
                .flatMap(Optional::stream)
                .forEach(tag -> uniqueTagsToLink.put(tag.getId(), tag));

        if (explicitTagIds != null && !explicitTagIds.isEmpty()) {
            explicitTagIds.stream()
                    .map(tagRepository::findById) // Busca a tag real pelo ID
                    .flatMap(Optional::stream)
                    .forEach(tag -> uniqueTagsToLink.put(tag.getId(), tag));
        }

        List<AnnouncementTag> newLinks = uniqueTagsToLink.values().stream()
                .filter(tag -> !alreadyLinked.contains(tag.getId()))
                .map(tag -> AnnouncementTag.builder().announcement(announcement).tag(tag).build())
                .toList();

        if (!newLinks.isEmpty()) {
            announcementTagRepository.saveAll(newLinks);
            log.info("Link: {} novas tags vinculadas ao comunicado {}", newLinks.size(), announcement.getId());
        }
    }

    private Optional<Tag> resolveTag(TagLinkDestinationCommand destination) {
        TagEntityType tagType = DESTINATION_TO_TAG_TYPE.get(destination.type());
        if (tagType == null) return Optional.empty();

        if (destination.hubEntityId() != null) {
            return findByTypeAndHubEntity(tagType, destination.hubEntityId());
        }
        return findFirstActiveByType(tagType);
    }

    private Optional<Tag> findByTypeAndHubEntity(TagEntityType type, UUID hubEntityId) {
        Optional<Tag> tag = tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(type, hubEntityId);
        if (tag.isEmpty()) {
            log.debug("Auto-link: tag ativa não encontrada para type={} hubEntityId={}", type, hubEntityId);
        }
        return tag;
    }

    private Optional<Tag> findFirstActiveByType(TagEntityType type) {
        Optional<Tag> tag = tagRepository.findFirstByEntityTypeAndActiveTrueOrderByCreatedAtAsc(type);
        if (tag.isEmpty()) {
            log.debug("Auto-link: nenhuma tag ativa encontrada para type={}", type);
        }
        return tag;
    }
}