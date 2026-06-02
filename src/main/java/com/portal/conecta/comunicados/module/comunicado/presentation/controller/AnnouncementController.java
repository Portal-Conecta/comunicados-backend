package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class AnnouncementController {

    @PostMapping
    public ResponseEntity<Object> save(@Valid @RequestBody Object request) {
        return ResponseEntity.created(URI.create("/api/posts/")).body(null);
    }

    @GetMapping
    public ResponseEntity<Object> list() {
        return  ResponseEntity.ok(null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> partialUpdate(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<Object> publish(@PathVariable UUID id, @RequestBody (required = false) Object request) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{id}/schedule")
    public ResponseEntity<Object> schedule(@PathVariable UUID id, @Valid @RequestBody Object request) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{id}/cancel-schedule")
    public ResponseEntity<Object> cancelSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<Object> pin(@PathVariable UUID id, @RequestBody (required = false) Object request) {
        return ResponseEntity.ok(null);
    }

    @PatchMapping("/{id}/unpin")
    public ResponseEntity<Object> unpin(@PathVariable UUID id) {
        return ResponseEntity.ok(null);
    }

    @GetMapping("/pinned")
    public ResponseEntity<Object> listPinned() {
        return ResponseEntity.ok(null);
    }
}