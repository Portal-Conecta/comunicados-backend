# Contexto para agentes de IA — comunicados-backend

Guia de referência para quem implementa, revisa ou debuga este repositório com assistentes de IA. Leia antes de alterar código.

**Stack:** Java 21 · Spring Boot 4.0.6 · Maven · JPA/H2 (dev) / PostgreSQL (prod) · JWT · RabbitMQ (opcional) · springdoc OpenAPI
---

## 1. Visão geral do produto

Backend do módulo **Comunicados** do Portal Conecta. Gerencia **postagens/comunicados** (`/api/posts`) e **tags** (`/api/tags`).

- Comunicados **não** gerencia turmas/cursos/usuários — consulta o **Hub** (HTTP) para escopo e permissões.
- Tags **não** são criadas manualmente via REST — são espelhadas do **Core** via **RabbitMQ** (TAG02).
- API exposta como **"Postagens"** com path `/api/posts` (não `/announcements`).

Issues no GitHub com label `squad: comunicados`. Consultar com `gh issue view <número>`.

---

## 2. Estrutura de pastas

```text
comunicados-backend/
├── src/main/java/com/portal/conecta/comunicados/
│   ├── ComunicadosApplication.java          # entry point único
│   ├── module/
│   │   ├── comunicado/                      # domínio de postagens (~90 arquivos)
│   │   │   ├── presentation/                # controllers + DTOs HTTP
│   │   │   ├── application/                 # usecase/, command/, query/
│   │   │   ├── domain/                      # model/, enums/, exception/, validator/, port/, specification/
│   │   │   └── infrastructure/hub/          # adapters HTTP/Mock do Hub
│   │   └── tag/                             # tags + mensageria Core
│   │       ├── presentation/
│   │       ├── application/
│   │       ├── domain/
│   │       └── infrastructure/messaging/    # RabbitMQ (consumer, config, DTOs de evento)
│   └── shared/                              # cross-cutting
│       ├── context/                         # RequestContext, UserType, ClassRole
│       ├── security/                        # JWT, SecurityConfig, filters
│       ├── exception/                       # GlobalExceptionHandler, ApiError
│       └── config/                          # OpenApiConfig
├── src/main/resources/
│   ├── application.yaml                     # defaults (profile dev)
│   ├── application-dev.yaml
│   ├── application-prod.yaml
│   ├── application-test.yaml
│   └── db/migration/                        # Flyway (desabilitado por padrão)
├── src/test/java/                           # testes unitários + WebMvcTest + flow tests
├── docs/                                    # guias (swagger, eventos Core)
├── CONTRIBUTING.md                          # GitFlow, commits, PRs
└── .env.example
```

**Não existe** camada `infrastructure/persistence` separada — repositórios JPA ficam em `domain/port/`.

---

## 3. Arquitetura e fluxo de uma feature

Padrão **hexagonal leve** por módulo de feature:

```text
Controller → Command/Query → UseCase → Port (Repository/HubPort) → Adapter (só Hub e RabbitMQ)
                ↓
         Response DTO.fromEntity()
```

### Convenções de naming

| Tipo | Sufixo / padrão | Localização | Exemplo |
|------|-----------------|-------------|---------|
| Use case | `*UseCase`, método `execute()` | `application/usecase/` | `ListAnnouncementsUseCase` |
| Command (escrita) | `*Command` (record) | `application/command/` | `ScheduleAnnouncementCommand` |
| Query (leitura) | `*Query` (record) | `application/query/` | `GetAnnouncementByIdQuery` |
| Port Hub | `*Port` | `domain/port/` | `HubClassPort` |
| Port persistência | `*Repository` extends `JpaRepository` | `domain/port/` | `AnnouncementRepository` |
| Adapter Hub | `Http*Adapter` / `Mock*Adapter` | `infrastructure/hub/adapter/` | `HttpHubClassAdapter` |
| Request DTO | `*Request` (record + Bean Validation) | `presentation/dto/request/` | `ScheduleAnnouncementRequest` |
| Response DTO | `*Response` (record) | `presentation/dto/response/` | `TagResponse` |
| Controller | `*Controller` | `presentation/controller/` | `AnnouncementController` |
| Exceção domínio | `*Exception` | `domain/exception/` | `TagNotFoundException` |

