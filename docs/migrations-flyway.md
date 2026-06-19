# Migrations — Flyway

O schema de **produção** é versionado e aplicado via [Flyway](https://documentation.red-gate.com/flyway).
Em dev/test o schema continua sendo gerado pelo Hibernate (`ddl-auto`), então o Flyway fica desligado nesses perfis.

## Estado por profile

| Profile | `spring.flyway.enabled` | `ddl-auto` | Quem cria o schema |
|---------|-------------------------|------------|--------------------|
| `dev` (default) | `false` | `update` | Hibernate (H2 em memória) |
| `test` | `false` | `create-drop` | Hibernate (H2 em memória) |
| `prod` | `true` | `validate` | **Flyway**; Hibernate só valida |

Em `prod`, o app **não sobe** se o schema do banco divergir das entidades JPA (`validate`).
Por isso toda alteração de entidade precisa de uma migration correspondente.

## Localização e nomenclatura

- Diretório: `src/main/resources/db/migration/`
- Padrão: `V<NNN>__<descricao_em_snake_case>.sql` (ex.: `V002__add_coluna_x.sql`)
- `V001__create_schema.sql` é o baseline: cria todas as tabelas, FKs, índices e UKs.
- Numeração sequencial e **imutável**: migrations já aplicadas nunca são editadas — corrija com uma nova versão.

### Tipos (dialeto PostgreSQL do Hibernate)

| Java | Coluna |
|------|--------|
| `UUID` | `uuid` |
| `String` | `varchar(255)` |
| `enum` (`EnumType.STRING`) | `varchar(255)` |
| `Instant` | `timestamp(6) with time zone` |
| `boolean` / `Short` / `Long` | `boolean` / `smallint` / `bigint` |
| `@Column(columnDefinition = "TEXT")` | `text` |

IDs são gerados pela aplicação (`GenerationType.UUID`); o banco não define `DEFAULT`.

## Como rodar / validar localmente

Subir um Postgres limpo e o app no profile `prod`:

```bash
# 1. Postgres (porta 5434 no host, via docker-compose)
docker compose up -d postgres

# 2. App em prod apontando para ele
DB_URL="jdbc:postgresql://localhost:5434/comunicados" \
DB_USERNAME=comunicados DB_PASSWORD=comunicados_dev_password \
JWT_SECRET="<base64>" JWT_ACCESS_EXPIRATION=900000 JWT_REFRESH_EXPIRATION=604800000 \
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

No startup os logs devem mostrar `Migrating schema "public" to version "001 ..."` →
`Successfully applied 1 migration` → `Started ComunicadosApplication` (sem erro de `Schema validation`).

Conferir o estado aplicado:

```bash
docker exec portal-conecta-comunicados-postgres \
  psql -U comunicados -d comunicados -c "SELECT version, description, success FROM flyway_schema_history;"
```

## Variáveis de ambiente

- `FLYWAY_BASELINE_ON_MIGRATE` (default `false`): use `true` apenas ao habilitar o Flyway pela
  primeira vez sobre um banco **já existente** (cria o baseline sem reexecutar o histórico).

## Dependências

- `org.springframework.boot:spring-boot-flyway` — autoconfiguração do Flyway no Spring Boot 4
  (necessária além do `flyway-core`, que sozinho não registra a integração).
- `org.flywaydb:flyway-database-postgresql` — suporte ao PostgreSQL no Flyway 11+.
