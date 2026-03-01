# Google Cloud Storage — configuración para subida de imágenes (CU-05)

La API usa **Google Cloud Storage** para guardar imágenes de platos y categorías. La autenticación se hace con una **cuenta de servicio** y un archivo JSON de credenciales (no con claves HMAC en el código actual). Este documento describe cómo crear la cuenta de servicio, el bucket y las variables de entorno.

La funcionalidad es **opcional**: si no configuras GCP, la API arranca igual; las operaciones de subida de imagen fallarán hasta que configures bucket y credenciales.

---

## 1. Crear proyecto y habilitar Storage (si no lo tienes)

1. Entra en [Google Cloud Console](https://console.cloud.google.com/).
2. Crea un proyecto o selecciona uno existente.
3. Activa la API de Cloud Storage:
   - Menú **APIs y servicios** → **Biblioteca**.
   - Busca **Cloud Storage API** y actívala.

---

## 2. Crear bucket

1. **Storage** → **Buckets** → **Crear bucket**.
2. Nombre del bucket (ej. `livemenu-images-prod`). Debe ser único a nivel global.
3. Elige región (o multirregión) según dónde estén tus usuarios.
4. Control de acceso: en producción suele usarse **Uniform** y luego hacer públicos solo los objetos que quieras (p. ej. con política por prefijo).
5. Crea el bucket.

Para que las URLs devueltas por la API sean accesibles desde el navegador, puedes:

- **Opción A:** Dejar el bucket privado y usar URLs firmadas (requeriría cambios en el código).
- **Opción B:** Hacer públicos los objetos del bucket (p. ej. política en `gs://TU_BUCKET/images/*` con `allUsers` y rol `Object Viewer`). La aplicación actual devuelve URLs públicas del tipo `https://storage.googleapis.com/TU_BUCKET/ruta/objeto`.

---

## 3. Cuenta de servicio y clave JSON

1. **IAM y administración** → **Cuentas de servicio** → **Crear cuenta de servicio**.
2. Nombre (ej. `livemenu-storage`) y opcionalmente descripción.
3. En **Conceder acceso al proyecto**, asigna el rol **Cloud Storage** → **Objeto de almacenamiento de Cloud Storage** (o un rol que incluya `storage.objects.create`, `storage.objects.delete`, `storage.objects.get`).
4. Crea la cuenta.
5. En la lista, abre la cuenta → pestaña **Claves** → **Agregar clave** → **Crear clave nueva** → **JSON**. Se descargará un archivo `.json`.

Guarda ese JSON en un lugar seguro (por ejemplo en el servidor donde corre livemenu, fuera del repositorio). La aplicación espera la ruta a este archivo en la variable de entorno `GOOGLE_APPLICATION_CREDENTIALS`.

---

## 4. Variables de entorno

La aplicación lee:

| Variable | Uso |
|----------|-----|
| `GCP_STORAGE_BUCKET_NAME` | Nombre del bucket (ej. `livemenu-images-prod`). |
| `GOOGLE_APPLICATION_CREDENTIALS` | Ruta absoluta al archivo JSON de la cuenta de servicio. |

En **Docker Compose** (ej. en `compose/.env` o en el servicio `livemenu`):

```env
GCP_STORAGE_BUCKET_NAME=livemenu-images-prod
GOOGLE_APPLICATION_CREDENTIALS=/app/config/gcp-service-account.json
```

Monta el archivo JSON en el contenedor, por ejemplo en `docker-compose.yml`:

```yaml
livemenu:
  environment:
    GCP_STORAGE_BUCKET_NAME: ${GCP_STORAGE_BUCKET_NAME}
    GOOGLE_APPLICATION_CREDENTIALS: /app/config/gcp-service-account.json
  volumes:
    - ./config/gcp-service-account.json:/app/config/gcp-service-account.json:ro
```

No subas el JSON al repositorio; usa `.gitignore` y copia el archivo en el servidor de forma segura.

---

## 5. Comprobar

1. Arranca la aplicación con las variables anteriores.
2. Sube una imagen desde el cliente o con una petición multipart al endpoint de imágenes.
3. Revisa los logs: debería aparecer un mensaje de subida correcta a GCS y la URL pública del objeto.

---
