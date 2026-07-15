package com.portal.conecta.comunicados;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.portal.conecta.comunicados.module.comunicado.domain.service.AnnouncementDescriptionNormalizer;
import com.portal.conecta.comunicados.module.comunicado.domain.service.NormalizedAnnouncementDescription;

class AnnouncementDescriptionNormalizerTest {

    private AnnouncementDescriptionNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new AnnouncementDescriptionNormalizer();
    }

    @Test
    void shouldKeepAllowedTagsAndDerivePlainText() {
        NormalizedAnnouncementDescription result = normalizer.normalize(
                "<p>Olá <strong>mundo</strong></p><ul><li>item</li></ul>");

        assertThat(result.html()).contains("<p>", "<strong>", "<ul>", "<li>");
        assertThat(result.plain()).isEqualTo("Olá mundo item");
    }

    @Test
    void shouldStripScriptAndEventHandlers() {
        NormalizedAnnouncementDescription result = normalizer.normalize(
                "<p onclick=\"alert(1)\">x</p><script>alert(2)</script><a href=\"javascript:alert(3)\">link</a>");

        assertThat(result.html()).doesNotContain("script", "onclick", "javascript:");
        assertThat(result.plain()).contains("x");
    }

    @Test
    void shouldPreserveHttpLinks() {
        NormalizedAnnouncementDescription result = normalizer.normalize(
                "<p><a href=\"https://example.com\">site</a></p>");

        assertThat(result.html()).contains("href=\"https://example.com\"");
        assertThat(result.plain()).isEqualTo("site");
    }

    @Test
    void shouldTreatPlainTextAsIs() {
        NormalizedAnnouncementDescription result = normalizer.normalize("Texto puro sem HTML");

        assertThat(result.html()).isEqualTo("Texto puro sem HTML");
        assertThat(result.plain()).isEqualTo("Texto puro sem HTML");
    }

    @Test
    void shouldNormalizeNullAndBlank() {
        assertThat(normalizer.normalize(null).html()).isEmpty();
        assertThat(normalizer.normalize(null).plain()).isEmpty();
        assertThat(normalizer.normalize("   ").plain()).isEmpty();
    }
}
