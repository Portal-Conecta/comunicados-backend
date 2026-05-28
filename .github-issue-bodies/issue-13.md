## Descrição

### Contexto

Implementar a **camada de aplicação (Use Cases)** do **módulo de Comunicados**, alinhado à especificação, RNs (RN-COM-*) e à **matriz oficial de permissões** do Hub.

Esta issue cobre **somente use cases** + **ports** (interfaces Hub/Storage). Controllers e clients HTTP ficam em outras issues.

**Dependências:** #11 entidades, #12 DTOs/Mappers, #14 repositories/adapters (parcialmente em paralelo).

### Fonte oficial de perfis (Hub)

`APRENDIZ`, `REPRESENTANTE`, `DOCENTE`, `PERFIL_SENAI`, `PERFIL_WEG`, `ADMINISTRADOR`.

Legados (`INSTRUTOR`, `GESTOR`) → `PERFIL_SENAI` ou `PERFIL_WEG`.

### Matriz oficial — Comunicados

| Perfil | Visualização | Criação |
|--------|--------------|--------|
| `PERFIL_SENAI` / `PERFIL_WEG` | Todos | Sim (todos escopos) |
| `DOCENTE` | Associados | Turmas vinculadas |
| `REPRESENTANTE` | Associados | **Própria turma** |
| `APRENDIZ` | Associados | **Não** |
| `ADMINISTRADOR` | Todos | Conforme Hub |

### Escopos de acesso

| Escopo | Perfis |
|--------|--------|
| Própria turma | `REPRESENTANTE` |
| Turmas vinculadas | `DOCENTE` |
| Pedagógico SENAI/WEG | `PERFIL_SENAI`, `PERFIL_WEG` |
| Administração global | `ADMINISTRADOR` |

**Filtros de listagem:** WEG, SENAI, TODOS, TURMA, UC.

### Objetivo

Use cases que recebem/retornam **DTOs**, aplicam RNs, persistem via repositories, registram **histórico** (RN-COM-C09) e checam permissão via **`HubPermissionPort`**.

**Pacote:** `com.portal.conecta.comunicados.application.usecase` e `.application.port`

### Escopo — Use Cases

#### Ciclo de vida — RN-COM-C01, C02, C10, C13

| Use Case | Resumo |
|----------|--------|
| `CreateAnnouncementUseCase` | Não `APRENDIZ`; destinos conforme perfil |
| `UpdateAnnouncementUseCase` | Rascunho/agendado; histórico EDIT |
| `ScheduleAnnouncementUseCase` | `SCHEDULED`, data futura |
| `PublishAnnouncementUseCase` | Título, descrição, destino obrigatórios |
| `RemoveAnnouncementUseCase` | Soft delete; histórico REMOVAL |
| `GetAnnouncementByIdUseCase` | Escopo visualização |
| `ListAnnouncementsUseCase` | Filtros + cronológico; sem removidos |

#### Fixação — RN-COM-C13

| Use Case | Quem |
|----------|------|
| `PinAnnouncementUseCase` / `UnpinAnnouncementUseCase` | WEG, SENAI, ADMIN |
| `ListPinnedAnnouncementsUseCase` | conforme visualização |

#### Destinos — RN-COM-C03

`AddAnnouncementDestinationUseCase`, `RemoveAnnouncementDestinationUseCase`, `ReplaceAnnouncementDestinationsUseCase`

#### Arquivos — RN-COM-IMG01, C12

`AttachAnnouncementFileUseCase`, `RemoveAnnouncementFileUseCase`, `SetAnnouncementThumbnailUseCase` (máx. 5 imagens)

#### Tags — RN-COM-TAG*

`ListTagsUseCase`, `LinkAnnouncementTagUseCase`, `UnlinkAnnouncementTagUseCase`, `ListAnnouncementTagsUseCase`

#### Avisos individuais — RN-COM-AVI*

`CreateAnnouncementIndividualNoticeUseCase` (WEG/SENAI), `List...`, `Resolve...`

#### Histórico — RN-COM-C09

`RecordAnnouncementHistoryUseCase` (interno), `ListAnnouncementHistoryUseCase`

### Ports

| Port | Uso |
|------|-----|
| `HubAuthPort` | Perfil autenticado |
| `HubPermissionPort` | canView, canCreate, canPublish, canPin |
| `HubClassPort` | Turmas vinculadas |
| `HubUserPort` | Autor, menções |
| `StoragePort` | Metadados S3 (opcional) |

### Critérios de aceite

- [ ] Use cases implementados com testes unitários (mocks Hub + repository)
- [ ] Matriz de permissões respeitada
- [ ] Histórico em fluxos relevantes
- [ ] Build compila

### Referências

- [Requisitos e RN](https://www.notion.so/Requisitos-e-RN-36141eabebf880dda4cbf40257d070d4)
- [Funcionalidades](https://www.notion.so/Funcionalidades-36041eabebf8807da267d23feb2a65d8)
- [Permissões definidas](https://www.notion.so/Permiss-es-Definidas-36541eabebf8803582fdf3816be49d04)
- [Modelagem do BD](https://www.notion.so/Modelagem-do-Banco-de-Dados-36441eabebf8809f9cb5ffb740263700)

---

## Sub-issues

- [ ] #36 — #13.1 Ports Hub e Storage
- [ ] #37 — #13.2 Use cases Create e Update Announcement
- [ ] #38 — #13.3 Use cases Publish e Schedule
- [ ] #39 — #13.4 Use cases Remove e GetById
- [ ] #40 — #13.5 Use case ListAnnouncements com filtros
- [ ] #41 — #13.6 Use cases Pin e Unpin
- [ ] #42 — #13.7 Use cases Destinos
- [ ] #43 — #13.8 Use cases Arquivos e Imagens
- [ ] #44 — #13.9 Use cases Tags
- [ ] #45 — #13.10 Use cases Avisos individuais
- [ ] #46 — #13.11 Histórico (Record e List)
- [ ] #47 — #13.12 Testes unitários dos use cases
