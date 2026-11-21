-- name: CreateUser :exec
INSERT INTO users (id, role, email, password_hash, display_name, terms_version, terms_accepted_at)
VALUES (@id, @role, @email, @password_hash, @display_name, sqlc.narg(terms_version),
        CASE WHEN sqlc.narg(terms_version)::text IS NULL THEN NULL ELSE now() END);

-- name: CreateBusiness :exec
INSERT INTO businesses (user_id, name, category, schedule, address, latitude, longitude)
VALUES (@user_id, @name, @category, @schedule, @address, @latitude, @longitude);

-- name: GetUserByEmail :one
SELECT id, role, email, password_hash, display_name, terms_version
FROM users
WHERE lower(email) = lower(@email);

-- name: GetBusiness :one
SELECT user_id, name, category, schedule, address, latitude, longitude
FROM businesses
WHERE user_id = @user_id;
