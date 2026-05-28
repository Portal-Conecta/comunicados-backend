## Descrição

### Contexto

Implementar **repositories** (Spring Data JPA) e **adapters** (Hub e S3) do **módulo de Comunicados**, suportando os use cases (#13).

Esta issue cobre **somente repositories + adapters**. Use cases e controllers em outras issues.

**Dependências:** #11 entidades/migrations, ports definidos em #13.1.

### Objetivo

- Interfaces de repositório + implementação JPA
- Adapters `HubAuthPort`, `HubPermissionPort`, `HubClassPort`, `HubUserPort`, `StoragePort`
- Queries para listagem, soft delete, filtros

**Pacotes:**

- `com.portal.conecta.comunicados.infrastructure.persistence.repository`
- `com.portal.conecta.comunicados.infrastructure.adapter.hub`
- `com.portal.conecta.comunicados.infrastructure.adapter.storage`

### Escopo — Repositories

| Repository | Entidade |
|------------|----------|
| `AnnouncementRepository` | `Announcement` |
| `AnnouncementDestinationRepository` | `AnnouncementDestination` |
| `AnnouncementFileRepository` | `AnnouncementFile` |
| `AnnouncementHistoryRepository` | `AnnouncementHistory` |
| `TagRepository` | `Tag` |
| `AnnouncementTagRepository` | `AnnouncementTag` |
| `AnnouncementMentionRepository` | `AnnouncementMention` |
| `IndividualNoticeCategoryRepository` | `IndividualNoticeCategory` |
| `AnnouncementIndividualNoticeRepository` | `AnnouncementIndividualNotice` |

**Queries sugeridas (`AnnouncementRepository`):**

- `findByIdAndRemovedAtIsNull`
- `findPinnedByRemovedAtIsNullOrderByPinnedOrderAsc`
- Filtros dinâmicos (Specification ou `@Query`)

### Escopo — Adapters Hub

| Port | Adapter |
|------|---------|
| `HubAuthPort` | `HubAuthAdapter` |
| `HubPermissionPort` | `HubPermissionAdapter` |
| `HubClassPort` | `HubClassAdapter` |
| `HubUserPort` | `HubUserAdapter` |

**Matriz em `HubPermissionAdapter`:**

| Perfil | Visualizar | Criar |
|--------|------------|-------|
| `PERFIL_SENAI` / `PERFIL_WEG` | Todos | Sim |
| `DOCENTE` | Associados | Turmas vinculadas |
| `REPRESENTANTE` | Associados | Própria turma |
| `APRENDIZ` | Associados | Não |

- Config: `hub.base-url`, timeouts
- `HubIntegrationException` em falhas HTTP

### Storage (opcional)

| Port | Adapter |
|------|---------|
| `StoragePort` | `S3StorageAdapter` ou stub |

### Configuração e testes

- [ ] Beans `@Component` / `@Configuration`
- [ ] Profile `local`/`test`: adapters fake ou WireMock (#53)
- [ ] (Opcional) `@DataJpaTest` (#54)

### Critérios de aceite

- [ ] Repositories injetáveis e queries de soft delete OK
- [ ] Adapters implementam ports de #13.1
- [ ] Build compila

### Referências

- [Requisitos e RN](https://www.notion.so/Requisitos-e-RN-36141eabebf880dda4cbf40257d070d4)
- [Modelagem do BD](https://www.notion.so/Modelagem-do-Banco-de-Dados-36441eabebf8809f9cb5ffb740263700)
- [Permissões definidas](https://www.notion.so/Permiss-es-Definidas-36541eabebf8803582fdf3816be49d04)

---

## Sub-issues

- [ ] #48 — #14.1 Repositories núcleo
- [ ] #49 — #14.2 Repositories suporte
- [ ] #50 — #14.3 HubAuthAdapter e HubPermissionAdapter
- [ ] #51 — #14.4 HubClassAdapter e HubUserAdapter
- [ ] #52 — #14.5 S3StorageAdapter
- [ ] #53 — #14.6 Adapters fake local/test
- [ ] #54 — #14.7 DataJpaTest repositories
