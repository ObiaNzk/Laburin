# API · Comercios y publicaciones (propuesta para el back en Go)

## `POST /auth/signup/business`

Body: `{ "businessName", "category", "schedule", "address", "latitude", "longitude", "email", "password" }`

- **201**: misma respuesta que el login, con `role: "BUSINESS"`.
- **409**: el email ya está registrado.
- La app manda la ubicación actual del dispositivo; si no hay ubicación, manda el centro por defecto. Las ofertas del comercio se ubican ahí.

## `PUT /offers/{id}` (requiere token de comercio)

Publicar. Body: `{ "title", "category", "schedule", "description" }`. **El `id` lo genera la app (UUID)**: reenviarlo es un no-op.

- **200 / 201**: publicada. Queda `ACTIVE` en el mapa, con la dirección y la ubicación del comercio.
- **401**: sesión vencida. La publicación queda en la cola hasta el próximo login.
- **403**: la cuenta no es de comercio.

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
