package com.portal.conecta.comunicados.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Teste ponta a ponta da comunicação via RabbitMQ entre {@code comunicados-backend} e
 * {@code core-backend} (Hub), rodando como dois processos reais via Docker Compose
 * (ver {@code ../infra/docker-compose.yml}, sibling deste repositório).
 *
 * <p>Não roda no {@code ./mvnw test} padrão — a tag "e2e" é excluída no surefire (ver pom.xml).
 * Requer a stack no ar:
 * <pre>
 * docker compose --project-directory .. --env-file ../infra/.env -f ../infra/docker-compose.yml \
 *     up -d --build core-postgres rabbitmq comunicados-postgres core comunicados
 * </pre>
 * Executar com: {@code ./mvnw test -Dgroups=e2e -Dtest=CoreComunicadosRabbitMqE2ETest}
 *
 * <p>As duas pernas do contrato são verificadas no efeito persistido de cada lado, não em
 * detalhes internos (fan-out de destinatários no Core, etc.):
 * <ul>
 *   <li>Core → Comunicados: {@code POST /courses} e {@code POST /classes} no Core devem
 *   resultar numa {@code Tag} em {@code GET /api/tags} no Comunicados.</li>
 *   <li>Comunicados → Core: {@code POST /api/posts/publish} deve resultar numa linha na
 *   tabela {@code notifications} do Core (chave de correlação = id do comunicado) e nada na
 *   DLQ {@code notifications.dispatch.dlq}.</li>
 * </ul>
 */
@Tag("e2e")
class CoreComunicadosRabbitMqE2ETest {

