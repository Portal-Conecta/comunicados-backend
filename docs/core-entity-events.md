# Contrato de eventos Core → Comunicados (tags)

Documento de referência para o time **Core** publicar entidades que o módulo Comunicados espelha como **tags** locais.

## Visão geral

- **Exchange (topic):** `portal.core.events`
- **Queue Comunicados:** `comunicados.core-entities`
- **DLQ:** `comunicados.core-entities.dlq`
- **Routing keys:** `core.course.#`, `core.class.#`, `core.user.#`
- **Source esperado:** `portal-core`

Tags **não** são criadas via REST (`POST /api/tags`). A origem é exclusivamente mensageria (TAG02).

O campo `payload.entityId` vira `hub_entity_id` na tabela `tag` e deve ser o **mesmo UUID** usado em `announcement_destination.reference_id` para auto-vinculação (#110).

## Envelope

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "core.class.created",
  "occurredAt": "2026-06-12T10:00:00Z",
  "source": "portal-core",
  "payload": {
    "entityId": "uuid-da-entidade-no-hub",
    "entityType": "CLASS",
    "name": "MI78 - Manhã",
    "active": true,
    "metadata": {
      "courseId": "uuid-curso",
      "shift": "MANHA"
    }
  }
}
```

## Eventos suportados

| eventType | Ação no Comunicados |
|-----------|---------------------|
| `core.course.created` | Upsert tag `COURSE` |
| `core.course.updated` | Upsert tag `COURSE` |
| `core.class.created` | Upsert tag `CLASS` |
| `core.class.updated` | Upsert tag `CLASS` |
| `core.user.created` | Upsert tag `USER` |
| `core.user.updated` | Upsert tag `USER` |
| `core.course.deactivated` | `active = false` |
| `core.class.deactivated` | `active = false` |
| `core.user.deactivated` | `active = false` |

## Regras

- **Idempotência:** `eventId` duplicado é ignorado (tabela `processed_event`).
- **Upsert:** chave natural `(entity_type, hub_entity_id)` — updates não duplicam registro.
- **Payload inválido:** mensagem vai para DLQ (`defaultRequeueRejected=false` + dead-letter).
- **metadata:** opcional; não persiste na tag hoje.

## Validação local

1. Publicar `core.class.created` no exchange.
2. `GET /api/tags?entityType=CLASS` deve listar a tag sincronizada.
3. Republicar o mesmo `eventId` não deve duplicar tag nem `processed_event` com efeito colateral.
