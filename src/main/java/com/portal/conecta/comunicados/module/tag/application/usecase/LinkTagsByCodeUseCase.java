package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Vincula tags de enum fixo local (sem evento do Core) a um comunicado, pelo código do enum
 * (ex.: {@code ShiftCode.FULL_AM_PM}, {@code UserType.TEACHER}). Usado para filtros secundários
 * de visibilidade (turno, papel de usuário) que não derivam dos destinos do comunicado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkTagsByCodeUseCase {

    private final TagRepository tagRepository;
    private final AnnouncementTagRepository announcementTagRepository;

    public void execute(Announcement announcement, TagEntityType entityType, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }

        Set<UUID> alreadyLinked = announcementTagRepository.findByAnnouncementId(announcement.getId())
                .stream()
                .map(link -> link.getTag().getId())
                .collect(Collectors.toSet());

        List<AnnouncementTag> newLinks = codes.stream()
                .distinct()
                .map(code -> tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(entityType, code))
                .flatMap(Optional::stream)
                .filter(tag -> !alreadyLinked.contains(tag.getId()))
                .map(tag -> AnnouncementTag.builder().announcement(announcement).tag(tag).build())
                .toList();

        if (newLinks.isEmpty()) {
            log.warn("Nenhuma tag {} ativa encontrada para os códigos {} do comunicado {}.",
                    entityType, codes, announcement.getId());
            return;
        }

        announcementTagRepository.saveAll(newLinks);
        log.info("Auto-link: {} tag(s) {} vinculada(s) ao comunicado {}",
                newLinks.size(), entityType, announcement.getId());
    }
}
