# API · Autenticación

Mientras la API no exista, la app usa `FakeBackend` (en memoria), que respeta este contrato. Cuentas demo ficticias: `postulante@demo.laburin` y `comercio@demo.laburin`, contraseña `demo1234`.

## `POST /auth/login`

Body: `{ "email": "…", "password": "…" }`

- **200**: `{ "token": "…", "user": { "id": "…", "role": "CANDIDATE" | "BUSINESS", "displayName": "…", "email": "…" } }`
- **401**: credenciales incorrectas.

## `POST /auth/signup/candidate`

Body: `{ "firstName": "…", "lastName": "…", "email": "…", "password": "…", "acceptedTerms": "2026-11-cv-v1" }` (contraseña de 8 caracteres como mínimo; la app ya lo valida).

`acceptedTerms` es la versión de los términos y condiciones que la persona aceptó en el registro. Tiene que ser la vigente (`2026-11-cv-v1`). El back guarda la versión y la fecha. **Provisorio:** al aceptarlos, el postulante permite que los comercios a los que se postule vean su CV, incluidos los datos de contacto que tenga, hasta que exista la ofuscación de datos personales del PDF.

- **201**: misma respuesta que el login, porque el usuario queda logueado.
- **409**: ya existe una cuenta con ese email (el email no distingue mayúsculas).
- **400**: faltan campos, el email no es válido, la contraseña tiene menos de 8 caracteres o no se aceptaron los términos vigentes.

## Reglas que la app asume

- Registro e inicio de sesión requieren conexión. Después, la sesión funciona sin conexión.
- La app guarda el token cifrado con el Android Keystore (RNF-09) y lo manda como `Authorization: Bearer <token>` en los endpoints privados.
- Un `role` desconocido se trata como error del servidor; la app no abre sesión.
