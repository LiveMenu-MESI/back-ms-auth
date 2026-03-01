# Certificado Let's Encrypt para HTTPS en producción

Este documento describe cómo configurar un certificado de Let's Encrypt para la API desplegada con Docker. Así el navegador confía en la conexión y no muestra avisos de certificado no válido.

## 1. Obtener el certificado en el servidor (certbot)

En el servidor donde corre Docker (y donde apunta api.livemenu.naing.co):

```bash
# Instalar certbot si no lo tienes (ej. Ubuntu/Debian)
sudo apt update && sudo apt install -y certbot

# Opción A: Si el puerto 80 está libre (o lo liberas temporalmente)
sudo certbot certonly --standalone -d api.livemenu.naing.co

# Opción B: Si usas Cloudflare u otro proxy, desafío DNS
sudo certbot certonly --manual --preferred-challenges dns -d api.livemenu.naing.co
# Certbot te pedirá crear un registro TXT en el DNS; luego sigue las instrucciones.
```

Los archivos quedan en:
- Certificado: `/etc/letsencrypt/live/api.livemenu.naing.co/fullchain.pem`
- Clave privada: `/etc/letsencrypt/live/api.livemenu.naing.co/privkey.pem`

## 2. Convertir a PKCS12 (keystore) para Quarkus

Quarkus puede usar PEM o PKCS12. Para seguir usando el mismo flujo (keystore.p12):

```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/api.livemenu.naing.co/fullchain.pem \
  -inkey /etc/letsencrypt/live/api.livemenu.naing.co/privkey.pem \
  -out /tmp/keystore.p12 \
  -name server \
  -passout pass:changeit
```

Copia el keystore a la carpeta del compose y reinicia:

```bash
sudo cp /tmp/keystore.p12 /home/tu-usuario/back-ms-auth/compose/config/keystore.p12
sudo chown tu-usuario:tu-usuario /home/tu-usuario/back-ms-auth/compose/config/keystore.p12
cd ~/back-ms-auth/compose
docker compose up -d livemenu
```

Usa la misma contraseña en `.env`: `HTTPS_KEYSTORE_PASSWORD=changeit` (o la que hayas puesto en `-passout pass:...`).

## 3. Renovación automática (Let's Encrypt caduca a los 90 días)

Certbot puede renovar solo. Añade un cron para renovar y volver a generar el keystore:

```bash
sudo crontab -e
```

Añade (ejecuta el segundo día de cada mes a las 3:00):

```
0 3 2 * * certbot renew --quiet --deploy-hook "/ruta/a/renovar-keystore.sh"
```

Crea el script `renovar-keystore.sh` (ajusta rutas y usuario):

```bash
#!/bin/bash
openssl pkcs12 -export \
  -in /etc/letsencrypt/live/api.livemenu.naing.co/fullchain.pem \
  -inkey /etc/letsencrypt/live/api.livemenu.naing.co/privkey.pem \
  -out /home/tu-usuario/back-ms-auth/compose/config/keystore.p12 \
  -name server -passout pass:changeit
chown tu-usuario:tu-usuario /home/tu-usuario/back-ms-auth/compose/config/keystore.p12
docker compose -f /home/tu-usuario/back-ms-auth/compose/docker-compose.yml restart livemenu
```

## 4. Alternativa: usar PEM directamente en Quarkus

Si prefieres no convertir a PKCS12, Quarkus puede usar los PEM. Monta los archivos en el contenedor y en `application-prod.properties` (o vía variables) apunta a ellos; el TLS registry de Quarkus acepta `key-store.pem.0.cert` y `key-store.pem.0.key`. Si quieres esta opción, se puede añadir un bloque de configuración alternativo en `application-prod.properties` para PEM.

---

**Resumen:** certificado Let's Encrypt → convertir a PKCS12 → usar como `compose/config/keystore.p12` con la misma `HTTPS_KEYSTORE_PASSWORD` → el navegador confiará en la conexión.
