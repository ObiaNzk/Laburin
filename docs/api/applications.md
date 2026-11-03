# API · Postulaciones y CV (propuesta para el back en Go)

Todos los endpoints requieren `Authorization: Bearer <token>`. Un **401** significa sesión vencida: la app conserva las acciones pendientes y las reenvía cuando el usuario vuelve a ingresar.

## `PUT /applications/{id}`

Postularse. Body: `{ "offerId": "…" }`. **El `id` lo genera la app (UUID)**, así un reintento no duplica la postulación.

- **200 / 201**: aceptada. Reenviar el mismo `id` es un no-op.
- **409**: la búsqueda está cerrada o no existe. La app la marca "Búsqueda cerrada" y no reintenta.
- **5xx / sin red**: la app reintenta con backoff exponencial (WorkManager, solo con conexión).

## `GET /applications/mine`

`200 [ { "id", "offerId", "offerTitle", "businessName", "status": "SENT" | "APPROVED" | "REJECTED" | "OFFER_CLOSED", "createdAt": ISO-8601 } ]`

Gana el servidor, salvo para las postulaciones que la app todavía no pudo enviar ("Pendiente de envío").

## `PUT /me/cv`

Sube el CV (`multipart/form-data` con un PDF de 5 MB como máximo). La app valida el tipo y la firma `%PDF` antes de mandarlo.

## `GET /me/cv`

`200 { "fileName": "…" }`, o `404` si el usuario todavía no cargó un CV.

## Reglas

- Para postularse hacen falta una cuenta de postulante y el CV cargado (RF-02).
- Una postulación por oferta y por usuario.
