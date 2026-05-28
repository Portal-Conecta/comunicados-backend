## Descrição

### Contexto

Implementar os **Controllers REST** do **módulo de Comunicados**, expondo os use cases (#13) via HTTP, com validação (`@Valid`), mapeamento de DTOs (#12) e documentação OpenAPI.

Esta issue cobre **somente a camada web** (controllers, exception handlers do módulo, DTOs de API se necessário). Segurança global (JWT/filtros) assume integração com o Hub conforme padrão do projeto.

**Dependências:** #12 DTOs, #13 use cases, #14 repositories/adapters (para fluxo E2E).

### Objetivo

- Endpoints REST alinhados à API acordada (`/api/posts`, imagens, histórico, tags, avisos)
- Delegação aos use cases (sem lógica de negócio no controller)
- Respostas HTTP consistentes (201, 204, 404, 403)
- OpenAPI/Swagger documentado

**Pacote:** `com.portal.conecta.comunicados.infrastructure.web.controller`

### Escopo — Controllers e rotas

#### `AnnouncementController` — `/api/posts`

| Método | Rota | Use case |
|--------|------|----------|
| GET | `/api/posts` | `ListAnnouncementsUseCase` |
| GET | `/api/posts/{id}` | `GetAnnouncementByIdUseCase` |
| POST | `/api/posts` | `CreateAnnouncementUseCase` |
| PUT/PATCH | `/api/posts/{id}` | `UpdateAnnouncementUseCase` |
| POST | `/api/posts/{id}/publish` | `PublishAnnouncementUseCase` |
| POST | `/api/posts/{id}/schedule` | `ScheduleAnnouncementUseCase` |
| DELETE | `/api/posts/{id}` | `RemoveAnnouncementUseCase` |
| POST | `/api/posts/{id}/pin` | `PinAnnouncementUseCase` |
| DELETE | `/api/posts/{id}/pin` | `UnpinAnnouncementUseCase` |
| GET | `/api/posts/pinned` | `ListPinnedAnnouncementsUseCase` |

**Query params listagem:** `origin`, escopo (WEG/SENAI/TODOS/TURMA/UC), `page`, `size`, ordenação cronológica.

#### `AnnouncementDestinationController`

| Método | Rota |
|--------|------|
| GET/POST/DELETE | `/api/posts/{postId}/destinations` |

#### `AnnouncementFileController` — `/api/posts/{postId}/images`

| Método | Rota |
|--------|------|
| GET/POST/DELETE | arquivos e thumbnail |
| PATCH | definir thumbnail |

RN-COM-IMG01 (máx. 5 imagens).

#### `AnnouncementHistoryController` — `/api/posts/{postId}/history`

| Método | Rota |
|--------|------|
| GET | histórico de alterações |

#### `TagController` / `AnnouncementTagController`

| Método | Rota |
|--------|------|
| CRUD | `/api/tags` |
| GET/POST/DELETE | `/api/posts/{postId}/tags` |

#### `AnnouncementMentionController`

| Método | Rota |
|--------|------|
| GET/POST | `/api/posts/{postId}/mentions` |

#### Avisos individuais

| Controller | Rotas |
|------------|-------|
| `IndividualNoticeCategoryController` | admin categorias |
| `AnnouncementIndividualNoticeController` | CRUD + resolve avisos |

### Tratamento de erros

- `403` — `HubPermissionPort` negou ação
- `404` — recurso inexistente ou removido (soft delete)
- `400` — Bean Validation / regra de negócio
- `HubIntegrationException` → 502/503 conforme padrão do projeto

### Critérios de aceite

- [ ] Todos os endpoints implementados e delegando a use cases
- [ ] `@Valid` nos bodies de escrita
- [ ] OpenAPI gerado/atualizado
- [ ] Testes de controller (`@WebMvcTest` ou integração) nos fluxos principais
- [ ] Build compila

### Referências

- [Funcionalidades](https://www.notion.so/Funcionalidades-36041eabebf8807da267d23feb2a65d8)
- [Requisitos e RN](https://www.notion.so/Requisitos-e-RN-36141eabebf880dda4cbf40257d070d4)
- [Permissões definidas](https://www.notion.so/Permiss-es-Definidas-36541eabebf8803582fdf3816be49d04)

---

## Sub-issues

- [ ] #55 — #15.1 Setup controllers e exception handler
- [ ] #56 — #15.2 AnnouncementController CRUD e listagem
- [ ] #57 — #15.3 AnnouncementController publish, schedule, pin
- [ ] #58 — #15.4 AnnouncementDestinationController
- [ ] #59 — #15.5 AnnouncementFileController
- [ ] #60 — #15.6 AnnouncementHistoryController
- [ ] #61 — #15.7 TagController e AnnouncementTagController
- [ ] #62 — #15.8 AnnouncementMentionController
- [ ] #63 — #15.9 Controllers avisos individuais e OpenAPI
