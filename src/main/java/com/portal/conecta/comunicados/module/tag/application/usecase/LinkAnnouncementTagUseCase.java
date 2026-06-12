package com.portal.conecta.comunicados.module.tag.application.usecase;

import com.portal.conecta.comunicados.module.comunicado.domain.model.Announcement;
import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementTag;
import com.portal.conecta.comunicados.module.comunicado.domain.port.announcement.AnnouncementRepository;
import com.portal.conecta.comunicados.module.comunicado.domain.port.support.AnnouncementTagRepository;
import com.portal.conecta.comunicados.module.tag.application.command.LinkAnnouncementTagCommand;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagNotFoundException;
import com.portal.conecta.comunicados.module.tag.domain.exception.TagPermissionDeniedException;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import com.portal.conecta.comunicados.shared.context.RequestContext;
import com.portal.conecta.comunicados.shared.context.RequestContextProvider;
import com.portal.conecta.comunicados.shared.context.UserType;
import com.portal.conecta.comunicados.shared.exception.UnauthorizedUserException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LinkAnnouncementTagUseCase {

    private static final EnumSet<UserType> PRIVILEGED = EnumSet.of(
            UserType.SENAI, UserType.WEG, UserType.ADMIN
    );

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementTagRepository announcementTagRepository;
    private final TagRepository tagRepository;
    private final RequestContextProvider contextProvider;

    @Transactional
    public List<AnnouncementTag> execute(LinkAnnouncementTagCommand command) {
        RequestContext context = contextProvider.getRequestContext();

        Announcement announcement = announcementRepository.findById(command.announcementId())
                .orElseThrow(() -> new UnauthorizedUserException(
                        "Comunicado não encontrado ou sem permissão de acesso."));

        boolean isAuthor = announcement.getCreatedByUserId().equals(context.userId());
        boolean isPrivileged = PRIVILEGED.contains(context.userType());

        if (!isAuthor && !isPrivileged) {
            throw new TagPermissionDeniedException();
        }

        List<AnnouncementTag> existingLinks = announcementTagRepository
                .findByAnnouncementId(command.announcementId());

        List<AnnouncementTag> newLinks = command.tagIds().stream()
                .filter(tagId -> existingLinks.stream()
                        .noneMatch(at -> at.getTag().getId().equals(tagId)))
                .map(tagId -> {
                    Tag tag = tagRepository.findById(tagId)
                            .orElseThrow(() -> new TagNotFoundException(tagId));
                    return AnnouncementTag.builder()
                            .announcement(announcement)
                            .tag(tag)
                            .build();
                })
                .toList();

        return announcementTagRepository.saveAll(newLinks);
    }
}

