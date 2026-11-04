# API · Autenticación

Mientras la API no exista, la app usa `FakeBackend` (en memoria), que respeta este contrato. Cuentas demo ficticias: `postulante@demo.laburin` y `comercio@demo.laburin`, contraseña `demo1234`.

## `POST /auth/login`

Body: `{ "email": "…", "password": "…" }`

- **200**: `{ "token": "…", "user": { "id": "…", "role": "CANDIDATE" | "BUSINESS", "displayName": "…", "email": "…" } }`
- **401**: credenciales incorrectas.

## `POST /auth/signup/candidate`

Body: `{ "firstName": "…", "lastName": "…", "email": "…", "password": "…" }` (contraseña de 8 caracteres como mínimo; la app ya lo valida).

- **201**: misma respuesta que el login, porque el usuario queda logueado.
- **409**: ya existe una cuenta con ese email (el email no distingue mayúsculas).
- **400**: faltan campos, el email no es válido o la contraseña tiene menos de 8 caracteres.

## Reglas que la app asume

- Registro e inicio de sesión requieren conexión. Después, la sesión funciona sin conexión.
- La app guarda el token cifrado con el Android Keystore (RNF-09) y lo manda como `Authorization: Bearer <token>` en los endpoints privados.
- Un `role` desconocido se trata como error del servidor; la app no abre sesión.