    private static final String CORE_URL = System.getProperty("e2e.core.url", "http://localhost:8082");
    private static final String COMUNICADOS_URL = System.getProperty("e2e.comunicados.url", "http://localhost:8085");
    private static final String RABBITMQ_MGMT_URL = System.getProperty("e2e.rabbitmq.management.url", "http://localhost:15672");
    private static final String RABBITMQ_USER = System.getProperty("e2e.rabbitmq.username", "guest");
    private static final String RABBITMQ_PASSWORD = System.getProperty("e2e.rabbitmq.password", "guest");
    private static final String CORE_DB_URL = System.getProperty("e2e.core.db.url", "jdbc:postgresql://localhost:5432/portal_conecta");
    private static final String CORE_DB_USER = System.getProperty("e2e.core.db.user", "portal_conecta");
    private static final String CORE_DB_PASSWORD = System.getProperty("e2e.core.db.password", "portal_conecta_dev_password");
    private static final String JWT_SECRET = System.getProperty("e2e.jwt.secret", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper JSON = new ObjectMapper();

    private static String adminToken;

    @BeforeAll
    static void setUpStackAndAdminUser() throws Exception {
        assumeStackIsUp();

        UUID adminUserId = UUID.randomUUID();
        adminToken = mintToken(adminUserId, "ADMIN");
        seedCoreAdminUser(adminUserId);
    }

    @Test
    void courseCreatedInCore_isMirroredAsTagInComunicados() throws Exception {
        String courseName = "E2E Curso " + UUID.randomUUID();
        String courseCode = "E2E-" + UUID.randomUUID().toString().substring(0, 8);

        createCourseInCore(courseName, courseCode);

        JsonNode tag = pollForTagByName(courseName);

        assertThat(tag).as("tag espelhada do curso '%s' em GET /api/tags", courseName).isNotNull();
        assertThat(tag.get("entityType").asText()).isEqualTo("COURSE");
        assertThat(tag.get("active").asBoolean()).isTrue();
    }

    @Test
    void classCreatedInCore_isMirroredAsTagInComunicados() throws Exception {
        String courseCode = "E2E" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        UUID courseId = createCourseInCore("Curso para turma E2E", courseCode);
        createClassInCore(courseId, "FULL_AM_PM", 1);

        String expectedClassName = courseCode + "1";
        JsonNode tag = pollForTagByName(expectedClassName);

        assertThat(tag).as("tag espelhada da turma '%s' em GET /api/tags", expectedClassName).isNotNull();
        assertThat(tag.get("entityType").asText()).isEqualTo("CLASS");
        assertThat(tag.get("active").asBoolean()).isTrue();
    }

    @Test
    void announcementPublishedInComunicados_reachesCoreAsNotification() throws Exception {
        String title = "E2E Comunicado " + UUID.randomUUID();

        String announcementId = publishAnnouncementInComunicados(title);

        NotificationRow row = pollForNotificationByCorrelationId(announcementId);

        assertThat(row).as("linha em notifications (Core) com correlation_id=%s", announcementId).isNotNull();
        assertThat(row.source()).isEqualTo("comunicados-api");
        assertThat(row.eventType()).isEqualTo("announcement.published");
        assertThat(row.title()).isEqualTo(title);
        assertThat(dlqDepth()).as("mensagens em notifications.dispatch.dlq").isZero();
    }

    // ---- setup helpers ----

    private static void assumeStackIsUp() {
        boolean coreUp = isReachable(CORE_URL + "/actuator/health/readiness");
        boolean comunicadosUp = isReachable(COMUNICADOS_URL + "/actuator/health/readiness");
        assumeTrue(coreUp && comunicadosUp,
                "Stack core+comunicados indisponível em " + CORE_URL + " / " + COMUNICADOS_URL
                        + " — suba via docker compose (ver javadoc da classe) antes de rodar este teste.");
    }

    private static boolean isReachable(String healthUrl) {
        try {
            HttpResponse<String> response = HTTP.send(
                    HttpRequest.newBuilder(URI.create(healthUrl)).GET().build(),
                    BodyHandlers.ofString()
            );
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static String mintToken(UUID userId, String userType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("userType", userType)
                .claim("classes", java.util.List.of())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .signWith(key)
                .compact();
    }

    private static void seedCoreAdminUser(UUID userId) throws Exception {
        String sql = """
                INSERT INTO users (id, name, email, password_hash, active, type_user, created_at, updated_at)
                VALUES (?, 'E2E Admin', ?, 'e2e-not-a-real-hash', true, 'ADMIN', now(), now())
                ON CONFLICT (id) DO NOTHING
                """;
        try (Connection connection = coreDbConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setString(2, "e2e-" + userId + "@portalconecta.local");
            statement.executeUpdate();
        }
    }

    // ---- Core actions ----

    private static UUID createCourseInCore(String name, String code) throws Exception {
        String body = JSON.createObjectNode()
                .put("name", name)
                .put("code", code)
                .toString();

        HttpResponse<String> response = postJson(CORE_URL + "/courses", body, adminToken);
        assertThat(response.statusCode()).as("POST /courses -> %s", response.body()).isEqualTo(201);

        return UUID.fromString(JSON.readTree(response.body()).get("id").asText());
    }

    private static UUID createClassInCore(UUID courseId, String shift, int number) throws Exception {
        String body = JSON.createObjectNode()
                .put("shift", shift)
                .put("courseId", courseId.toString())
                .put("number", number)
                .toString();

        HttpResponse<String> response = postJson(CORE_URL + "/classes", body, adminToken);
        assertThat(response.statusCode()).as("POST /classes -> %s", response.body()).isEqualTo(201);

        return UUID.fromString(JSON.readTree(response.body()).get("id").asText());
    }

    private static long dlqDepth() throws Exception {
        String url = RABBITMQ_MGMT_URL + "/api/queues/%2F/notifications.dispatch.dlq";
        String credentials = Base64.getEncoder().encodeToString(
                (RABBITMQ_USER + ":" + RABBITMQ_PASSWORD).getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Basic " + credentials)
                        .GET()
                        .build(),
                BodyHandlers.ofString()
        );
        assertThat(response.statusCode()).as("GET %s -> %s", url, response.body()).isEqualTo(200);

        return JSON.readTree(response.body()).get("messages").asLong();
    }

    // ---- Comunicados actions ----

    private static String publishAnnouncementInComunicados(String title) throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "Corpo gerado pelo teste E2E de RabbitMQ.",
                  "origin": "BOTH",
                  "destinations": [{"type": "GENERAL"}]
                }
                """.formatted(title);

        HttpResponse<String> response = postJson(COMUNICADOS_URL + "/api/posts/publish", body, adminToken);
        assertThat(response.statusCode()).as("POST /api/posts/publish -> %s", response.body()).isEqualTo(201);

        return JSON.readTree(response.body()).get("id").asText();
    }

    private static JsonNode pollForTagByName(String name) throws Exception {
        return pollUntil(
                () -> {
                    try {
                        HttpResponse<String> response = HTTP.send(
                                HttpRequest.newBuilder(URI.create(COMUNICADOS_URL + "/api/tags"))
                                        .header("Authorization", "Bearer " + adminToken)
                                        .GET()
                                        .build(),
                                BodyHandlers.ofString()
                        );
                        if (response.statusCode() != 200) {
                            return null;
                        }
                        for (JsonNode tag : JSON.readTree(response.body())) {
                            if (name.equals(tag.get("name").asText())) {
                                return tag;
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                },
                java.util.Objects::nonNull
        );
    }

    private record NotificationRow(String source, String eventType, String title) {
    }

    private static NotificationRow pollForNotificationByCorrelationId(String correlationId) throws Exception {
        return pollUntil(
                () -> {
                    String sql = "SELECT source, event_type, title FROM notifications WHERE correlation_id = ?";
                    try (Connection connection = coreDbConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, correlationId);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            if (!resultSet.next()) {
                                return null;
                            }
                            return new NotificationRow(
                                    resultSet.getString("source"),
                                    resultSet.getString("event_type"),
                                    resultSet.getString("title")
                            );
                        }
                    } catch (Exception e) {
                        return null;
                    }
                },
                java.util.Objects::nonNull
        );
    }

    // ---- low-level helpers ----

    private static HttpResponse<String> postJson(String url, String body, String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .POST(BodyPublishers.ofString(body))
                        .build(),
                BodyHandlers.ofString()
        );
    }

    private static Connection coreDbConnection() throws Exception {
        return DriverManager.getConnection(CORE_DB_URL, CORE_DB_USER, CORE_DB_PASSWORD);
    }

    private static <T> T pollUntil(Supplier<T> attempt, Predicate<T> condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(POLL_TIMEOUT);
        T last = null;
        while (Instant.now().isBefore(deadline)) {
            last = attempt.get();
            if (condition.test(last)) {
                return last;
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        return last;
    }
}
