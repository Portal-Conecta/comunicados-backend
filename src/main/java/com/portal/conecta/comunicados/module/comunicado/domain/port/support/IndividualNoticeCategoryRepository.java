package com.portal.conecta.comunicados.module.comunicado.domain.port.support;

import com.portal.conecta.comunicados.module.comunicado.domain.model.IndividualNoticeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IndividualNoticeCategoryRepository extends JpaRepository<IndividualNoticeCategory, UUID> {

    List<IndividualNoticeCategory> findByDeletedAtIsNull();
}