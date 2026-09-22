CREATE TABLE businesses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id          UUID         NOT NULL REFERENCES users(id),
    name              VARCHAR(200) NOT NULL,
    slug              VARCHAR(220) NOT NULL,
    description       TEXT,
    phone             VARCHAR(32),
    email             VARCHAR(255),
    address           VARCHAR(500),
    city              VARCHAR(120),
    district          VARCHAR(120),
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    logo_url          VARCHAR(500),
    cover_image_url   VARCHAR(500),
    timezone          VARCHAR(64)  NOT NULL DEFAULT 'Europe/Istanbul',
    auto_confirm      BOOLEAN      NOT NULL DEFAULT TRUE,
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_businesses_slug UNIQUE (slug),
    CONSTRAINT chk_businesses_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_APPROVAL', 'SUSPENDED'))
);

CREATE INDEX idx_businesses_owner_id ON businesses (owner_id);
CREATE INDEX idx_businesses_city ON businesses (city);
CREATE INDEX idx_businesses_district ON businesses (district);
CREATE INDEX idx_businesses_status ON businesses (status);
CREATE INDEX idx_businesses_slug ON businesses (slug);

CREATE TABLE business_categories (
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    PRIMARY KEY (business_id, category_id)
);
