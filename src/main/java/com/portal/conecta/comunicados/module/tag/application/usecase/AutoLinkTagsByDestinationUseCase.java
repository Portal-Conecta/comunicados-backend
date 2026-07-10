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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoLinkTagsByDestinationUseCase {

    private static final Map<AnnouncementDestinationType, TagEntityType> DESTINATION_TO_TAG_TYPE = Map.of(
            AnnouncementDestinationType.CLASS, TagEntityType.CLASS,
            AnnouncementDestinationType.COURSE, TagEntityType.COURSE,
            AnnouncementDestinationType.GENERAL, TagEntityType.GENERAL,
            AnnouncementDestinationType.USER, TagEntityType.USER
    );

    private final TagRepository tagRepository;
    private final AnnouncementTagRepository announcementTagRepository;
    private final ResolveTagForDestinationUseCase resolveTagForDestinationUseCase;

    public void execute(Announcement announcement, List<TagLinkDestinationCommand> destinations) {
        execute(announcement, destinations, null);
    }

    public void execute(Announcement announcement, List<TagLinkDestinationCommand> destinations, List<UUID> explicitTagIds) {
        Set<UUID> alreadyLinked = announcementTagRepository
                .findByAnnouncementId(announcement.getId())
                .stream()
                .map(at -> at.getTag().getId())
                .collect(Collectors.toSet());

        List<Tag> destinationTags = destinations.stream()
                .map(this::resolveTag)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        List<Tag> explicitTags = (explicitTagIds == null || explicitTagIds.isEmpty())
                ? List.of()
                : tagRepository.findAllById(explicitTagIds);

        Set<UUID> seen = new HashSet<>();
        List<AnnouncementTag> newLinks = Stream.concat(destinationTags.stream(), explicitTags.stream())
                .filter(tag -> !alreadyLinked.contains(tag.getId()))
                .filter(tag -> seen.add(tag.getId()))
                .map(tag -> AnnouncementTag.builder().announcement(announcement).tag(tag).build())
                .toList();

        if (!newLinks.isEmpty()) {
            announcementTagRepository.saveAll(newLinks);
            log.info("Auto-link: {} novas tags vinculadas ao comunicado {}", newLinks.size(), announcement.getId());
        }
    }

    private Optional<Tag> resolveTag(TagLinkDestinationCommand destination) {
        TagEntityType tagType = DESTINATION_TO_TAG_TYPE.get(destination.type());
        if (tagType == null) return Optional.empty();

        if (destination.hubEntityId() != null) {
            return findByTypeAndHubEntity(tagType, destination.hubEntityId().toString());
        }
        return findFirstActiveByType(tagType);
    }

    private Optional<Tag> findByTypeAndHubEntity(TagEntityType type, String hubEntityId) {
        if (type == TagEntityType.COURSE || type == TagEntityType.CLASS) {
            return resolveTagForDestinationUseCase.resolve(type, hubEntityId);
        }

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
