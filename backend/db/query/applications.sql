-- name: GetApplication :one
SELECT id, offer_id, candidate_id, status, created_at
FROM applications
WHERE id = @id;

-- name: CreateApplication :exec
INSERT INTO applications (id, offer_id, candidate_id) VALUES (@id, @offer_id, @candidate_id);

-- name: ListCandidateApplications :many
SELECT a.id, a.offer_id, a.status, a.created_at,
       o.title AS offer_title, o.status AS offer_status, b.name AS business_name
FROM applications a
JOIN offers o ON o.id = a.offer_id
JOIN businesses b ON b.user_id = o.business_id
WHERE a.candidate_id = @candidate_id
ORDER BY a.created_at DESC;

-- name: ListApplicants :many
SELECT a.id, a.offer_id, a.candidate_id, a.status, u.display_name, u.email, c.file_name AS cv_file_name, o.business_id
FROM applications a
JOIN users u ON u.id = a.candidate_id
JOIN offers o ON o.id = a.offer_id
LEFT JOIN cvs c ON c.user_id = a.candidate_id
WHERE a.offer_id = @offer_id
ORDER BY a.created_at;

-- name: GetApplicant :one
SELECT a.id, a.offer_id, a.candidate_id, a.status, u.display_name, u.email, c.file_name AS cv_file_name, o.business_id
FROM applications a
JOIN users u ON u.id = a.candidate_id
JOIN offers o ON o.id = a.offer_id
LEFT JOIN cvs c ON c.user_id = a.candidate_id
WHERE a.id = @id;

-- name: UpdateApplicationStatus :exec
UPDATE applications SET status = @status, reviewed_at = now() WHERE id = @id;
