-- V001 — schema inicial do módulo Comunicados (#139)
--
-- Cobre todas as entidades JPA atuais. Compatível com `ddl-auto: validate` em prod.
-- Tipos espelham o mapeamento do dialeto PostgreSQL do Hibernate:
--   UUID            -> uuid
--   String          -> varchar(255)
--   enum (STRING)   -> varchar(255)
--   Instant         -> timestamp(6) with time zone (timestamptz)
--   boolean         -> boolean
--   Short / Long    -> smallint / bigint
-- IDs são gerados pela aplicação (GenerationType.UUID); o banco não define default.

-- ---------------------------------------------------------------------------
-- Tabelas-raiz (sem dependências de FK)
-- ---------------------------------------------------------------------------

CREATE TABLE announcement (
    id                   uuid                        NOT NULL,
    title                varchar(255)                NOT NULL,
    description          varchar(255)                NOT NULL,
    origin               varchar(255)                NOT NULL,
    status               varchar(255)                NOT NULL,
    pinned               boolean                     NOT NULL,
    pinned_order         smallint,
    created_by_user_id   uuid                        NOT NULL,
    created_by_user_type varchar(255),
    published_by_user_id uuid,
    scheduled_for        timestamp(6) with time zone,
    published_at         timestamp(6) with time zone,
    removed_at           timestamp(6) with time zone,
    created_at           timestamp(6) with time zone NOT NULL,
    updated_at           timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_announcement PRIMARY KEY (id)
);

-- Listagem padrão: visíveis (removed_at IS NULL) em ordem cronológica.
CREATE INDEX idx_announcement_status ON announcement (status);
CREATE INDEX idx_announcement_removed_at ON announcement (removed_at);
CREATE INDEX idx_announcement_published_at ON announcement (published_at);
-- Usado pela futura publicação automática de agendados: status + scheduled_for.
CREATE INDEX idx_announcement_scheduled_for ON announcement (scheduled_for);

CREATE TABLE tag (
    id            uuid                        NOT NULL,
    name          varchar(255)                NOT NULL,
    entity_type   varchar(255)                NOT NULL,
    hub_entity_id varchar(255)                NOT NULL,
    active        boolean                     NOT NULL,
    created_at    timestamp(6) with time zone NOT NULL,
    updated_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT uk_tag_entity_hub UNIQUE (entity_type, hub_entity_id)
);

CREATE INDEX idx_tag_hub_entity_id ON tag (hub_entity_id);

CREATE TABLE processed_event (
    event_id     varchar(255)                NOT NULL,
    processed_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_processed_event PRIMARY KEY (event_id)
);

-- ---------------------------------------------------------------------------
-- Tabelas dependentes de announcement
-- ---------------------------------------------------------------------------

CREATE TABLE announcement_destination (
    id              uuid         NOT NULL,
    announcement_id uuid         NOT NULL,
    type            varchar(255) NOT NULL,
    reference_id    uuid,
    CONSTRAINT pk_announcement_destination PRIMARY KEY (id),
    CONSTRAINT fk_destination_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id)
);

CREATE INDEX idx_destination_announcement_id ON announcement_destination (announcement_id);
-- Auto-vínculo de tags por destino (#110): casa type + reference_id com a tag.
CREATE INDEX idx_destination_type_reference ON announcement_destination (type, reference_id);

CREATE TABLE announcement_file (
    id                  uuid                        NOT NULL,
    announcement_id     uuid                        NOT NULL,
    original_name       varchar(255)                NOT NULL,
    s3_key              varchar(255)                NOT NULL,
    s3_bucket           varchar(255)                NOT NULL,
    content_type        varchar(255)                NOT NULL,
    type                varchar(255)                NOT NULL,
    size_bytes          bigint                      NOT NULL,
    is_thumbnail        boolean                     NOT NULL,
    uploaded_by_user_id uuid                        NOT NULL,
    created_at          timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_announcement_file PRIMARY KEY (id),
    CONSTRAINT fk_file_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id)
);

CREATE INDEX idx_file_announcement_id ON announcement_file (announcement_id);

CREATE TABLE announcement_history (
    id              uuid                        NOT NULL,
    announcement_id uuid                        NOT NULL,
    user_id         uuid                        NOT NULL,
    action          varchar(255)                NOT NULL,
    snapshot        text,
    created_at      timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_announcement_history PRIMARY KEY (id),
    CONSTRAINT fk_history_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id)
);

CREATE INDEX idx_history_announcement_id ON announcement_history (announcement_id);

CREATE TABLE announcement_mention (
    id              uuid NOT NULL,
    announcement_id uuid NOT NULL,
    user_id         uuid NOT NULL,
    CONSTRAINT pk_announcement_mention PRIMARY KEY (id),
    CONSTRAINT fk_mention_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id)
);

CREATE INDEX idx_mention_announcement_id ON announcement_mention (announcement_id);

CREATE TABLE announcement_tag (
    id              uuid NOT NULL,
    announcement_id uuid NOT NULL,
    tag_id          uuid NOT NULL,
    CONSTRAINT pk_announcement_tag PRIMARY KEY (id),
    CONSTRAINT fk_announcement_tag_announcement FOREIGN KEY (announcement_id)
        REFERENCES announcement (id),
    CONSTRAINT fk_announcement_tag_tag FOREIGN KEY (tag_id)
        REFERENCES tag (id),
    -- Vínculo é deduplicado em código; a UK protege contra corrida (handler -> 409).
    CONSTRAINT uk_announcement_tag UNIQUE (announcement_id, tag_id)
);

CREATE INDEX idx_announcement_tag_announcement_id ON announcement_tag (announcement_id);
CREATE INDEX idx_announcement_tag_tag_id ON announcement_tag (tag_id);
