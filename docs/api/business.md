# API · Comercios y publicaciones (propuesta para el back en Go)

## `POST /auth/signup/business`

Body: `{ "businessName", "category", "schedule", "address", "latitude", "longitude", "email", "password" }`

- **201**: misma respuesta que el login, con `role: "BUSINESS"`.
- **409**: el email ya está registrado.
- La app manda la ubicación actual del dispositivo; si no hay ubicación, manda el centro por defecto. Las ofertas del comercio se ubican ahí.

## `PUT /offers/{id}` (requiere token de comercio)

Publicar o editar. Body: `{ "title", "category", "schedule", "description" }`. **El `id` lo genera la app (UUID)**.

- Si la oferta no existe, la crea (publicar). Si existe, **reemplaza su contenido** (editar). Mandar el mismo contenido dos veces no cambia nada, así que los reintentos son seguros.
- **200 / 201**: publicada o actualizada. Queda `ACTIVE` en el mapa, con la dirección y la ubicación del comercio.
- **401**: sesión vencida. La acción queda en la cola hasta el próximo login.
- **403**: la cuenta no es de comercio, o la oferta es de otro comercio.
- **409**: la oferta está cerrada y ya no se puede editar. La app la muestra como cerrada (gana el servidor).
- El back valida todo lo demás (por ejemplo, si la posición sigue abierta). La app solo evita mandar formularios incompletos.

## `POST /offers/{id}/close` (requiere token del comercio dueño)

Cierra la búsqueda: deja de aparecer en `GET /offers` y no acepta más postulaciones. No se puede reabrir. Es idempotente.

- **200 / 204**: cerrada.
- **404**: la oferta no existe. **409**: ya estaba cerrada. En los dos casos la app la da por cerrada.
- **403**: la oferta es de otro comercio.
- Sin conexión, la app la muestra como "Cierre pendiente de envío" y la manda cuando vuelve la conexión. Los cambios guardados antes del cierre y todavía sin enviar se descartan.

## `GET /offers/mine` (requiere token de comercio)

`200 [OfferDto]` (mismo formato que `GET /offers`), en cualquier estado.

## QR del cartel

Codifica `laburin://offer/{id}`. El escáner de la app lo lee y abre el detalle de la oferta.

## `GET /offers/{id}/applicants` (requiere el token del comercio dueño de la oferta)

`200 [ { "applicationId", "offerId", "candidateName", "cvFileName", "status": "PENDING_REVIEW" | "APPROVED" | "REJECTED", "contactEmail" } ]`

- `contactEmail` viene **solo** para postulantes aprobados (RNF-05). Para el resto es `null`.
- **403**: la oferta no es de ese comercio.

## `PUT /applications/{id}/review` (requiere el token del comercio dueño)

Body: `{ "approve": true | false }`. Es idempotente.

- **200**: el postulante actualizado. Si se aprobó, **la respuesta trae el contacto**: es la confirmación del Match. La app no muestra el contacto hasta recibir esta respuesta. Sin conexión, la decisión queda como "pendiente de envío".
- El postulante ve el resultado (`APPROVED` / `REJECTED`) en `GET /applications/mine`.
