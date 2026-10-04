-- Image metadata only; binaries live in the storage provider (Cloudinary).
CREATE TABLE images (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_provider VARCHAR(32)  NOT NULL,
    public_id        VARCHAR(300) NOT NULL,
    url              TEXT         NOT NULL,
    resource_type    VARCHAR(32)  NOT NULL DEFAULT 'image',
    folder           VARCHAR(32)  NOT NULL,
    owner_type       VARCHAR(32)  NOT NULL,
    owner_id         UUID         NOT NULL,
    uploaded_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    width            INTEGER,
    height           INTEGER,
    format           VARCHAR(16),
    bytes            BIGINT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_images_provider_public_id UNIQUE (storage_provider, public_id),
    CONSTRAINT chk_images_owner_type CHECK (owner_type IN ('BUSINESS', 'SERVICE', 'USER')),
    CONSTRAINT chk_images_folder CHECK (folder IN
        ('BUSINESS_PROFILE', 'BUSINESS_COVER', 'BUSINESS_GALLERY', 'SERVICE_IMAGE', 'USER_AVATAR'))
);

CREATE INDEX idx_images_owner ON images (owner_type, owner_id, folder, created_at DESC);
CREATE INDEX idx_images_uploaded_by ON images (uploaded_by);

ALTER TABLE services ADD COLUMN image_url TEXT;
