# Notificaciones

La app tiene dos fuentes de notificaciones. Las dos producen los mismos eventos y usan el mismo `Notifier`.

1. **Push con FCM (Firebase Cloud Messaging):** es la definitiva y la manda el back.
2. **Chequeo local con WorkManager:** es la que se usa mientras no hay back. Cada ~15 minutos, con conexión, la app compara el estado de sus postulaciones (o de los postulantes del comercio) con el que ya conocía y notifica los cambios.

## Activar FCM

1. Crear un proyecto en [Firebase](https://console.firebase.google.com/) y registrar la app Android `com.example.laburin`.
2. Descargar `google-services.json` y ponerlo en `app/`. **No se commitea** (agregarlo al `.gitignore` si se comparte el repo).
3. Con el archivo presente, Gradle aplica el plugin `com.google.gms.google-services` y FCM queda activo. Sin el archivo, la app compila y funciona igual, solo con el chequeo local.

## `POST /me/push-token` (requiere token de sesión)

Body: `{ "token": "<FCM registration token>" }`. **204**. Cualquier cuenta. Si el token ya era de otro usuario, pasa al último que lo registró.

- El back en Go ya guarda el token (`push_tokens`), pero **todavía no manda mensajes**: hacen falta las credenciales de servicio de Firebase.
- Pendiente del lado de la app: `LaburinMessagingService.onNewToken` tiene que llamarlo.

## Mensajes que manda el back

Mensajes de datos (`data`) de FCM, todos los valores como string:

| `type` | Campos | A quién | Cuándo |
|---|---|---|---|
| `APPLICATION_REVIEWED` | `applicationId`, `offerTitle`, `approved` (`"true"` / `"false"`) | Postulante | El comercio aprobó o rechazó la postulación |
| `NEW_APPLICANTS` | `offerId`, `offerTitle`, `count` | Comercio dueño de la oferta | Llegaron postulantes nuevos |

Los mensajes con un `type` desconocido o incompletos se ignoran (`PushPayload.parse`).

## Permisos

En Android 13+ la app pide el permiso de notificaciones una vez, cuando el usuario inicia sesión. Sin ese permiso no se muestra nada, y la app no falla.
