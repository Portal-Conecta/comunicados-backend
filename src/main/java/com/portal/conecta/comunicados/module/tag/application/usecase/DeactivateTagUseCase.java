package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.tag.application.command.DeactivateTagCommand;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DeactivateTagUseCase {

    private final TagRepository tagRepository;

    @Transactional
    public void execute(DeactivateTagCommand command) {
        Instant now = Instant.now();

        tagRepository.findByEntityTypeAndHubEntityId(command.entityType(), command.hubEntityId())
                .ifPresent(tag -> {
                    tag.setActive(false);
                    tag.setUpdatedAt(now);
                    tagRepository.save(tag);
                });
    }
}
