CREATE TABLE users
(
    id         TEXT PRIMARY KEY UNIQUE NOT NULL,
    login      TEXT UNIQUE             NOT NULL,
    password   TEXT                    NOT NULL,
    name       TEXT                    NOT NULL,
    phone      TEXT,
    role       TEXT                    NOT NULL,
    google_id  TEXT UNIQUE,
    is_active  BOOLEAN                 NOT NULL,
    deleted_at TIMESTAMPTZ,
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

CREATE TABLE businesses
(
    id         TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id    TEXT UNIQUE             NOT NULL,
    name       TEXT                    NOT NULL,
    is_active  BOOLEAN                 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_business_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE team_members
(
    id          TEXT PRIMARY KEY UNIQUE NOT NULL,
    business_id TEXT                    NOT NULL,
    name        VARCHAR(100)            NOT NULL,
    user_id     TEXT,
    role        VARCHAR(20)             NOT NULL,
    pin_code    VARCHAR(4),
    is_active   BOOLEAN                 NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT fk_team_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE queue_sessions
(
    id                TEXT PRIMARY KEY   NOT NULL,
    business_id       TEXT               NOT NULL,
    ticket_code       VARCHAR(50) UNIQUE NOT NULL,
    is_active         BOOLEAN            NOT NULL,
    tolerance_minutes INTEGER            NOT NULL,
    prefix            VARCHAR(10),
    created_by        VARCHAR(255)       NOT NULL,
    created_at        TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ,
    CONSTRAINT fk_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
);

CREATE TABLE queue_entries
(
    id                  TEXT PRIMARY KEY NOT NULL,
    queue_sessions_id   TEXT             NOT NULL,
    user_id             TEXT             NOT NULL,
    served_by_member_id TEXT,
    service_name        VARCHAR(100)     NOT NULL,
    status              VARCHAR(30)      NOT NULL,
    missed_calls        INTEGER          NOT NULL,
    version             BIGINT           NOT NULL,
    joined_at           TIMESTAMPTZ      NOT NULL,
    called_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT fk_queue_sessions FOREIGN KEY (queue_sessions_id) REFERENCES queue_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_served_by_member FOREIGN KEY (served_by_member_id) REFERENCES team_members (id) ON DELETE SET NULL
);

CREATE TABLE password_reset_tokens
(
    id          TEXT PRIMARY KEY UNIQUE NOT NULL,
    email       TEXT                    NOT NULL,
    code        VARCHAR(10)             NOT NULL,
    expiry_date TIMESTAMPTZ             NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_invites
(
    id          VARCHAR(255) PRIMARY KEY,
    business_id VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_team_invites_business FOREIGN KEY (business_id) REFERENCES businesses (id) ON DELETE CASCADE
);

CREATE INDEX idx_team_invites_email ON team_invites (email);

CREATE INDEX idx_password_reset_email_code
    ON password_reset_tokens (email, code);

CREATE UNIQUE INDEX idx_unique_active_user_queue
    ON queue_entries (user_id) WHERE status IN ('WAITING', 'CALLED', 'IN_SERVICE');

CREATE INDEX idx_queue_entries_session_status
    ON queue_entries (queue_sessions_id, status, joined_at);