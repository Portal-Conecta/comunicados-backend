package com.portal.conecta.comunicados.module.tag.domain.port;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.conecta.comunicados.module.tag.domain.model.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
