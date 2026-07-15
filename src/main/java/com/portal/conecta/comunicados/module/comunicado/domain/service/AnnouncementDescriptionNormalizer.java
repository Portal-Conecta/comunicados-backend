package com.portal.conecta.comunicados.module.comunicado.domain.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * Sanitiza HTML de {@code description} (allowlist TipTap) e deriva {@code descriptionPlain}
 * no servidor — nunca confiar no client para o plain-text.
 */
@Component
public class AnnouncementDescriptionNormalizer {

    private static final Safelist ALLOWLIST = Safelist.none()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https", "mailto");

    private static final Document.OutputSettings OUTPUT = new Document.OutputSettings()
            .prettyPrint(false);

    public NormalizedAnnouncementDescription normalize(String rawDescription) {
        String input = rawDescription == null ? "" : rawDescription;
        String html = Jsoup.clean(input, "", ALLOWLIST, OUTPUT);
        String plain = toPlainText(html);
        return new NormalizedAnnouncementDescription(html, plain);
    }

    public String toPlainText(String htmlOrText) {
        if (htmlOrText == null || htmlOrText.isBlank()) {
            return "";
        }
        String text = Jsoup.parse(htmlOrText).text();
        return text.replaceAll("\\s+", " ").trim();
    }
}
