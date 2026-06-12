package com.portal.conecta.comunicados.module.tag.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UpsertTagFromCoreUseCase {

    private final TagRepository tagRepository;

    @Transactional
    public Tag execute(UpsertTagFromCoreCommand command) {
        Instant now = Instant.now();

        return tagRepository.findByEntityTypeAndHubEntityId(command.entityType(), command.hubEntityId())
                .map(existing -> {
                    command.applyTo(existing, now);
                    return tagRepository.save(existing);
                })
                .orElseGet(() -> tagRepository.save(command.toNewEntity(now)));
    }
}
