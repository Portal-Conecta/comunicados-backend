package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.exception.InvalidTagCodeException;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Vincula tags de enum fixo local (sem evento do Core) a um comunicado, pelo código do enum
 * (ex.: {@code ShiftCode.FULL_AM_PM}, {@code UserType.TEACHER}). Usado para filtros secundários
 * de visibilidade (turno, papel de usuário) que não derivam dos destinos do comunicado.
 *
 * <p>Para {@link TagEntityType#ROLE} e {@link TagEntityType#SHIFT}, garante a existência da tag
 * (seed sob demanda) e falha com {@link InvalidTagCodeException} se algum código não puder ser
 * vinculado — evita publicar “broadcast” silencioso quando o front envia restrição de papel/turno.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkTagsByCodeUseCase {

    private static final Map<String, String> ROLE_DISPLAY_NAMES = Map.of(
            "STUDENT", "Alunos",
            "REPRESENTATIVE", "Responsáveis",
            "TEACHER", "Professores",
            "SENAI", "SENAI",
            "WEG", "WEG",
            "ADMIN", "Administradores"
    );

    private static final Map<String, String> SHIFT_DISPLAY_NAMES = Map.of(
            "FULL_AM_PM", "Manhã e tarde",
            "FULL_PM_NT", "Tarde e noite"
    );

    private final TagRepository tagRepository;
    private final AnnouncementTagRepository announcementTagRepository;

    public void execute(Announcement announcement, TagEntityType entityType, List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }

        List<String> distinctCodes = codes.stream().distinct().toList();
        List<Tag> resolved = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();

        for (String code : distinctCodes) {
            Optional<Tag> tag = resolveActiveTag(entityType, code);
            if (tag.isPresent()) {
                resolved.add(tag.get());
            } else {
                unresolved.add(code);
            }
        }

        if (!unresolved.isEmpty()) {
            throw new InvalidTagCodeException(entityType, unresolved);
        }

        Set<UUID> alreadyLinked = announcementTagRepository.findByAnnouncementId(announcement.getId())
                .stream()
                .map(link -> link.getTag().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AnnouncementTag> newLinks = resolved.stream()
                .filter(tag -> alreadyLinked.add(tag.getId()))
                .map(tag -> AnnouncementTag.builder().announcement(announcement).tag(tag).build())
                .toList();

        if (newLinks.isEmpty()) {
            return;
        }

        announcementTagRepository.saveAll(newLinks);
        log.info("Auto-link: {} tag(s) {} vinculada(s) ao comunicado {}",
                newLinks.size(), entityType, announcement.getId());
    }

    private Optional<Tag> resolveActiveTag(TagEntityType entityType, String code) {
        Optional<Tag> existing = tagRepository.findByEntityTypeAndHubEntityId(entityType, code);
        if (existing.isPresent()) {
            Tag tag = existing.get();
            if (tag.isActive()) {
                return Optional.of(tag);
            }
            tag.setActive(true);
            tag.setUpdatedAt(Instant.now());
            return Optional.of(tagRepository.save(tag));
        }

        if (entityType != TagEntityType.ROLE && entityType != TagEntityType.SHIFT) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        Tag created = Tag.builder()
                .name(displayName(entityType, code))
                .entityType(entityType)
                .hubEntityId(code)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return Optional.of(tagRepository.save(created));
    }

    private static String displayName(TagEntityType entityType, String code) {
        if (entityType == TagEntityType.ROLE) {
            return ROLE_DISPLAY_NAMES.getOrDefault(code, code);
        }
        if (entityType == TagEntityType.SHIFT) {
            return SHIFT_DISPLAY_NAMES.getOrDefault(code, code);
        }
        return code;
    }
}
