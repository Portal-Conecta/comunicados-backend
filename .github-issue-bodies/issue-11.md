## Descrição

### Contexto

Implementar as entidades de persistência do **módulo de Comunicados** no backend, alinhadas ao modelo de dados definido na especificação e às regras de negócio referenciadas (RN-COM-C01, C02, C03, C09, C10, C13, RN-COM-IMG01).

Esta issue cobre **somente a camada de domínio/persistência** (entidades, enums, relacionamentos e mapeamentos). Repositórios, services e APIs ficam fora do escopo. **Migrations** estão na sub-issue #26.

### Objetivo

Criar classes de entidade (e enums associados) para todas as tabelas listadas abaixo, com:

- Tipos corretos (`UUID`, `String`, `Boolean`, `LocalDateTime`, etc.)
- Relacionamentos JPA (`@ManyToOne`, `@OneToMany`, tabelas associativas)
- FKs documentadas para entidades do Hub (`User`, e referências por `referenceId` quando aplicável)
- Enums para campos com valores fixos
- Convenções do projeto (nomenclatura, auditing, soft delete onde indicado)

### Observações

Atributos no backend em **camelCase**; em `@Column(name = "...")` usar **snake_case** em inglês:

```java
@Column(name = "user_type")
private UserType userType;
```

### Escopo — Entidades principais

#### 1. `Announcement`

Tabela central do aviso.

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `id` | `UUID` | PK |
| `title` | `String` | Obrigatório |
| `description` | `String` (text) | Corpo do comunicado |
| `origin` | enum | `WEG`, `SENAI`, `BOTH` |
| `status` | enum | `DRAFT`, `SCHEDULED`, `PUBLISHED`, `REMOVED` |
| `pinned` | `boolean` | Destaque na listagem |
| `pinnedOrder` | `Short` | Nullable; ordem entre fixados |
| `createdByUserId` | `UUID` | FK → Hub `User` |
| `publishedByUserId` | `UUID` | FK → Hub `User`; nullable até publicar |
| `scheduledFor` | `LocalDateTime` | Publicação agendada |
| `publishedAt` | `LocalDateTime` | Visível ao público |
| `removedAt` | `LocalDateTime` | Soft delete; null enquanto ativo |
| `createdAt` | `LocalDateTime` | Auditoria |
| `updatedAt` | `LocalDateTime` | Auditoria |

**Relacionamentos esperados (lado inverso):** destinos, arquivos, histórico, tags, menções, avisos individuais.

---

#### 2. `AnnouncementDestination`

Destinatários do comunicado (N destinos por comunicado). RN-COM-C03.

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `announcementId` / `@ManyToOne announcement` | `UUID` | FK |
| `type` | enum | `GENERAL`, `COURSE`, `CLASS`, `USER` |
| `referenceId` | `UUID` | ID no Hub; nullable se `GENERAL` |

> **Nota:** Avaliar PK composta (`announcementId` + `type` + `referenceId`) ou surrogate `id`, conforme padrão do projeto.

---

#### 3. `AnnouncementFile`

Anexos/imagens no S3. RN-COM-IMG01.

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `id` | `UUID` | PK |
| `announcementId` | `UUID` | FK |
| `originalName` | `String` | Nome do upload |
| `s3Key` | `String` | Path/UUID no bucket |
| `s3Bucket` | `String` | Nome do bucket |
| `contentType` | `String` | Ex.: `image/png` |
| `type` | enum | `IMAGE`, `DOCUMENT`, `VIDEO` |
| `sizeBytes` | `Long` | Auditoria/limites |
| `isThumbnail` | `boolean` | Capa/miniatura |
| `uploadedByUserId` | `UUID` | FK Hub `User` |
| `createdAt` | `LocalDateTime` | Timestamp do upload |

---

#### 4. `AnnouncementHistory`

Auditoria de alterações. RN-COM-C09 (sem JSONB no banco; `snapshot` como `text`).

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `id` | `UUID` | PK |
| `announcementId` | `UUID` | FK |
| `userId` | `UUID` | FK Hub `User` |
| `action` | enum | `CREATION`, `EDIT`, `PUBLICATION`, `REMOVAL` |
| `snapshot` | `String` (text) | JSON serializado; preenchido em `PUBLICATION`, `REMOVAL`, `EDIT` |
| `createdAt` | `LocalDateTime` | Momento do evento |

---

### Escopo — Tabelas de suporte

#### 5. `Tag`

