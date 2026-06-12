package com.portal.conecta.comunicados.module.tag.application.usecase;

import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;
import com.portal.conecta.comunicados.module.tag.domain.model.Tag;
import com.portal.conecta.comunicados.module.tag.domain.port.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListTagsUseCase {

    private final TagRepository tagRepository;

    public List<Tag> execute(TagEntityType entityType) {
        if(entityType != null) {
            return tagRepository.findByEntityTypeAndActiveTrue(entityType);
        }
        return tagRepository.findAll()
                .stream()
                .filter(Tag::isActive)
                .toList();
    }
}
