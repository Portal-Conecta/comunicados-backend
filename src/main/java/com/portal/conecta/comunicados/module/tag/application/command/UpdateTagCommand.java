package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.presentation.dto.request.UpdateTagRequest;

import java.util.UUID;

public record UpdateTagCommand(

    UUID id,
    UpdateTagRequest data

) {}