# Guia de testes via Postman — comunicados-backend

Guia prático para testar manualmente a API com **Postman**, usando o **core-backend** (Hub real em `localhost:8080`) ou **JWT local** (jwt.io) como fallback.

> **Coleções atuais:** `postman/Core - Login.postman_collection.json` + `postman/Comunicados - {PERFIL}.postman_collection.json`. Ver [postman/README.md](../postman/README.md).

---

## Modo recomendado: Core + Postman

1. Suba **Core** (`8080`) e **Comunicados** (`8083`)
2. Importe environment + coleções de `postman/`
3. Rode **Core - Login** (Collection Runner) na ordem das pastas
4. Rode **Comunicados - SENAI** (ou outro perfil)

Admin pré-cadastrado no Core: `admin@portal.test` / `123456`

---

## Modo legado: JWT manual (jwt.io)

O Comunicados **não emite token**. Ele **valida** o JWT que o Hub entregaria ao usuário. Em dev, você pode gerar o token localmente com o mesmo segredo configurado em `app.jwt.secret`.

### 3.1 Claims obrigatórios

| Claim | Tipo | Descrição |
|-------|------|-----------|
| `sub` | string (UUID) | ID do usuário autenticado |
| `userType` | string | Um de: `STUDENT`, `REPRESENTATIVE`, `TEACHER`, `SENAI`, `WEG`, `ADMIN` |
| `classes` | array (opcional) | Turmas do usuário: `{ "classId": "uuid", "role": "TEACHER" \| "REPRESENTATIVE" \| "STUDENT" }` |
| `permissionVersion` | number (opcional) | Versão de permissão (inteiro ≥ 0) |
| `exp` | number | Expiração (Unix timestamp) |

### 3.2 Segredo JWT (profile `dev`)

```
ZGV2LXNlY3JldC1rZXktMzItYnl0ZXMtbWluaW11bS1mb3ItaHMyNTY=
```

(decodifica para: `dev-secret-key-32-bytes-minimum-for-hs256`)

### 3.3 Gerar token no [jwt.io](https://jwt.io)

1. Algorithm: **HS256**
2. Em **VERIFY SIGNATURE**, cole o segredo acima em **base64** (marque “secret is base64 encoded” se disponível).
3. Payload de exemplo — perfil **SENAI** (vê todos os comunicados, pode publicar):

```json
{
  "sub": "11111111-1111-1111-1111-111111111111",
  "userType": "SENAI",
  "classes": [],
  "exp": 1893456000
}
```

4. Payload — perfil **TEACHER** (escopo por turma):

```json
{
  "sub": "22222222-2222-2222-2222-222222222222",
  "userType": "TEACHER",
  "classes": [
    {
      "classId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "role": "TEACHER"
    }
  ],
  "exp": 1893456000
}
```

5. Payload — perfil **STUDENT** (só vê comunicados do seu escopo):

```json
{
  "sub": "33333333-3333-3333-3333-333333333333",
  "userType": "STUDENT",
  "classes": [
    {
      "classId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "role": "STUDENT"
    }
  ],
  "exp": 1893456000
}
```

6. Copie o token gerado.

### 3.4 Configurar no Postman

**Opção A — por request**

- Aba **Authorization** → Type: **Bearer Token** → cole o JWT.

**Opção B — ambiente (recomendado)**

Crie um Environment `Comunicados Local`:

