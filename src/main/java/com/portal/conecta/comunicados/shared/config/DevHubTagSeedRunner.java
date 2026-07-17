package com.portal.conecta.comunicados.shared.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties.HubApiProperties;
import com.portal.conecta.comunicados.module.tag.application.command.UpsertTagFromCoreCommand;
import com.portal.conecta.comunicados.module.tag.application.usecase.UpsertTagFromCoreUseCase;
import com.portal.conecta.comunicados.module.tag.domain.enums.TagEntityType;

/**
 * Espelha cursos/turmas do Hub como tags locais no profile {@code dev}.
 *
 * <p>O Core popula a massa via {@code DevDataInitializer} sem publicar eventos RabbitMQ;
 * sem esse seed o comunicados fica sem tags COURSE/CLASS e o auto-link de destinos falha.
 * Os UUIDs são dinâmicos — por isso o seed consulta o Hub em runtime (não usa SQL fixo).
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev-seed.hub-tags", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevHubTagSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevHubTagSeedRunner.class);

    private final HubApiProperties hubApiProperties;
    private final DevHubTagSeedProperties seedProperties;
    private final UpsertTagFromCoreUseCase upsertTagFromCoreUseCase;

    public DevHubTagSeedRunner(
            HubApiProperties hubApiProperties,
            DevHubTagSeedProperties seedProperties,
            UpsertTagFromCoreUseCase upsertTagFromCoreUseCase
    ) {
        this.hubApiProperties = hubApiProperties;
        this.seedProperties = seedProperties;
        this.upsertTagFromCoreUseCase = upsertTagFromCoreUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hubApiProperties.mockEnabled()) {
            log.info(
                    "[DEV SEED] hub.api.mock-enabled=true — seed COURSE/CLASS ignorado. "
                            + "Use HUB_MOCK_ENABLED=false apontando para o Core real."
            );
            return;
        }

        log.info("[DEV SEED] Sincronizando tags COURSE/CLASS a partir do Hub...");

        for (int attempt = 1; attempt <= seedProperties.maxAttempts(); attempt++) {
            try {
                String token = login(hubClient());
                int courses = seedCourses(token);
                int classes = seedClasses(token);
                log.info(
                        "[DEV SEED] Tags sincronizadas do Hub: {} cursos, {} turmas.",
                        courses,
                        classes
                );
                return;
            } catch (RuntimeException exception) {
                log.warn(
                        "[DEV SEED] Tentativa {}/{} falhou ({}). Nova tentativa em {}ms.",
                        attempt,
                        seedProperties.maxAttempts(),
                        exception.getMessage(),
                        seedProperties.retryDelayMs()
                );
                if (attempt == seedProperties.maxAttempts()) {
                    log.error(
                            "[DEV SEED] Não foi possível sincronizar tags COURSE/CLASS do Hub após {} tentativas. Publish com destino CLASS/COURSE pode falhar até o Core estar acessível.",
                            seedProperties.maxAttempts(),
                            exception
                    );
                    return;
                }
                sleep(seedProperties.retryDelayMs());
            }
        }
    }

    private RestClient hubClient() {
        return RestClient.builder()
                .baseUrl(hubApiProperties.url())
                .build();
    }

    private String login(RestClient client) {
        LoginResponse response = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(seedProperties.email(), seedProperties.password()))
                .retrieve()
                .body(LoginResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Login no Hub não retornou accessToken.");
        }
        return response.accessToken();
    }

    private int seedCourses(String token) {
        HubCoursesResponse response = hubClient().get()
                .uri("/courses")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(HubCoursesResponse.class);

        if (response == null || response.courses() == null) {
            return 0;
        }

        int count = 0;
        for (HubCourseItem course : response.courses()) {
            if (course == null || course.id() == null) {
                continue;
            }
            String name = course.name() != null && !course.name().isBlank()
                    ? course.name()
                    : (course.code() != null ? course.code() : course.id().toString());
            upsertTagFromCoreUseCase.execute(
                    UpsertTagFromCoreCommand.forHubEntity(TagEntityType.COURSE, course.id().toString(), name)
            );
            count++;
        }
        return count;
    }

    private int seedClasses(String token) {
        int count = 0;
        int page = 0;
        int totalPages = 1;

        while (page < totalPages) {
            final int currentPage = page;
            HubClassesPage response = hubClient().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/classes")
                            .queryParam("page", currentPage)
                            .queryParam("size", 100)
                            .queryParam("includeInactive", true)
                            .build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(HubClassesPage.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                break;
            }

            totalPages = (int) Math.max(1, response.totalPages());
            for (HubClassItem cls : response.items()) {
                if (cls == null || cls.id() == null) {
                    continue;
                }
                String name = cls.name() != null && !cls.name().isBlank()
                        ? cls.name()
                        : cls.id().toString();
                upsertTagFromCoreUseCase.execute(
                        UpsertTagFromCoreCommand.forHubEntity(TagEntityType.CLASS, cls.id().toString(), name)
                );
                count++;
            }
            page++;
        }
        return count;
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Seed interrompido.", interrupted);
        }
    }

    private record LoginRequest(String email, String password) {
    }

    private record LoginResponse(String accessToken, String refreshToken, Long expiresIn) {
    }

    private record HubCoursesResponse(List<HubCourseItem> courses) {
    }

    private record HubCourseItem(UUID id, String name, String code) {
    }

    private record HubClassesPage(
            List<HubClassItem> items,
            int page,
            int size,
            long totalElements,
            long totalPages
    ) {
    }

    private record HubClassItem(
            UUID id,
            String name,
            Integer number,
            String shift,
            UUID courseId,
            boolean active
    ) {
    }
}
