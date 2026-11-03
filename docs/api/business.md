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
