package com.portal.conecta.comunicados.module.comunicado.application.dto;

import com.portal.conecta.comunicados.module.comunicado.domain.model.AnnouncementFile;

public record AnnouncementFileView(AnnouncementFile file, String displayUrl) {}
