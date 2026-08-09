CREATE TABLE users
(
    id         TEXT PRIMARY KEY UNIQUE NOT NULL,
    login      TEXT UNIQUE             NOT NULL,
    password   TEXT                    NOT NULL,
    name       TEXT                    NOT NULL,
    phone      TEXT,
    role       TEXT                    NOT NULL,
    google_id  TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE lgpd_consents
(
    id           TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id      TEXT                    NOT NULL,
    term_version VARCHAR(20)             NOT NULL,
    ip_address   VARCHAR(45),
    user_agent   TEXT,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lgpd_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE professionals
(
    id            TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id       TEXT UNIQUE             NOT NULL,
    business_name TEXT                    NOT NULL,
    is_active     BOOLEAN     DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255),
    CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE queue_sessions
(
    id                TEXT PRIMARY KEY   NOT NULL,
    professional_id   TEXT               NOT NULL,
    ticket_code       VARCHAR(50) UNIQUE NOT NULL,
    is_active         BOOLEAN            NOT NULL DEFAULT FALSE,
    tolerance_minutes INTEGER            NOT NULL,
    prefix            VARCHAR(10),
    created_at        TIMESTAMPTZ                 DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ,
    CONSTRAINT fk_professional FOREIGN KEY (professional_id) REFERENCES professionals (id) ON DELETE CASCADE
);

CREATE TABLE queue_entries
(
    id                TEXT PRIMARY KEY NOT NULL,
    queue_sessions_id TEXT             NOT NULL,
    user_id           TEXT             NOT NULL,
    service_name      VARCHAR(100)     NOT NULL,
    status            VARCHAR(30)      NOT NULL,
    missed_calls      INTEGER          NOT NULL DEFAULT 0,
    version           BIGINT           NOT NULL DEFAULT 0,
    joined_at         TIMESTAMPTZ      NOT NULL,
    called_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    CONSTRAINT fk_queue_sessions FOREIGN KEY (queue_sessions_id) REFERENCES queue_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens
(
    id          TEXT PRIMARY KEY UNIQUE NOT NULL,
    email       TEXT                    NOT NULL,
    code        VARCHAR(10)             NOT NULL,
    expiry_date TIMESTAMPTZ             NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_email_code
    ON password_reset_tokens (email, code);

CREATE UNIQUE INDEX idx_unique_active_user_queue
    ON queue_entries (user_id) WHERE status IN ('WAITING', 'CALLED', 'IN_SERVICE');

CREATE INDEX idx_queue_entries_session_status
    ON queue_entries (queue_sessions_id, status, joined_at);