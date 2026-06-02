package com.portal.conecta.comunicados.module.tag.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(

    @Size(max = 255)
    String name,

    Boolean active

) {}