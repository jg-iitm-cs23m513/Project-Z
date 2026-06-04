-- =====================================================
-- User Management Service - Initial Data Insertion
-- =====================================================

-- ─────────────────────────────────────────────────────
-- STEP 1: CREATE ROLES
-- Safe idempotent insert (runs every restart -> skips if exists)
-- ─────────────────────────────────────────────────────
INSERT INTO roles (name) VALUES
    ('Admin'),
    ('ProjectDirector'),
    ('SecurityHead'),
    ('ReleaseEngineer'),
    ('User')
ON CONFLICT (name) DO NOTHING;

-- ─────────────────────────────────────────────────────
-- STEP 2: REGISTER A USER Using API (with default 'User' role)
-- Email: admin@gmail.com | Password: admin123 (BCrypt)
-- Initially created as a normal 'User' role
-- ─────────────────────────────────────────────────────

-- ─────────────────────────────────────────────────────
-- STEP 3: SET THE USER'S ROLE TO ADMIN
-- Promotes admin@gmail.com → Admin role + marks as internal
-- ─────────────────────────────────────────────────────
UPDATE users
SET role_id = (SELECT id FROM roles WHERE name = 'Admin'),
    is_internal = true,
    last_modified_at = NOW()
WHERE email = 'admin@gmail.com';