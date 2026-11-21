-- migrate:up
-- Provisional terms a candidate accepts when signing up: businesses they apply to may see their CV (contact details
-- included) until the CV can be redacted. Accounts created before have no terms and don't share their CV.
ALTER TABLE users
    ADD COLUMN terms_version     text,
    ADD COLUMN terms_accepted_at timestamptz,
    ADD CONSTRAINT users_terms_accepted_together CHECK ((terms_version IS NULL) = (terms_accepted_at IS NULL));

-- migrate:down
ALTER TABLE users
    DROP CONSTRAINT users_terms_accepted_together,
    DROP COLUMN terms_accepted_at,
    DROP COLUMN terms_version;
