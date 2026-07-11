 CREATE TABLE users (
	id TEXT PRIMARY KEY UNIQUE NOT NULL,
	login TEXT UNIQUE NOT NULL,
	password TEXT  NOT NULL,
	role TEXT NOT NULL,
    google_id TEXT UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
 );

 CREATE TABLE professionals (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id TEXT UNIQUE NOT NULL,
    business_name TEXT NOT NULL,
    contact_phone TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
 );