### Factories em Commands/DTOs

```java
// Request → Command (no controller)
ScheduleAnnouncementCommand.fromRequest(id, request);

// Command → Entity (no use case, com Instant.now())
command.toEntity(existing, now);

// Entity → Response (no controller)
TagResponse.fromEntity(tag);
ListAnnouncementsResponse.fromPinnedAndPage(pinned, page);
```

**Consumer RabbitMQ:** fino — `Envelope → Command.from(envelope) → UseCase.execute(command)`. Sem lógica de negócio no `@RabbitListener`.

---

## 4. Mapeamento de objetos — SEM MapStruct

- Mapeamento **100% manual** via métodos estáticos em records: `fromEntity`, `fromRequest`, `toEntity`, `applyTo`.
- `mapstruct` não está mais no `pom.xml` (removido — nenhum `@Mapper` era usado).
- **Não introduzir MapStruct** sem alinhamento explícito — siga o padrão existente.

---

## 5. Datas e timestamps

- **Usar exclusivamente `java.time.Instant`** em domínio, DTOs, commands e eventos.
- **Não usar** `LocalDateTime`, `ZonedDateTime` ou `OffsetDateTime`.
- Timestamps gerados com `Instant.now()` nos use cases no momento da operação.
- JSON na API: ISO-8601 UTC (`"2026-06-12T10:00:00Z"`).
- H2 dev simula `TIMESTAMPTZ` via domain SQL no datasource URL.

Campos comuns: `createdAt`, `updatedAt`, `publishedAt`, `scheduledFor`, `removedAt`.

---

## 6. Persistência (JPA)

### Entidades

- Pacote: `domain/model/`
- Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- ID: `@GeneratedValue(strategy = GenerationType.UUID)`
- Enums: `@Enumerated(EnumType.STRING)`
- Soft delete comunicados: campo `removedAt` (não usar status `REMOVED` sozinho para queries — filtrar `removedAt IS NULL`)

### Repositories

- Interfaces Spring Data em `domain/port/` (não em infrastructure).
- Queries derivadas: `findByIdAndRemovedAtIsNull`, `findByEntityTypeAndHubEntityId`, etc.
- Specifications: `AnnouncementSpecifications` para filtros de listagem.

### Profiles e schema

| Profile | DB | ddl-auto | Flyway | Messaging |
|---------|-----|----------|--------|-----------|
| `dev` (default) | H2 in-memory | `update` | off | `MESSAGING_ENABLED=false` (default) |
| `test` | H2 | `create-drop` | off | off + Rabbit auto-config excluído |
| `prod` | PostgreSQL | `validate` | **on** (`db/migration`) | via env |

- `spring.jpa.open-in-view: false` — inicializar coleções lazy explicitamente nos use cases quando necessário.
- Migrations em `db/migration/` quando Flyway for habilitado (`V###__descricao.sql`).

---

## 7. Segurança e contexto do usuário

### JWT Bearer

1. `JwtAuthenticationFilter` lê `Authorization: Bearer <token>`
2. `JwtExtractToken` valida e monta `RequestContext`
3. Principal no `SecurityContext` = objeto `RequestContext` (não username string)

### Claims esperados no token

| Claim | Tipo | Uso |
|-------|------|-----|
| `sub` | UUID | `userId` |
| `userType` | string | enum `UserType` |
| `classes` | array | `{ classId, role }` → `List<ContextClass>` |

### Obter usuário no controller/use case

```java
RequestContext ctx = contextProvider.getRequestContext();
UUID userId = ctx.userId();
UserType type = ctx.userType();
```

### Regra anti-vazamento

Recursos fora do escopo do usuário retornam **404**, não 403 (ex.: `TagPermissionDeniedException` → 404 no handler).

### Endpoints públicos

`/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/health`, `/actuator/info` — demais rotas exigem autenticação.

---

## 8. Tratamento de erros