| Variável | Valor inicial |
|----------|---------------|
| `baseUrl` | `http://localhost:8083` |
| `token` | *(cole o JWT gerado)* |
| `userId` | `11111111-1111-1111-1111-111111111111` |
| `classId` | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` |
| `postId` | *(preencher após criar comunicado)* |
| `tagId` | *(preencher após listar tags)* |

Na collection, em **Authorization**:

- Type: **Bearer Token**
- Token: `{{token}}`

Header em todas as rotas autenticadas:

```
Authorization: Bearer {{token}}
```

### 3.5 Respostas de segurança

| Situação | HTTP | Mensagem típica |
|----------|------|-----------------|
| Sem header | 401 | `Authentication is required.` |
| Token inválido/expirado | 401 | `Invalid or expired token` |
| Sem permissão de negócio | 403 | `Access is denied.` ou mensagem do domínio |
| Recurso fora do escopo | 404 | Comunicado/tag “não encontrado” (anti-vazamento) |

---

## 4. Mock da integração Hub (escopo, alunos, busca)

Com `HUB_MOCK_ENABLED=true`, os adapters `Mock*Adapter` substituem chamadas HTTP ao Hub. O JWT continua sendo a fonte de **quem** é o usuário; o mock define **turmas, alunos e vínculos** usados em permissões e na busca por destinatário (#117).

### 4.1 Comportamento com mock vazio (default)

Se `hub.mock.*` estiver vazio em `application.yaml`:

- `existsById` de turma/usuário tende a ser **permissivo** (listas vazias = aceita qualquer UUID).
- Busca por nome de destinatário (`search`) **não encontra usuários** (sem nomes no mock).
- Publicar com destino `GENERAL` funciona para perfis privilegiados (`SENAI`, `WEG`, `ADMIN`).

### 4.2 Mock com dados de turma e alunos (recomendado)

Crie `src/main/resources/application-local.yaml` (não commitar segredos):

```yaml
spring:
  config:
    activate:
      on-profile: local

hub:
  mock:
    class-ids:
      - aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
    students-by-class:
      aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa:
        - id: "44444444-4444-4444-4444-444444444444"
          name: "João da Silva"
        - id: "55555555-5555-5555-5555-555555555555"
          name: "Maria Santos"
    user-types-by-id:
      "44444444-4444-4444-4444-444444444444": "STUDENT"
      "55555555-5555-5555-5555-555555555555": "STUDENT"
