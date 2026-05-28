## Descrição

### Contexto

Implementar os **DTOs** (request/response) e **Mappers** do **módulo de Comunicados**, alinhados às entidades JPA (#11) e à especificação (§5 — Tabelas do Módulo de Comunicados).

Validar entradas com **Jakarta Bean Validation** (`@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Future`, etc.) nos DTOs de escrita.

Esta issue cobre **somente DTOs, mappers e validações**. Controllers, use cases, repositories e integração Hub/S3 ficam em outras issues.

**Dependência:** #11 (entidades) concluída ou com contratos estáveis.

### Objetivo

- Separação **Request** (create/update) e **Response** (leitura)
- Mapeamento **Entity ↔ DTO** (MapStruct ou padrão do projeto)
- Bean Validation nos DTOs de entrada
- Não expor entidades JPA na API

### Observações

- DTOs em **camelCase**; mesmos nomes das entidades
- FKs Hub como `UUID` nos DTOs
- Enums do domínio nos DTOs (`AnnouncementOrigin`, etc.)

```java
public record CreateAnnouncementRequest(
    @NotBlank @Size(max = 255) String title,
    @NotNull AnnouncementOrigin origin,
    @NotNull AnnouncementStatus status
) {}
```

### Escopo — DTOs por domínio

#### 1. `Announcement`

| DTO | Uso | Campos principais |
|-----|-----|-------------------|
| `CreateAnnouncementRequest` | POST | `title`, `description`, `origin`, `status`, `pinned`, `pinnedOrder`, `scheduledFor` |
| `UpdateAnnouncementRequest` | PUT/PATCH | campos editáveis conforme RN |
| `PublishAnnouncementRequest` | ação | `publishedByUserId` (se não vier do token) |
| `AnnouncementResponse` | GET | campos completos + auditoria |
| `AnnouncementSummaryResponse` | listagem | subconjunto para feed |

**Validações:** `title` `@NotBlank`; `origin`, `status` `@NotNull`; `pinnedOrder` `@Min(0)` quando fixado.

#### 2. `AnnouncementDestination`

| DTO | Uso |
|-----|-----|
| `CreateAnnouncementDestinationRequest` | POST |
| `AnnouncementDestinationResponse` | GET |

`referenceId` obrigatório exceto quando `type = GENERAL`.

#### 3. `AnnouncementFile`

| DTO | Uso |
|-----|-----|
| `CreateAnnouncementFileRequest` | POST metadados |
| `AnnouncementFileResponse` | GET |

RN-COM-IMG01.

#### 4. `AnnouncementHistory`

| DTO | Uso |
|-----|-----|
| `CreateAnnouncementHistoryRequest` | interno |
| `AnnouncementHistoryResponse` | GET |

#### 5. `Tag` / `AnnouncementTag`

| DTO | Uso |
|-----|-----|
| `CreateTagRequest`, `UpdateTagRequest`, `TagResponse` | CRUD tag |
| `LinkAnnouncementTagRequest` | POST batch |
| `AnnouncementTagResponse` | GET |

#### 6. `AnnouncementMention`

| DTO | Uso |
|-----|-----|
| `CreateAnnouncementMentionRequest` | POST |
| `AnnouncementMentionResponse` | GET |

#### 7. Avisos individuais

| DTO | Uso |
|-----|-----|
| `CreateIndividualNoticeCategoryRequest` | admin |
| `CreateAnnouncementIndividualNoticeRequest` | POST |
| `ResolveAnnouncementIndividualNoticeRequest` | PATCH |
| `AnnouncementIndividualNoticeResponse` | GET |

#### 8. DTOs compostos

| DTO | Descrição |
|-----|-----------|
| `AnnouncementDetailResponse` | post + destinos + arquivos + tags |
| `PostFilterRequest` | filtros listagem (origin, WEG/SENAI/TODOS/TURMA/UC, page, size) |

### Mappers

| Mapper | Responsabilidade |
|--------|------------------|
| `AnnouncementMapper` | Entity ↔ DTOs Announcement |
| `AnnouncementDestinationMapper` | idem |
| `AnnouncementFileMapper` | idem |
| `AnnouncementHistoryMapper` | idem |
| `TagMapper`, `AnnouncementTagMapper` | idem |
| `AnnouncementMentionMapper` | idem |
| Mappers avisos individuais | idem |

**Pacote:** `com.portal.conecta.comunicados.dto` e `.mapper`

### Critérios de aceite

- [ ] DTOs Request/Response para todas as entidades
- [ ] Bean Validation em todos os requests de escrita
- [ ] Mappers compilando (MapStruct se utilizado)
- [ ] Build compila

### Referências

- [Modelagem do BD](https://www.notion.so/Modelagem-do-Banco-de-Dados-36441eabebf8809f9cb5ffb740263700)
- [Requisitos e RN](https://www.notion.so/Requisitos-e-RN-36141eabebf880dda4cbf40257d070d4)
- [Funcionalidades](https://www.notion.so/Funcionalidades-36041eabebf8807da267d23feb2a65d8)

---

## Sub-issues

- [ ] #27 — #12.1 Setup DTOs e Bean Validation
- [ ] #28 — #12.2 DTOs e mapper Announcement
- [ ] #29 — #12.3 DTOs e mapper AnnouncementDestination
- [ ] #30 — #12.4 DTOs e mapper AnnouncementFile
- [ ] #31 — #12.5 DTOs e mapper AnnouncementHistory
- [ ] #32 — #12.6 DTOs e mapper Tag e AnnouncementTag
- [ ] #33 — #12.7 DTOs e mapper AnnouncementMention
- [ ] #34 — #12.8 DTOs e mapper avisos individuais
- [ ] #35 — #12.9 DTOs compostos e filtros de listagem
