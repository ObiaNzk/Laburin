-- name: CreateUser :exec
INSERT INTO users (id, role, email, password_hash, display_name)
VALUES (@id, @role, @email, @password_hash, @display_name);

-- name: CreateBusiness :exec
INSERT INTO businesses (user_id, name, category, schedule, address, latitude, longitude)
VALUES (@user_id, @name, @category, @schedule, @address, @latitude, @longitude);

-- name: GetUserByEmail :one
SELECT id, role, email, password_hash, display_name
FROM users
WHERE lower(email) = lower(@email);

-- name: GetBusiness :one
SELECT user_id, name, category, schedule, address, latitude, longitude
FROM businesses
WHERE user_id = @user_id;
