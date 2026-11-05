# API · Postulaciones y CV

Todos los endpoints requieren `Authorization: Bearer <token>`. Un **401** significa sesión vencida: la app conserva las acciones pendientes y las reenvía cuando el usuario vuelve a ingresar.

## `PUT /applications/{id}`

Postularse. Body: `{ "offerId": "…" }`. **El `id` lo genera la app (UUID)**, así un reintento no duplica la postulación.

- **200 / 201**: aceptada. Reenviar el mismo `id` es un no-op.
- **409**: la búsqueda está cerrada o no existe, o ese `id` ya lo usó otra postulación. La app la marca "Búsqueda cerrada" y no reintenta.
- **422**: el postulante todavía no cargó el CV. La app lo controla antes de encolar, así que no debería pasar; si pasa, la app lo trata como un error a reintentar.
- **400**: el `id` o el `offerId` no son UUID.
- Si el postulante ya se había postulado a esa oferta con otro `id`, el back responde **200**: la postulación anterior es la que cuenta.
- **5xx / sin red**: la app reintenta con backoff exponencial (WorkManager, solo con conexión).

## `GET /applications/mine`

`OFFER_CLOSED` lo calcula el back: una postulación enviada (todavía sin revisar) a una búsqueda que se cerró.

`200 [ { "id", "offerId", "offerTitle", "businessName", "status": "SENT" | "APPROVED" | "REJECTED" | "OFFER_CLOSED", "createdAt": ISO-8601 } ]`

Gana el servidor, salvo para las postulaciones que la app todavía no pudo enviar ("Pendiente de envío").

## `PUT /me/cv`

Sube el CV (`multipart/form-data`, parte `file`, un PDF de 5 MB como máximo). Reemplaza el anterior. La app valida el tipo y la firma `%PDF` antes de mandarlo, y el back vuelve a validar la firma y el tamaño.

- **200** `{ "fileName": "…" }`. **400**: no es un PDF, pesa más de 5 MB o falta la parte `file`.

## `GET /me/cv`

`200 { "fileName": "…" }`, o `404` si el usuario todavía no cargó un CV.

## Reglas

- Para postularse hacen falta una cuenta de postulante y el CV cargado (RF-02).
- Una postulación por oferta y por usuario.