```

Suba com:

```bash
SPRING_PROFILES_ACTIVE=dev,local ./mvnw spring-boot:run
```

**Efeitos:**

- Professor com `classId` no JWT pode publicar para destino `CLASS` ou `USER` daquela turma.
- `GET /api/posts?search=joão` encontra comunicados com destinatário `USER` = João (escopo da turma no token).
- `MockHubClassAdapter.getClassIdForUser` resolve aluno → turma.

### 4.3 Hub HTTP real (opcional)

```env
HUB_MOCK_ENABLED=false
HUB_API_URL=http://localhost:8080
```

O adapter HTTP repassa o header `Authorization` da request atual para o Hub. Use um JWT emitido pelo Hub real e garanta que os endpoints existam (`/users/search`, `/me/courses`, etc.).

---

## 5. Perfis e permissões (resumo)

| Perfil | Listar tudo | Publicar/agendar | Editar | Remover |
|--------|-------------|------------------|--------|---------|
| `ADMIN` | Sim | Sim | Sim | Sim (qualquer) |
| `SENAI` / `WEG` | Sim | Sim | Sim | Próprio ou criado por teacher/rep |
| `TEACHER` / `REPRESENTATIVE` | Escopo turma | Destinos da turma no JWT | Só autor | Só autor |
| `STUDENT` | Escopo visibilidade | Não | Não | Não |

**Tags (`POST /api/posts/{id}/tags`):** autor do comunicado ou perfil privilegiado (`SENAI`, `WEG`, `ADMIN`).

---

## 6. Endpoints implementados

### 6.1 Comunicados — `AnnouncementController`

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| `GET` | `/api/posts` | Implementado | Listagem paginada + filtros + `search` |
| `GET` | `/api/posts/{id}` | Implementado | Detalhe por ID (com escopo) |
| `GET` | `/api/posts/{id}/history` | Implementado | Histórico paginado do comunicado |
| `PUT` | `/api/posts/{id}` | Implementado | Atualização completa |
| `DELETE` | `/api/posts/{id}` | Implementado | Remoção (soft delete) |
| `POST` | `/api/posts/publish` | Implementado | Cria + publica (#107) |
| `POST` | `/api/posts/schedule` | Implementado | Cria + agenda (#108) |
| `PATCH` | `/api/posts/{id}/schedule` | Implementado | Reagenda comunicado agendado |
| `PATCH` | `/api/posts/{id}/pin` | Implementado | Fixa comunicado |
| `PATCH` | `/api/posts/{id}/unpin` | Implementado | Desafixa comunicado |

### 6.2 Imagens e arquivos — `PostImageController`

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| `GET` | `/api/posts/{postId}/images` | Implementado | Lista arquivos vinculados ao comunicado |
| `POST` | `/api/posts/{postId}/images` | Implementado | Upload multipart de arquivo |
| `DELETE` | `/api/posts/{postId}/images/{imageId}` | Implementado | Remove arquivo do comunicado |
| `PATCH` | `/api/posts/{postId}/images/{imageId}/thumbnail` | Implementado | Define arquivo como thumbnail |

### 6.3 Tags — `TagController`

| Método | Rota | Status | Descrição |
|--------|------|--------|-----------|
| `GET` | `/api/tags` | Implementado | Lista tags ativas (`?entityType=CLASS` opcional) |
| `GET` | `/api/posts/{id}/tags` | Implementado | Tags vinculadas ao comunicado |
| `POST` | `/api/posts/{id}/tags` | Implementado | Associa tags ao comunicado |
| `DELETE` | `/api/posts/{id}/tags/{tagId}` | Implementado | Remove associação |

> Tags **não** são criadas via `POST /api/tags`. Origem: RabbitMQ (Core) ou insert manual no H2 para testes locais.

### 6.4 Públicos (sem JWT)

| Método | Rota |
|--------|------|
| `GET` | `/swagger-ui.html`, `/swagger-ui/**` |
| `GET` | `/v3/api-docs`, `/v3/api-docs/**` |
| `GET` | `/actuator/health`, `/actuator/info` |

---

## 7. Endpoints ainda NÃO implementados (não testar como feature)

Retornam corpo vazio/`null` ou não possuem regra de negócio — **stubs** para documentação futura:

| Método | Rota | Observação |
|--------|------|------------|
| `PATCH` | `/api/posts/{id}` | Atualização parcial ainda não implementada |
| `PATCH` | `/api/posts/{id}/cancel-schedule` | Cancelamento de agendamento ainda não implementado |
| `GET` | `/api/posts/pinned` | Listagem de comunicados fixados ainda não implementada |
| `POST` | `/api/posts` | Removido — criação só via `POST /api/posts/publish` ou `POST /api/posts/schedule` |

---

## 8. Cenários de teste no Postman

### 8.1 Health check (sem auth)

```
GET {{baseUrl}}/actuator/health
```

Esperado: `200` com `"status":"UP"`.

---

### 8.2 Publicar comunicado

```
POST {{baseUrl}}/api/posts/publish
Content-Type: application/json
Authorization: Bearer {{token}}
```

Body (destino geral — use token `SENAI`):

```json
{
  "title": "Retirada de documentos",
  "description": "Comparecer na secretaria até sexta-feira.",
  "origin": "SENAI",
  "destinations": [
    { "type": "GENERAL", "referenceId": null }
  ],
  "pinned": false,
  "tagIds": []
}
```

Esperado: `201 Created`, header `Location: /api/posts/{uuid}`, corpo com `id`, `status: "PUBLISHED"`.

**Test script (Postman)** — salvar ID:

```javascript
if (pm.response.code === 201) {
    const body = pm.response.json();
    pm.environment.set("postId", body.id);
}
```

**Destino por turma** (token `TEACHER` com `classId` no JWT):

```json
{
  "title": "Aviso da turma",
  "description": "Material para a próxima aula.",
  "origin": "BOTH",
  "destinations": [
    {
      "type": "CLASS",
      "referenceId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    }
  ]
}
```

**Destino por usuário** (com mock de alunos configurado):

```json
{
  "title": "Aviso individual",
  "description": "Entrega pendente.",
  "origin": "WEG",
  "destinations": [
    {
      "type": "USER",
      "referenceId": "44444444-4444-4444-4444-444444444444"
    }
  ]
}
```

---

### 8.3 Agendar comunicado

```
POST {{baseUrl}}/api/posts/schedule
```

```json
{
  "title": "Comunicado agendado",
  "description": "Será publicado automaticamente.",
  "origin": "SENAI",
  "scheduledFor": "2026-12-31T10:00:00Z",
  "destinations": [
    { "type": "GENERAL", "referenceId": null }
  ],
  "pinned": false
}
```

Esperado: `201`, `status: "SCHEDULED"`.

Erros comuns:

- `400` — `scheduledFor` no passado ou body inválido (`@Future` no DTO).

---

### 8.4 Listar comunicados

```
GET {{baseUrl}}/api/posts?page=0&size=20
```

Filtros opcionais (query params):

| Param | Exemplo | Descrição |
|-------|---------|-----------|
| `origin` | `WEG` | `WEG`, `SENAI`, `BOTH` |
| `classId` | `uuid` | Comunicados com destino à turma |
| `publishedFrom` | `2026-01-01T00:00:00Z` | Publicados a partir de |
| `publishedTo` | `2026-12-31T23:59:59Z` | Publicados até |
| `search` | `retirada` | Busca em título, descrição, tag e destinatário (#117) |
| `page` | `0` | Página (default 0) |
| `size` | `20` | Tamanho (default 20, máx. 100) |

Exemplos:

```
GET {{baseUrl}}/api/posts?search=retirada
GET {{baseUrl}}/api/posts?origin=SENAI&search=joão
GET {{baseUrl}}/api/posts?classId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
```

Esperado: `200` com `{ "items": [...], "page", "size", "totalElements", "totalPages" }`.

**Dica:** com token `STUDENT`, a lista já vem filtrada pelo escopo — não retorna comunicados invisíveis.

---

### 8.5 Buscar comunicado por ID

```
GET {{baseUrl}}/api/posts/{{postId}}
```

Esperado: `200` com detalhes (`destinations`, `tags`, histórico, etc.).

Fora do escopo: `404` (mesmo que exista no banco).

---

### 8.6 Atualizar comunicado

1. `GET {{baseUrl}}/api/posts/{{postId}}` — copie campos atuais.
2. `PUT {{baseUrl}}/api/posts/{{postId}}`

```json
{
  "title": "Retirada de documentos (atualizado)",
  "description": "Novo horário: 14h às 17h.",
  "origin": "SENAI",
  "status": "PUBLISHED",
  "pinned": false,
  "pinnedOrder": 0,
  "scheduledFor": null,
  "destinations": [
    {
      "announcementId": "{{postId}}",
      "type": "GENERAL",
      "referenceId": null
    }
  ]
}
```

Esperado: `200`. Comunicado removido: `409`.

---

### 8.7 Remover comunicado

```
DELETE {{baseUrl}}/api/posts/{{postId}}
```

Esperado: `204 No Content`. Depois, `GET` do mesmo ID → `404`.

---

### 8.8 Tags

#### Listar tags disponíveis

```
GET {{baseUrl}}/api/tags
GET {{baseUrl}}/api/tags?entityType=CLASS
```

`entityType`: `COURSE`, `CLASS`, `USER`, `GENERAL`.

#### Criar tags para teste (sem RabbitMQ)

Com H2 console (`http://localhost:8083/h2-console`, JDBC URL do `application-dev.yaml`):

```sql
INSERT INTO tag (id, name, entity_type, hub_entity_id, active, created_at, updated_at)
VALUES (
  '66666666-6666-6666-6666-666666666666',
  'MI78 - Manhã',
  'CLASS',
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
```

#### Vincular tags ao comunicado

```
POST {{baseUrl}}/api/posts/{{postId}}/tags
```

```json
{
  "tagIds": ["66666666-6666-6666-6666-666666666666"]
}
```

Esperado: `200` com lista de tags associadas.

#### Listar tags do comunicado

```
GET {{baseUrl}}/api/posts/{{postId}}/tags
```

#### Desvincular tag

```
DELETE {{baseUrl}}/api/posts/{{postId}}/tags/{{tagId}}
```

Esperado: `204`.

---

### 8.9 Tags via RabbitMQ (opcional)

Só se `MESSAGING_ENABLED=true` e RabbitMQ rodando. Contrato completo: [`docs/tags-por-eventos.md`](./tags-por-eventos.md).

Fluxo resumido:

1. Publicar evento `turma.created` no exchange `portal.core.events`.
2. `GET /api/tags?entityType=CLASS` deve listar a tag sincronizada.
3. Republicar o mesmo `eventId` não duplica registro.

---

## 9. Importar collection a partir do OpenAPI

1. Postman → **Import** → **Link**
2. URL: `http://localhost:8083/v3/api-docs`
3. Configure o ambiente com `token` e `baseUrl`
4. Remova ou ignore requests de endpoints **não implementados** (seção 7)

No Swagger UI, use **Authorize** com o mesmo Bearer token.

---

## 10. Fluxo completo sugerido (checklist)

Ordem recomendada para validar o módulo inteiro:

1. [ ] `GET /actuator/health` — sem token
2. [ ] Gerar JWT `SENAI` e configurar Postman
3. [ ] `POST /api/posts/publish` — salvar `postId`
4. [ ] `GET /api/posts` — comunicado aparece na lista
5. [ ] `GET /api/posts?search=retirada` — busca textual (#117)
6. [ ] `GET /api/posts/{{postId}}` — detalhe
7. [ ] Inserir tag no H2 (ou via RabbitMQ)
8. [ ] `GET /api/tags`
9. [ ] `POST /api/posts/{{postId}}/tags`
10. [ ] `GET /api/posts/{{postId}}/tags`
11. [ ] `PUT /api/posts/{{postId}}` — editar
12. [ ] `POST /api/posts/schedule` — agendar outro comunicado
13. [ ] Trocar token para `STUDENT` — validar escopo na listagem
14. [ ] `DELETE /api/posts/{{postId}}`

---

## 11. Troubleshooting

| Problema | Causa provável | Solução |
|----------|----------------|---------|
| `401 Authentication is required` | Request sem Bearer | Adicionar `Authorization: Bearer {{token}}` |
| `401 Invalid or expired token` | Segredo ou `exp` incorretos | Regenerar JWT com segredo do `application-dev.yaml` |
| `403` ao publicar como TEACHER | Destino fora das turmas do JWT | Alinhar `classId` no token e em `destinations` |
| `404` em comunicado existente | Fora do escopo do perfil | Usar token com permissão ou destino visível |
| `400` no schedule | Data no passado | `scheduledFor` futuro em ISO-8601 UTC |
| Busca por nome não acha destinatário | Mock sem alunos | Configurar `hub.mock.students-by-class` (seção 4.2) |
| App não sobe (`ClassReader` / ASM) | `.class` corrompido | `./mvnw clean compile` |
| Erro RabbitMQ na subida | Messaging ligado sem broker | `MESSAGING_ENABLED=false` |
| `POST /api/posts` → 405 | Endpoint removido (#106) | Usar `/publish` ou `/schedule` |

---

## 12. Referências no código

- JWT: `shared/security/token/JwtExtractToken.java`
- Segurança: `shared/security/config/SecurityConfig.java`
- Mock Hub: `infrastructure/hub/adapter/Mock*Adapter.java`, `hub.mock.*` em `application.yaml`
- Permissões: `domain/validator/AnnouncementPermissionValidator.java`
- Listagem + busca: `ListAnnouncementsUseCase`, `AnnouncementSpecifications.matchesSearch`
- Contrato HTTP: `http://localhost:8083/swagger-ui.html`
