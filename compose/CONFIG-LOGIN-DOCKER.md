# Login y Keycloak en Docker

Este documento describe la configuración necesaria para que el login (OIDC contra Keycloak) funcione cuando livemenu y Keycloak se ejecutan con Docker Compose.

## Problema típico

Cuando Keycloak anuncia en su discovery una URL basada en la IP del host (p. ej. `https://192.168.1.4:8443`), el contenedor de livemenu intenta llamar al endpoint de token en esa URL. Desde dentro de la red Docker esa IP puede no ser alcanzable o provocar timeouts.

**Solución:** hacer que Keycloak anuncie la URL interna (nombre del servicio) en el discovery, de modo que livemenu use `https://keycloak:8443` también para el token.

## Configuración recomendada

### 1. Variables en el `.env` del compose

Usar el hostname del servicio Keycloak (nombre interno de la red Docker):

```env
KEYCLOAK_URL=https://keycloak:8443
KC_HOSTNAME=keycloak
KC_HOSTNAME_PORT=8443
```

No usar aquí la IP pública ni el hostname público de Keycloak para `KC_HOSTNAME`, o el discovery devolverá URLs que el contenedor livemenu no puede alcanzar.

### 2. Reiniciar servicios

Tras cambiar el `.env`:

```bash
cd /ruta/al/compose
docker compose up -d keycloak livemenu
```

### 3. Comprobar el discovery

En el host donde corre Docker:

```bash
curl -sk https://localhost:8443/realms/livemenu/.well-known/openid-configuration | grep -o '"issuer":"[^"]*"'
```

Debe aparecer: `"issuer":"https://keycloak:8443/realms/livemenu"`.

### 4. Login desde el navegador

Si el login es solo usuario/contraseña contra el backend (password grant), con la configuración anterior suele ser suficiente.

Si el flujo incluye redirección del navegador a Keycloak, con `KC_HOSTNAME=keycloak` la URL de redirección sería `https://keycloak:8443`, que el navegador no resuelve. En ese caso hace falta un proxy inverso que exponga Keycloak por el host o dominio público y mantenga el backend usando `KEYCLOAK_URL=https://keycloak:8443`.

## Checklist

| Dónde | Comprobar |
|-------|-----------|
| `.env` (compose) | `KEYCLOAK_URL=https://keycloak:8443` |
| `.env` (compose) | `KC_HOSTNAME=keycloak`, `KC_HOSTNAME_PORT=8443` |
| Contenedor livemenu | `docker compose exec livemenu env \| grep KEYCLOAK_URL` → `https://keycloak:8443` |
| Keycloak | Escucha en 8443: `docker logs keycloak \| tail` |
| Tras cambios | `docker compose up -d keycloak livemenu` |

## Certificado HTTPS del API

Si al acceder al API por HTTPS (p. ej. `https://api.tudominio.com:8444`) el navegador o el cliente muestran error de certificado (SSL handshake), el keystore del API debe incluir el dominio en el SAN (Subject Alternative Name).

Con los scripts incluidos en `compose/scripts/` se puede generar un keystore autofirmado solo con el dominio (sin localhost):

**Linux/Mac:**

```bash
KEYSTORE_DNS=*.tudominio.com ./gen-keystore.sh
# Incluir también el dominio raíz:
KEYSTORE_DNS=*.tudominio.com,tudominio.com ./gen-keystore.sh
```

**Windows:**

```cmd
set KEYSTORE_DNS=*.tudominio.com
gen-keystore.bat
```

Se genera `compose/config/keystore.p12`. La contraseña por defecto es `changeit` (o la definida en `HTTPS_KEYSTORE_PASSWORD`). Reiniciar livemenu tras sustituir el keystore.

Para que el navegador confíe sin avisos, conviene usar un certificado firmado por una CA pública; ver [HTTPS-LETSENCRYPT.md](HTTPS-LETSENCRYPT.md).
