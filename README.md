# liveMenu (Backend) — Despliegue en producción

API REST del proyecto LiveMenu: autenticación (Keycloak/OIDC), restaurantes, categorías, platos, menú público, analytics y subida de imágenes (GCP opcional). Desarrollado con **Quarkus**. Este README describe el **despliegue en producción** con Docker Compose, HTTPS y Keycloak.

---

## Índice

- [Requisitos del servidor](#requisitos-del-servidor)
- [Despliegue con Docker Compose](#despliegue-con-docker-compose)
- [Variables de entorno (producción)](#variables-de-entorno-producción)
- [HTTPS y certificados](#https-y-certificados)
- [Keycloak y login](#keycloak-y-login)
- [CORS](#cors)
- [GCP Storage (opcional)](#gcp-storage-opcional)
- [Mantenimiento y troubleshooting](#mantenimiento-y-troubleshooting)
- [Desarrollo local](#desarrollo-local)
- [Documentación de API (OpenAPI)](#documentación-de-api-openapi)
- [Documentación adicional](#documentación-adicional)

---

## Requisitos del servidor

- **Docker** y **Docker Compose** (v2)
- Puertos disponibles: **5432** (Postgres), **8180** y **8443** (Keycloak), **8081** y **8444** (livemenu; o los que definas en `.env`)
- Para HTTPS con Let's Encrypt: **certbot** en el host y (opcionalmente) puerto **80** para el desafío HTTP

---

## Despliegue con Docker Compose

### 1. Clonar / copiar el proyecto en el servidor

```bash
# Ejemplo: el proyecto está en ~/back-ms-auth (o tu ruta)
cd ~/back-ms-auth
```

### 2. Configurar variables de entorno

```bash
cd compose
cp .env.example .env
# Editar .env con valores de producción (ver sección siguiente)
```

### 3. Certificado HTTPS (recomendado en producción)

Para que el navegador no muestre avisos de certificado inválido, usa **Let's Encrypt**:

- Obtener certificado en el servidor (certbot).
- Convertir a PKCS12 y colocar el keystore en `compose/config/keystore.p12`.

Guía completa: **[compose/HTTPS-LETSENCRYPT.md](compose/HTTPS-LETSENCRYPT.md)**.

Si solo necesitas HTTPS de prueba (aviso en el navegador):

```bash
cd compose/scripts
KEYSTORE_DNS=*.tudominio.co ./gen-keystore.sh
# Copiar compose/config/keystore.p12 si no se generó ahí
```

### 4. Levantar los servicios

```bash
cd compose
docker compose build livemenu --no-cache   # primera vez o tras cambiar código
docker compose up -d
```

- **Postgres**: puerto `DB_PORT` (ej. 5432).
- **Keycloak**: HTTP **8180**, HTTPS **8443**.
- **livemenu**: HTTP **8081**, HTTPS **8444** (por defecto; configurables con `LIVEMENU_PORT` y `LIVEMENU_HTTPS_PORT`).

### 5. Comprobar que todo responde

```bash
docker compose ps
curl -k https://localhost:8444/q/health   # health de livemenu (HTTPS)
```

---

## Variables de entorno (producción)

Todas las variables sensibles se configuran en **`compose/.env`** (no versionado). Referencia: `compose/.env.example`.

### Base de datos

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `POSTGRES_DB` | Base de datos Postgres | `postgres` |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Usuario y contraseña de Postgres | — |
| `DB_PORT` | Puerto expuesto de Postgres | `5432` |
| `DB_URL` | JDBC URL para livemenu | `jdbc:postgresql://postgres:5432/livemenu` |
| `DB_USER` / `DB_PASSWORD` | Usuario y contraseña de la app | `livemenu` / secreto |

### Keycloak

| Variable | Descripción | Producción (Docker) |
|----------|-------------|----------------------|
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | Admin del realm master | Valores seguros |
| `KC_HOSTNAME` | Hostname que anuncia Keycloak en el discovery | **`keycloak`** |
| `KC_HOSTNAME_PORT` | Puerto HTTPS de Keycloak | **`8443`** |
| `KEYCLOAK_URL` | URL de Keycloak usada por livemenu | **`https://keycloak:8443`** |
| `OIDC_CLIENT_SECRET` | Secreto del cliente OIDC (realm livemenu) | Coincidir con el realm |
| `KEYCLOAK_ADMIN_CLIENT_SECRET` | Secreto del cliente admin (registro de usuarios) | Coincidir con el realm |

**Importante:** En Docker, `KEYCLOAK_URL` y `KC_HOSTNAME=keycloak` son necesarios para que el login funcione (evitar timeouts). Ver [compose/CONFIG-LOGIN-DOCKER.md](compose/CONFIG-LOGIN-DOCKER.md).

### HTTPS (livemenu)

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `HTTPS_KEYSTORE_FILE` | Ruta al keystore PKCS12 dentro del contenedor | `/app/config/keystore.p12` |
| `HTTPS_KEYSTORE_PASSWORD` | Contraseña del keystore | La usada al generar/convertir el cert |
| `HTTPS_PORT` | Puerto HTTPS dentro del contenedor | `8443` |
| `LIVEMENU_HTTPS_PORT` | Puerto HTTPS en el host | `8444` |
| `HTTPS_INSECURE_REQUESTS` | Comportamiento del puerto HTTP | `enabled` (HTTP+HTTPS) o `redirect` |

### Otros

| Variable | Descripción |
|----------|-------------|
| `HIBERNATE_SCHEMA_STRATEGY` | Estrategia de schema (prod: `update`) |
| `GCP_STORAGE_BUCKET_NAME` | Bucket GCP para imágenes (opcional) |
| `GOOGLE_APPLICATION_CREDENTIALS` | Ruta al JSON de cuenta de servicio (opcional) |

---

## HTTPS y certificados

- **Contenedor:** livemenu escucha HTTP en **8080** y HTTPS en **8443**.
- **Host:** el Compose publica HTTP en **8081** y HTTPS en **8444** por defecto.

En producción se recomienda:

1. **Certificado Let's Encrypt** (gratuito, confiable en navegadores): ver **[compose/HTTPS-LETSENCRYPT.md](compose/HTTPS-LETSENCRYPT.md)** (obtención, conversión a PKCS12, renovación con cron).
2. **Keystore** en `compose/config/keystore.p12` y montado en el contenedor (el `docker-compose.yml` ya define el volumen).
3. **Perfil prod:** la app usa `QUARKUS_PROFILE=prod` en Compose para servir HTTPS con TLS 1.2 y 1.3 (evita `ERR_SSL_VERSION_OR_CIPHER_MISMATCH`).

Si usas **proxy inverso** (nginx, Cloudflare, etc.) que termina TLS en el borde, puedes exponer solo HTTP del contenedor y dejar que el proxy gestione el certificado.

---

## Keycloak y login

- **Registro de usuarios:** el backend usa el usuario admin de Keycloak (realm `master`). Deben estar configurados `KEYCLOAK_ADMIN` y `KEYCLOAK_ADMIN_PASSWORD` en `.env`.
- **Login / refresh:** flujo OIDC (password grant) contra Keycloak. En Docker, `KEYCLOAK_URL=https://keycloak:8443` y `KC_HOSTNAME=keycloak` son **obligatorios** para que el cliente OIDC alcance el endpoint de token sin timeout.

Si el login falla (timeout, "OIDC server not available"), revisar **[compose/CONFIG-LOGIN-DOCKER.md](compose/CONFIG-LOGIN-DOCKER.md)**.

---

## CORS

Los orígenes permitidos están en `application.properties` (`quarkus.http.cors.origins`). Por defecto se incluyen orígenes como:

- `https://livemenu.naing.co:4443`, `https://livemenu.naing.co`, `https://www.livemenu.naing.co`
- `https://api.livemenu.naing.co`, `http://api.naing.co`
- `http://localhost:4200`, `http://localhost:8080`

Si tu frontend en producción usa otra URL (origen = esquema + host + puerto), hay que **añadirla** en `quarkus.http.cors.origins`, **reconstruir** la imagen de livemenu y **reiniciar** el servicio.

---

## GCP Storage (opcional)

Para subida de imágenes (CU-05) con Google Cloud Storage:

- Configurar `GCP_STORAGE_BUCKET_NAME` y el archivo de credenciales (montado como `GOOGLE_APPLICATION_CREDENTIALS` en el contenedor).
- Guía: **[docs/GCP_HMAC_SETUP.md](docs/GCP_HMAC_SETUP.md)**.

Si no se configuran, la API arranca igual; las operaciones de imagen no usarán GCP.

---

## Mantenimiento y troubleshooting

### Reiniciar solo livemenu

```bash
cd compose
docker compose restart livemenu
```

### Reconstruir y levantar livemenu tras cambios de código

```bash
cd compose
docker compose build livemenu --no-cache
docker compose up -d livemenu
```

### Ver logs

```bash
docker compose logs -f livemenu
docker compose logs -f keycloak
```

### Rate limiting

Límite por IP: **100 peticiones/minuto** (configurable con `RATE_LIMIT_REQUESTS_PER_MINUTE`). Respuesta al superar: **429 Too Many Requests**.

### Documentación de incidencias

| Problema | Documento |
|----------|-----------|
| Login timeout / OIDC no disponible en Docker | [compose/CONFIG-LOGIN-DOCKER.md](compose/CONFIG-LOGIN-DOCKER.md) |
| Certificado no confiable (ERR_CERT_AUTHORITY_INVALID) | [compose/HTTPS-LETSENCRYPT.md](compose/HTTPS-LETSENCRYPT.md) |
| ERR_SSL_VERSION_OR_CIPHER_MISMATCH | Usar perfil `prod` y TLS registry (TLS 1.2/1.3); ver sección HTTPS |

---

## Desarrollo local

Para desarrollar en local sin desplegar en producción:

1. En `compose`: `cp .env.example .env`, configurar al menos Postgres, Keycloak y OIDC.
2. Levantar solo infra: `docker compose up -d postgres keycloak`.
3. En la raíz del proyecto: `./mvnw quarkus:dev`.
4. Probar auth con la colección Postman en `postman/Auth-LiveMenu.postman_collection.json`.

En local suele usarse `KEYCLOAK_URL=https://localhost:8443` (o la IP del host) y `KC_HOSTNAME=localhost` si accedes a Keycloak desde el navegador.

---

## Documentación de API (OpenAPI)

La API expone documentación **OpenAPI 3** y **Swagger UI** generadas a partir de los recursos JAX-RS:

| Entorno | OpenAPI (JSON) | Swagger UI |
|---------|----------------|------------|
| Local (HTTP) | http://localhost:8080/q/openapi | http://localhost:8080/q/swagger-ui |
| Producción (HTTPS) | https://tu-dominio:8444/q/openapi | https://tu-dominio:8444/q/swagger-ui |

En desarrollo (`./mvnw quarkus:dev`) están disponibles por defecto. Para producción puedes restringir el acceso a `/q/openapi` y `/q/swagger-ui` por firewall o proxy si no quieres exponerlos.

---

## Documentación adicional

Documentos en el repositorio para configuración y troubleshooting en producción:

### [compose/CONFIG-LOGIN-DOCKER.md](compose/CONFIG-LOGIN-DOCKER.md)

**Login y Keycloak en Docker.**  
Explica por qué el login puede hacer timeout cuando livemenu y Keycloak corren en Compose: el discovery devuelve la URL pública (p. ej. IP) y el contenedor no puede alcanzarla. Incluye:

- Uso de `KEYCLOAK_URL=https://keycloak:8443` y `KC_HOSTNAME=keycloak` para que el token endpoint sea accesible desde livemenu.
- Checklist de variables y comprobaciones.
- Sección de **certificado HTTPS** para el API (keystore con `*.dominio` y sin localhost).

### [compose/HTTPS-LETSENCRYPT.md](compose/HTTPS-LETSENCRYPT.md)

**Certificado Let's Encrypt para HTTPS.**  
Pasos para que el navegador confíe en la API (evitar `ERR_CERT_AUTHORITY_INVALID`):

- Instalación de certbot y obtención del certificado (standalone o desafío DNS).
- Conversión del certificado a PKCS12 (keystore) para Quarkus.
- Sustitución de `compose/config/keystore.p12` y reinicio de livemenu.
- **Renovación automática** con cron y script de deploy (renovar cert + regenerar keystore + reiniciar contenedor).

### [docs/GCP_SETUP.md](docs/GCP_SETUP.md)

**Google Cloud Storage (subida de imágenes).**  
Configuración de GCP Storage con cuenta de servicio para la funcionalidad de imágenes (CU-05): cuenta de servicio, bucket y variables de entorno (`GCP_STORAGE_BUCKET_NAME`, `GOOGLE_APPLICATION_CREDENTIALS`). Opcional; la API arranca sin GCP si no se configuran.

### [docs/HIBERNATE.md](docs/HIBERNATE.md)

**Hibernate ORM y persistencia.**  
Configuración del datasource, estrategia de esquema (`HIBERNATE_SCHEMA_STRATEGY`), entidades (Restaurant, Category, Dish, MenuView, DishView), relaciones y uso de JSON/JSONB. Incluye notas sobre migraciones versionadas (Flyway/Liquibase) para producción.

### Scripts de utilidad

| Script | Uso |
|--------|-----|
| `compose/scripts/gen-keystore.sh` | Genera keystore PKCS12 autofirmado. Ej.: `KEYSTORE_DNS=*.naing.co ./gen-keystore.sh`. |
| `compose/scripts/gen-keystore.bat` | Misma funcionalidad en Windows. |

### Referencia de configuración

| Archivo | Descripción |
|---------|-------------|
| `compose/.env.example` | Plantilla de variables para el Compose (copiar a `.env` y rellenar). |
| `compose/docker-compose.yml` | Definición de servicios Postgres, Keycloak y livemenu. |
| `src/main/resources/application.properties` | Configuración de la app (BD, Keycloak, HTTPS, CORS, GCP, OpenAPI). |
| `src/main/resources/application-prod.properties` | Overrides en perfil prod (TLS registry para HTTPS con TLS 1.2/1.3). |
| `docs/HIBERNATE.md` | Documentación de Hibernate: esquema, entidades y estrategia de BD. |

---

Quarkus: <https://quarkus.io/>.
