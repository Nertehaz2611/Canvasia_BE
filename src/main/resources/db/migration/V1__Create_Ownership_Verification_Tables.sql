-- Migration: Create Ownership Verification Tables
-- Date: 2026-06-17

-- Create OwnershipVerificationStatus enum type
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'ownership_verification_status') THEN
        CREATE TYPE ownership_verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
    END IF;
END
$$;

-- Create RejectionReason enum type
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'rejection_reason') THEN
        CREATE TYPE rejection_reason AS ENUM ('INVALID_DOCUMENT', 'UNCLEAR_INFORMATION', 'MISMATCHED_IDENTITY', 'SUSPICIOUS_ACTIVITY', 'OTHER');
    END IF;
END
$$;

-- Create ownership_verifications table
CREATE TABLE IF NOT EXISTS ownership_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    post_id UUID UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    identity_document_type VARCHAR(50) NOT NULL,
    identity_document_number VARCHAR(50) NOT NULL,
    country_code VARCHAR(50) NOT NULL,
    status ownership_verification_status NOT NULL DEFAULT 'PENDING',
    admin_notes TEXT,
    rejection_reason rejection_reason,
    reviewed_at TIMESTAMP,
    reviewed_by_admin_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ownership_verifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ownership_verifications_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE SET NULL,
    CONSTRAINT fk_ownership_verifications_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for ownership_verifications
CREATE INDEX idx_ownership_verifications_user_id ON ownership_verifications(user_id);
CREATE INDEX idx_ownership_verifications_status ON ownership_verifications(status);
CREATE INDEX idx_ownership_verifications_created_at ON ownership_verifications(created_at);
CREATE INDEX idx_ownership_verifications_post_id ON ownership_verifications(post_id);

-- Create ownership_verification_media table
CREATE TABLE IF NOT EXISTS ownership_verification_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ownership_verification_id UUID NOT NULL,
    media_url VARCHAR(500) NOT NULL,
    public_id VARCHAR(100) NOT NULL,
    media_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ownership_verification_media_verification FOREIGN KEY (ownership_verification_id) REFERENCES ownership_verifications(id) ON DELETE CASCADE
);

-- Create indexes for ownership_verification_media
CREATE INDEX idx_ownership_verification_media_verification_id ON ownership_verification_media(ownership_verification_id);

-- Add new notification types
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('POST_LIKE', 'COMMENT_LIKE', 'POST_COMMENT', 'COMMENT_REPLY', 'COMMENT_THREAD_REPLY', 'POST_PENDING', 'POST_APPROVED', 'POST_REJECTED', 'POST_DELETED', 'POST_DELETED_REPORTED', 'FOLLOW', 'OWNERSHIP_VERIFICATION_PENDING', 'OWNERSHIP_VERIFICATION_APPROVED', 'OWNERSHIP_VERIFICATION_REJECTED', 'COPYRIGHT_VIOLATION_DETECTED'));
