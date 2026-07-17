package com.portal.conecta.comunicados.module.comunicado.domain.service;

/**
 * Resultado da normalização de description: HTML sanitizado + versão plain-text derivada no servidor.
 */
public record NormalizedAnnouncementDescription(String html, String plain) {
}