**Handler:** `shared/exception/GlobalExceptionHandler.java`  
**Resposta:** `ApiError` record (`timestamp: Instant`, `status`, `error`, `message`, `path`, `errors[]`)

| Exceção | HTTP |
|---------|------|
| `UnauthorizedUserException` | 401 |
| `AnnouncementPermissionDeniedException` | 403 |
| `AnnouncementNotFoundException`, `TagNotFoundException` | 404 |
| `TagPermissionDeniedException` | 404 (proposital) |
| `MethodArgumentNotValidException` / `BindException` | 400 + field errors |
| `ResponseStatusException` | status do exception |
| `DataIntegrityViolationException` (uk_*) | 409 |
| `HttpRequestMethodNotSupportedException` | 405 |

Mensagens de negócio em **português**. Use cases de comunicado lançam exceções de domínio (`Announcement*Exception`); mutações sem permissão usam `AnnouncementNotFoundException` (404) para anti-vazamento; create (publish/schedule) e pin ainda usam `AnnouncementPermissionDeniedException` (403).

---

## 9. Integração Hub (HTTP)

Comunicados consulta o Hub para cursos, turmas, alunos e permissões.

```text
domain/port/*Port  →  infrastructure/hub/adapter/Http*Adapter | Mock*Adapter
```

- Toggle: `hub.api.mock-enabled` (default **`true`** em dev — não precisa do Hub real).
- HTTP adapters usam `RestClient` com `HubAuthForwardingInterceptor` (repassa `Authorization` da request atual).
- Degradação graciosa: vários adapters retornam lista vazia em falha (ex.: `HubCourseHttpAdapter`).
- Config: `hub.api.url`, `hub.mock.*` em `application.yaml`.

**Referência:** `module/comunicado/infrastructure/hub/`

---

## 10. Mensageria RabbitMQ (tags)

Sincroniza entidades do Core como tags locais. Contrato completo: [`docs/tags-por-eventos.md`](./docs/tags-por-eventos.md) (oficial).

### Feature flag

```yaml
app.messaging.enabled: ${MESSAGING_ENABLED:false}  # default OFF — app sobe sem broker
```

Quando `true`, exige RabbitMQ em `localhost:5672` (ou `RABBITMQ_*` no `.env`).

### Infraestrutura

Dois exchanges topic separados (course e class), ambos roteados para a mesma queue. Config em `RabbitMqConfig.java` + `application.yaml` (`app.messaging.core.*`) — fonte de verdade, não os `.md` (histórico de drift: até `docs/tags-por-eventos.md` já divergiu do código real; sempre conferir contra o código antes de confiar num doc).

| Recurso | Nome |
|---------|------|
| Exchange curso (topic) | `course-events.exchange` |
| Exchange turma (topic) | `class-events.exchange` |
| Queue | `comunicados.core-entities` |
| DLQ | `comunicados.core-entities.dlq` |
| Routing keys curso | `course.created`, `course.updated`, `course.deleted` |
| Routing keys turma | `class.created`, `class.deleted` (**não existe** `class.updated`) |

### Fluxo

```text
CoreEntityTagConsumer (@RabbitListener)
  → idempotência (ProcessedEventRepository por eventId)
  → course.created | course.updated | class.created → UpsertTagFromCoreUseCase
  → course.deleted  | class.deleted                 → DeactivateTagUseCase
```

