# Configuración de Credenciales HMAC para GCP Storage

## Credenciales que necesitas

Para usar HMAC con GCP Storage, necesitas **dos valores** de la misma clave HMAC:

1. **Access Key ID** (Clave de acceso): Ejemplo: `GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS`
2. **Secret Access Key** (Clave secreta): Ejemplo: `GOOG1EZXQ5HAM63NUZOE52E235LBKZ6TNQEW7LDCB64PGAZANTTVVKJ3WBBWO`

## ⚠️ Importante sobre el Secret Key

El **Secret Access Key** debe ser el valor **raw** (sin codificar). Cuando GCP te muestra la clave, puede mostrarla de diferentes formas:
- Si te muestra un valor que empieza con `GOOG1...`, ese es el valor raw que debes usar
- **NO** debes codificarlo en Base64
- **NO** debes hacer ningún procesamiento adicional

## Configuración en el proyecto

Agrega estas variables de entorno en tu archivo `.env`:

```bash
# Google Cloud Storage HMAC credentials
GCP_STORAGE_BUCKET_NAME=livemenu-images
GCP_STORAGE_HMAC_ACCESS_KEY=GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS
GCP_STORAGE_HMAC_SECRET_KEY=GOOG1EZXQ5HAM63NUZOE52E235LBKZ6TNQEW7LDCB64PGAZANTTVVKJ3WBBWO
```

## Verificación

1. Asegúrate de que la cuenta de servicio `livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com` tenga:
   - Rol: **Storage Admin** o **Storage Object Admin**
   - Permisos en el bucket `livemenu-images`

2. Verifica que el bucket exista y sea accesible

3. Inicia la aplicación y revisa los logs cuando intentes subir una imagen

## Solución de problemas

### Error: SignatureDoesNotMatch
- Verifica que el **Secret Key** sea el valor raw (no Base64)
- Verifica que ambas credenciales sean de la misma clave HMAC
- Verifica que las credenciales no tengan espacios o caracteres extra

### Error: Access Denied
- Verifica que la cuenta de servicio tenga permisos en el bucket
- Verifica que el bucket exista y esté en el proyecto correcto

