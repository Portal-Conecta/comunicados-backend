package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/posts/{postId}/images")
public class PostImageController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> upload(@PathVariable UUID postId, @RequestPart("file") MultipartFile file, @RequestPart Object meta) {
        return ResponseEntity.status(201).body(null);
    }

    @GetMapping
    public ResponseEntity<Object> list(@PathVariable UUID postId) {
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId, @PathVariable UUID imageId) {
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{imageId}/thumbnail")
    public ResponseEntity<Object> setThumbnail(@PathVariable UUID postId, @PathVariable UUID imageId) {
        return ResponseEntity.ok(null);
    }
}