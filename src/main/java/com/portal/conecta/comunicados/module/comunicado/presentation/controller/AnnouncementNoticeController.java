package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AnnouncementNoticeController {

    @PostMapping("/api/posts/{postId}/notices")
    public ResponseEntity<Object> create(@PathVariable UUID postId, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @GetMapping("/api/posts/{postId}/notices")
    public ResponseEntity<Object> list(@PathVariable UUID postId) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("api/notices/{noticeId}/resolve")
    public ResponseEntity<Object> resolve(@PathVariable UUID noticeId, @Valid @RequestBody Object request) {
        return  ResponseEntity.ok(null);
    }
}
