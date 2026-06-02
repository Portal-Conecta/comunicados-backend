package com.portal.conecta.comunicados.module.tag.application.command;

import com.portal.conecta.comunicados.module.tag.presentation.dto.request.CreateTagRequest;

public record CreateTagCommand(

    CreateTagRequest data

) {}