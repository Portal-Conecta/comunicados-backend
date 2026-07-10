package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.enums.ShiftCode;
import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkShiftTagsUseCase {

    private final TagRepository tagRepository;
    private final AnnouncementTagRepository announcementTagRepository;

    public void execute(Announcement announcement, List<ShiftCode> shiftCodes) {
        if (shiftCodes == null || shiftCodes.isEmpty()) {
            return;
        }

        var alreadyLinked = announcementTagRepository.findByAnnouncementId(announcement.getId())
                .stream()
                .map(link -> link.getTag().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<AnnouncementTag> newLinks = shiftCodes.stream()
                .map(ShiftCode::name)
                .map(code -> tagRepository.findByEntityTypeAndHubEntityIdAndActiveTrue(TagEntityType.SHIFT, code))
                .flatMap(Optional::stream)
                .filter(tag -> !alreadyLinked.contains(tag.getId()))
                .map(tag -> AnnouncementTag.builder().announcement(announcement).tag(tag).build())
                .toList();

        if (newLinks.isEmpty()) {
            shiftCodes.forEach(code -> log.warn(
                    "Turno {} informado no publish, mas tag SHIFT ativa não encontrada.", code));
            return;
        }

        announcementTagRepository.saveAll(newLinks);
        log.info("Auto-link: {} tag(s) de turno vinculada(s) ao comunicado {}", newLinks.size(), announcement.getId());
    }
}
