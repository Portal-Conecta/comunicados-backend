package com.portal.conecta.comunicados.module.comunicado.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
}