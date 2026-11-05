-- name: GetOffer :one
SELECT o.id, o.business_id, o.title, o.category, o.schedule, o.description, o.job_type,
       o.salary_min, o.salary_max, o.salary_period, o.status, o.updated_at,
       b.name AS business_name, b.address, b.latitude, b.longitude
FROM offers o
JOIN businesses b ON b.user_id = o.business_id
WHERE o.id = @id;

-- name: ListActiveOffersByIDs :many
SELECT o.id, o.business_id, o.title, o.category, o.schedule, o.description, o.job_type,
       o.salary_min, o.salary_max, o.salary_period, o.status, o.updated_at,
       b.name AS business_name, b.address, b.latitude, b.longitude
FROM offers o
JOIN businesses b ON b.user_id = o.business_id
WHERE o.id = ANY(@ids::uuid[])
  AND o.status = 'ACTIVE'
  AND (sqlc.narg(category)::offer_category IS NULL OR o.category = sqlc.narg(category)::offer_category);

-- name: ListBusinessOffers :many
SELECT o.id, o.business_id, o.title, o.category, o.schedule, o.description, o.job_type,
       o.salary_min, o.salary_max, o.salary_period, o.status, o.updated_at,
       b.name AS business_name, b.address, b.latitude, b.longitude
FROM offers o
JOIN businesses b ON b.user_id = o.business_id
WHERE o.business_id = @business_id
ORDER BY o.updated_at DESC;

-- name: ListActiveOfferLocations :many
SELECT o.id, b.latitude, b.longitude
FROM offers o
JOIN businesses b ON b.user_id = o.business_id
WHERE o.status = 'ACTIVE';

-- name: CreateOffer :exec
INSERT INTO offers (id, business_id, title, category, schedule, description, salary_min, salary_max, salary_period)
VALUES (@id, @business_id, @title, @category, @schedule, @description,
        sqlc.narg(salary_min), sqlc.narg(salary_max), sqlc.narg(salary_period));

-- name: UpdateOfferContent :exec
UPDATE offers
SET title = @title, category = @category, schedule = @schedule, description = @description,
    salary_min = sqlc.narg(salary_min), salary_max = sqlc.narg(salary_max), salary_period = sqlc.narg(salary_period),
    updated_at = now()
WHERE id = @id;

-- name: CloseOffer :exec
UPDATE offers SET status = 'CLOSED', updated_at = now() WHERE id = @id AND status = 'ACTIVE';
