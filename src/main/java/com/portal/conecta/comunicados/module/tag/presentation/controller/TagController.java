package com.portal.conecta.comunicados.module.tag.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @GetMapping
    public ResponseEntity<List<Object>> listTags() {
        return ResponseEntity.ok(null);
    }

}