-- name: UpsertCV :exec
INSERT INTO cvs (user_id, file_name, content)
VALUES (@user_id, @file_name, @content)
ON CONFLICT (user_id) DO UPDATE SET file_name = EXCLUDED.file_name, content = EXCLUDED.content, uploaded_at = now();

-- name: GetCVFileName :one
SELECT file_name FROM cvs WHERE user_id = @user_id;

-- name: GetCV :one
SELECT file_name, content FROM cvs WHERE user_id = @user_id;

-- name: UpsertPushToken :exec
INSERT INTO push_tokens (token, user_id)
VALUES (@token, @user_id)
ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, updated_at = now();
