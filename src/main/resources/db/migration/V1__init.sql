-- Baseline schema, generated from the JPA entities (Hibernate ddl-auto) on PostgreSQL 18
-- and tidied: tables in dependency order, readable constraint names, and the partial
-- unique index on files that JPA cannot express.

CREATE TABLE users (
    id                 uuid                        NOT NULL,
    email              varchar(255)                NOT NULL,
    password_hash      varchar(255),
    first_name         varchar(255)                NOT NULL,
    last_name          varchar(255)                NOT NULL,
    created_date       timestamp(6) with time zone NOT NULL DEFAULT now(),
    deleted_at         timestamp(6) with time zone,
    email_verified_at  timestamp(6) with time zone,
    version            bigint                      NOT NULL DEFAULT 0,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_users_email_active
    ON users (lower(email))
    WHERE deleted_at IS NULL;

CREATE TABLE workspaces (
    id          uuid                        NOT NULL,
    name        varchar(255)                NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL DEFAULT now(),
    deleted_at  timestamp(6) with time zone,
    version     bigint                      NOT NULL DEFAULT 0,
    owner_id    uuid                        NOT NULL,
    CONSTRAINT workspaces_pkey PRIMARY KEY (id),
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE projects (
    id            uuid                        NOT NULL,
    workspace_id  uuid                        NOT NULL,
    name          varchar(255)                NOT NULL,
    path          varchar(255)                NOT NULL,
    seq           bigint                      NOT NULL DEFAULT 0,
    created_at    timestamp(6) with time zone NOT NULL DEFAULT now(),
    deleted_at    timestamp(6) with time zone,
    version       bigint                      NOT NULL DEFAULT 0,
    owner_id      uuid                        NOT NULL,
    CONSTRAINT projects_pkey PRIMARY KEY (id),
    CONSTRAINT fk_projects_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- A (path, name) is unique per workspace among projects that are not soft-deleted.
CREATE UNIQUE INDEX uk_projects_workspace_path_name_active
    ON projects (workspace_id, path, name)
    WHERE deleted_at IS NULL;

CREATE TABLE files (
    id          uuid                        NOT NULL,
    project_id  uuid                        NOT NULL,
    path        varchar(255)                NOT NULL,
    created_at  timestamp(6) with time zone NOT NULL DEFAULT now(),
    owner_id    uuid                        NOT NULL,
    deleted_at  timestamp(6) with time zone,
    version     bigint                      NOT NULL DEFAULT 0,
    CONSTRAINT files_pkey PRIMARY KEY (id),
    CONSTRAINT fk_files_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_files_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- A path is unique per project among files that are not soft-deleted.
CREATE UNIQUE INDEX uk_files_project_path_active
    ON files (project_id, path)
    WHERE deleted_at IS NULL;

CREATE TABLE operations (
    id                uuid                        NOT NULL,
    project_id        uuid                        NOT NULL,
    user_id           uuid                        NOT NULL,
    seq               bigint                      NOT NULL,
    client_seq        bigint                      NOT NULL,
    "timestamp"       timestamp(6) with time zone NOT NULL DEFAULT now(),
    client_timestamp  timestamp(6) with time zone NOT NULL,
    type              varchar(255)                NOT NULL,
    payload           jsonb                       NOT NULL,
    CONSTRAINT operations_pkey PRIMARY KEY (id),
    CONSTRAINT uk_project_seq UNIQUE (project_id, seq),
    CONSTRAINT operations_type_check CHECK (type IN ('FILE', 'TERMINAL')),
    CONSTRAINT fk_operations_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_operations_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE refresh_tokens (
    id          uuid                        NOT NULL,
    user_id     uuid                        NOT NULL,
    token_hash  varchar(255)                NOT NULL,
    expires_at  timestamp(6) with time zone NOT NULL,
    revoked_at  timestamp(6) with time zone,
    created_at  timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE email_verification_tokens (
    id          uuid                        NOT NULL,
    user_id     uuid                        NOT NULL,
    token_hash  varchar(255)                NOT NULL,
    expires_at  timestamp(6) with time zone NOT NULL,
    used_at     timestamp(6) with time zone,
    created_at  timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE extension_auth_codes (
    id                     uuid                        NOT NULL,
    user_id                uuid                        NOT NULL,
    code_hash              varchar(255)                NOT NULL,
    code_challenge         varchar(255)                NOT NULL,
    code_challenge_method  varchar(255)                NOT NULL,
    expires_at             timestamp(6) with time zone NOT NULL,
    used_at                timestamp(6) with time zone,
    created_at             timestamp(6) with time zone NOT NULL DEFAULT now(),
    CONSTRAINT extension_auth_codes_pkey PRIMARY KEY (id),
    CONSTRAINT uk_extension_auth_codes_code_hash UNIQUE (code_hash),
    CONSTRAINT uk_extension_auth_codes_code_challenge UNIQUE (code_challenge),
    CONSTRAINT fk_extension_auth_codes_user FOREIGN KEY (user_id) REFERENCES users (id)
);