- `payload.entityId` = `tag.hub_entity_id` (mesmo UUID de `announcement_destination.reference_id` para auto-vinculação #110).
- Payload inválido ou `eventType` não suportado → `InvalidCoreEntityEventException` → DLQ (`defaultRequeueRejected=false`).
- Beans de messaging: `@ConditionalOnProperty(app.messaging.enabled=true)`.
- Bindings dinâmicos (`classEntityBindings`/`courseEntityBindings` em `RabbitMqConfig.java`) devem retornar `Declarables`, não `Binding[]`/`List<Binding>` — um array/lista simples não é auto-declarado pelo `RabbitAdmin` no broker (bug real já visto em produção local: exchange e queue existiam, mas sem nenhum binding real, mensagens eram descartadas silenciosamente pelo RabbitMQ).

**Referência:** `module/tag/infrastructure/messaging/`

---

## 11. Domínio — enums e conceitos

### Status do comunicado

`AnnouncementStatus`: `SCHEDULED` → `PUBLISHED` | `REMOVED`  
**Não existe mais `DRAFT`** — criação será atômica via `POST /publish` (#107) e `POST /schedule` (#108).

### Destinos

`AnnouncementDestinationType`: `GENERAL`, `COURSE`, `CLASS`, `USER`  
`referenceId` aponta para entidade no Hub; alinha com `tag.hub_entity_id`.

### Tags

`TagEntityType`: `COURSE`, `CLASS`, `USER`  
Chave natural: `(entity_type, hub_entity_id)`. Campo `active` para desativação via evento.

### Perfis

`UserType`: `STUDENT`, `REPRESENTATIVE`, `TEACHER`, `SENAI`, `WEG`, `ADMIN`  
`ClassRole`: `STUDENT`, `TEACHER`, `REPRESENTATIVE` (dentro de `ContextClass`)

### Permissões centralizadas

`AnnouncementPermissionValidator` — create/publish/schedule/delete/view. Reutilizar em novos fluxos unificados.

---

## 12. Endpoints — estado atual

### Comunicados (`AnnouncementController` — `/api/posts`)

| Método | Rota | Status |
|--------|------|--------|
| GET | `/api/posts` | Implementado (listagem com `pinned` + `items` paginado, filtros e busca textual) |
| GET | `/api/posts/{id}` | Implementado (detalhe com regra de escopo) |
| GET | `/api/posts/{id}/history` | Implementado (histórico paginado do comunicado) |
| PUT | `/api/posts/{id}` | Implementado (atualização parcial — campos ausentes/`destinations` omitido são preservados) |
| DELETE | `/api/posts/{id}` | Implementado (soft delete) |
| POST | `/api/posts/publish` | Implementado (cria + publica) |
| POST | `/api/posts/schedule` | Implementado (cria + agenda) |
| PATCH | `/api/posts/{id}/schedule` | Implementado (reagenda comunicado agendado) |
| PATCH | `/api/posts/{id}/pin` | Implementado (fixa comunicado) |
| PATCH | `/api/posts/{id}/unpin` | Implementado (desafixa comunicado) |
| PATCH | `/api/posts/{id}` | **Removido** (#137 won't-do) — edição parcial é coberta pelo `PUT /api/posts/{id}` |
| PATCH | `/api/posts/{id}/cancel-schedule` | **Não existe** — cancelamento de agendamento ainda não implementado |
| GET | `/api/posts/pinned` | Implementado (redundante para o mural — preferir `pinned` em `GET /api/posts`) |
| POST | `/api/posts` | **Removido** — retorna 405; usar `POST /publish` ou `POST /schedule` |

### Imagens e arquivos (`PostImageController` — `/api/posts/{postId}/images`)

| Método | Rota | Status |
|--------|------|--------|
| GET | `/api/posts/{postId}/images` | Implementado (lista arquivos do comunicado) |
| POST | `/api/posts/{postId}/images` | Implementado (upload multipart) |
| DELETE | `/api/posts/{postId}/images/{imageId}` | Implementado (remove arquivo) |
| PATCH | `/api/posts/{postId}/images/{imageId}/thumbnail` | Implementado (define miniatura) |

### Tags (`TagController`)

| Método | Rota | Status |
|--------|------|--------|
| GET | `/api/tags` | Implementado |
| GET | `/api/posts/{id}/tags` | Implementado |
| POST | `/api/posts/{id}/tags` | Implementado (vincular) |
| DELETE | `/api/posts/{id}/tags/{tagId}` | Implementado |
| POST | `/api/tags` | **Não existe** (TAG02 — tags vêm do Core/RabbitMQ) |

---

## 13. OpenAPI / Swagger

- Lib: `springdoc-openapi-starter-webmvc-ui`
- UI: `/swagger-ui.html` · Spec: `/v3/api-docs`
- Config: `shared/config/OpenApiConfig.java` — scheme `bearerAuth`
- Guia: `docs/swagger-documentation-guide.md`
- Anotar controllers com `@Tag`, `@Operation`, `@ApiResponses`
- Documentar **contrato HTTP**, não implementação interna (repositories, SQL, use cases)
- Javadoc Maven desabilitado — documentação via OpenAPI apenas

---

## 14. Testes

Rodar: `./mvnw test` (profile `test` automático).

### Três estilos

1. **Unitário puro** — `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`
2. **WebMvcTest** — `@WebMvcTest(controllers=...)`, `@MockitoBean`, `@AutoConfigureMockMvc(addFilters=false)`, excluir `SecurityConfig`/`JwtAuthenticationFilter`
3. **Flow test** — `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build()` com use case real parcialmente mockado

### Convenções

- Assertions: AssertJ (`assertThat`) + MockMvc `jsonPath`
- Mensagens de erro em português nos testes
- Messaging desligado nos testes — não exige RabbitMQ
- Smoke: `ComunicadosApplicationTests` com `@SpringBootTest` + `@ActiveProfiles("test")`

---

## 15. Git, commits e PRs

Ver `CONTRIBUTING.md`. Resumo:

- **GitFlow:** nunca commit direto em `main`/`develop`
- Branches: `feature/<issue>-descricao-curta` (de `develop`)
- Commits: Conventional Commits em **português imperativo** (`feat:`, `fix:`, `refactor:`, `test:`, `chore:`)
- PR: template em CONTRIBUTING.md, `Closes #<issue>`, review Tech Lead + squad comunicados
- CI: `.github/workflows/ci.yml` — `mvn clean test` (JDK 21)

---

## 16. O que NÃO fazer

| Regra | Motivo |
|-------|--------|
| `POST /api/tags` | TAG02 — tags só via RabbitMQ |
| `POST /api/posts` (criar rascunho) | Removido (#106) — usar fluxos unificados #107/#108 |
| MapStruct / classes `*Mapper` | Projeto mapeia manualmente |
| Lógica de negócio no `@RabbitListener` | Delegar para use case via command |
| 403 quando recurso está fora de escopo | Usar 404 para não vazar existência |
| Commit em `main`/`develop` | GitFlow estrito |
| Dependências novas sem alinhamento | Checklist do PR |
| Assumir Flyway ativo | Default `enabled: false`; schema via `ddl-auto` em dev |
| Habilitar messaging sem RabbitMQ rodando | `Connection refused :5672` — usar `MESSAGING_ENABLED=false` ou subir broker |

---

## 17. Referências rápidas (arquivos-chave)

| Tópico | Caminho |
|--------|---------|
| Controller posts | `module/comunicado/presentation/controller/AnnouncementController.java` |
| Controller tags | `module/tag/presentation/controller/TagController.java` |
| Permissões | `module/comunicado/domain/validator/AnnouncementPermissionValidator.java` |
| Exception handler | `shared/exception/GlobalExceptionHandler.java` |
| JWT / contexto | `shared/security/`, `shared/context/RequestContext.java` |
| Hub adapters | `module/comunicado/infrastructure/hub/` |
| Rabbit consumer | `module/tag/infrastructure/messaging/consumer/CoreEntityTagConsumer.java` |
| Contrato eventos Core (oficial) | `docs/tags-por-eventos.md` |
| Contrato eventos Core (depreciado) | `docs/core-entity-events.md` |
| Guia Swagger | `docs/swagger-documentation-guide.md` |
| Contribuição | `CONTRIBUTING.md` |

---

## 18. Checklist antes de entregar código

- [ ] Seguiu naming (`UseCase`, `Command`, `fromEntity`, sem MapStruct)?
- [ ] Usou `Instant` para datas?
- [ ] Controller fino — lógica no use case?
- [ ] Exceções de domínio com mensagem em português?
- [ ] Testes passam com `./mvnw test`?
- [ ] OpenAPI atualizado se mudou contrato HTTP?
- [ ] Não criou endpoint proibido (POST tags, POST posts)?
- [ ] Escopo mínimo — sem refatoração não solicitada?