| Atributo | Tipo |
|----------|------|
| `id` | `UUID` |
| `name` | `String` |
| `entityType` | enum `COURSE`, `CLASS`, `USER` |
| `entityId` | `UUID` |
| `active` | `boolean` |
| `createdAt` | `LocalDateTime` |

#### 6. `AnnouncementTag` (associativa)

| Atributo |
|----------|
| `announcementId` |
| `tagId` |

PK composta.

#### 7. `AnnouncementMention`

Menções explícitas (`@usuario`) no texto.

| Atributo |
|----------|
| `announcementId` |
| `userId` |

#### 8. `IndividualNoticeCategory`

Catálogo de categorias para avisos individuais (ex.: retirada de crachá).

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `id` | `UUID` | PK |
| `name` | `String` | Nome da categoria |
| `createdAt` | `LocalDateTime` | Auditoria |
| `updatedAt` | `LocalDateTime` | Auditoria |
| `deletedAt` | `LocalDateTime` | Soft delete; null enquanto ativo |

#### 9. `AnnouncementIndividualNotice`

Notificação direta vinculada a um comunicado.

| Atributo | Tipo | Observações |
|----------|------|-------------|
| `id` | `UUID` | PK |
| `announcementId` | `UUID` | FK → `Announcement` |
| `categoryId` | `UUID` | FK → `IndividualNoticeCategory` |
| `resolvedAt` | `LocalDateTime` | Nullable; preenchido quando o aviso for resolvido |

**Relacionamentos esperados:**

- `@ManyToOne` → `Announcement`
- `@ManyToOne` → `IndividualNoticeCategory`

---

### Enums a criar

| Enum Java | Valores |
|-----------|---------|
| `AnnouncementOrigin` | `WEG`, `SENAI`, `BOTH` |
| `AnnouncementStatus` | `DRAFT`, `SCHEDULED`, `PUBLISHED`, `REMOVED` |
| `AnnouncementDestinationType` | `GENERAL`, `COURSE`, `CLASS`, `USER` |
| `AnnouncementFileType` | `IMAGE`, `DOCUMENT`, `VIDEO` |
| `AnnouncementHistoryAction` | `CREATION`, `EDIT`, `PUBLICATION`, `REMOVAL` |
| `TagEntityType` | `COURSE`, `CLASS`, `USER` |

Mapear valores do banco (`RASCUNHO`, `GERAL`, `IMAGEM`, etc.) com `@Enumerated` + conversor ou nomes de coluna explícitos, conforme migrations.

---

### Critérios de aceite

- [ ] Todas as entidades acima criadas no pacote/módulo de Comunicados
- [ ] Enums criados e mapeados aos valores da especificação
- [ ] Relacionamentos bidirecionais configurados onde fizer sentido
- [ ] FKs para `User` do Hub modeladas como `UUID`
- [ ] Campos nullable conforme tabela
- [ ] `AnnouncementHistory.snapshot` como `String`/`@Lob`, **sem** `jsonb`
- [ ] Nomenclatura de colunas em snake_case inglês
- [ ] Build compila sem erros

---

### Referências

- [Modelagem do BD — §5 Comunicados](https://www.notion.so/Modelagem-do-Banco-de-Dados-36441eabebf8809f9cb5ffb740263700)
- [Requisitos e RN](https://www.notion.so/Requisitos-e-RN-36141eabebf880dda4cbf40257d070d4)
- [Diagrama ER](https://www.notion.so/Comunicados-36041eabebf8805a98e1d2d0b6844a6e)

---

### Checklist técnico para o dev

1. Confirmar tipo de data global (`LocalDateTime`)
2. Validar PKs das tabelas associativas com a migration (#26)
3. Abrir PR referenciando esta issue

---

## Sub-issues

- [ ] #16 — #11.1 Setup pacote e convenções JPA
- [ ] #17 — #11.2 Enums do módulo
- [ ] #18 — #11.3 Entidade Announcement
- [ ] #19 — #11.4 Entidade AnnouncementDestination
- [ ] #20 — #11.5 Entidade AnnouncementFile
- [ ] #21 — #11.6 Entidade AnnouncementHistory
- [ ] #22 — #11.7 Entidades Tag e AnnouncementTag
- [ ] #23 — #11.8 Entidade AnnouncementMention
- [ ] #24 — #11.9 IndividualNoticeCategory e AnnouncementIndividualNotice
- [ ] #25 — #11.10 Relacionamentos bidirecionais JPA
- [ ] #26 — #11.11 Migrations Flyway
