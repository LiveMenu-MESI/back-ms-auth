# Guía para obtener credenciales HMAC de Google Cloud Storage

Esta guía explica cómo obtener las credenciales HMAC (Access Key ID y Secret Access Key) para autenticarse con Google Cloud Storage.

## Pasos para obtener credenciales HMAC

### 1. Crear una cuenta de servicio

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Selecciona tu proyecto (o créalo si no tienes uno)
3. Navega a **IAM & Admin** → **Service Accounts**
4. Haz clic en **+ CREATE SERVICE ACCOUNT**
5. Completa:
   - **Service account name**: `livemenu-storage` (o el nombre que prefieras)
   - **Service account ID**: se genera automáticamente
   - Haz clic en **CREATE AND CONTINUE**

### 2. Asignar roles a la cuenta de servicio

1. En **Grant this service account access to project**, asigna el rol:
   - **Storage Admin** (o al menos **Storage Object Admin** para escribir objetos)
2. Haz clic en **CONTINUE**
3. Opcional: agrega usuarios que puedan administrar esta cuenta
4. Haz clic en **DONE**

### 3. Crear clave HMAC

1. En la lista de cuentas de servicio, haz clic en la cuenta que acabas de crear
2. Ve a la pestaña **KEYS**
3. Haz clic en **ADD KEY** → **Create new key**
4. Selecciona **HMAC** como tipo de clave
5. Haz clic en **CREATE**
6. **IMPORTANTE**: Se descargará un archivo JSON o se mostrará un diálogo con:
   - **Access Key ID** (Access Key)
   - **Secret Access Key** (Secret)

### 4. Guardar las credenciales

**⚠️ IMPORTANTE**: Las credenciales HMAC solo se muestran una vez. Guárdalas de forma segura.

Copia los valores:
- **Access Key ID**: Ejemplo: `GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS`
- **Secret Access Key**: Ejemplo: `GOOG1EZXQ5HAM63NUZOE52E235LBKZ6TNQEW7LDCB64PGAZANTTVVKJ3WBBWO`

**Nota importante**: 
- El formato de las claves HMAC de GCP es `GOOG1...` (no `GOOGTS...`)
- El **Secret Access Key** debe ser el valor **raw** tal como se muestra (no codificado en Base64)
- Asegúrate de copiar ambos valores de la **misma clave HMAC**

### 5. Configurar en el proyecto

Agrega estas variables de entorno en tu archivo `.env`:

```bash
# Google Cloud Storage HMAC credentials
GCP_STORAGE_BUCKET_NAME=tu-bucket-name
GCP_STORAGE_HMAC_ACCESS_KEY=GOOGTS7C7FUP3AIRVJTE2BCN
GCP_STORAGE_HMAC_SECRET_KEY=bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ
```

### 6. Crear el bucket (si no existe)

1. Ve a **Cloud Storage** → **Buckets**
2. Haz clic en **CREATE BUCKET**
3. Configura:
   - **Name**: `livemenu-images` (o el nombre que prefieras)
   - **Location type**: Elige la región más cercana
   - **Storage class**: `Standard`
   - **Access control**: `Uniform` (recomendado) o `Fine-grained`
4. Haz clic en **CREATE**

### 7. Configurar permisos del bucket (opcional)

Si quieres que las imágenes sean públicas:

1. Ve al bucket creado
2. Haz clic en **PERMISSIONS**
3. Haz clic en **ADD PRINCIPAL**
4. Agrega:
   - **New principals**: `allUsers`
   - **Role**: `Storage Object Viewer`
5. Haz clic en **SAVE**

## Verificación

Para verificar que las credenciales funcionan:

1. Asegúrate de que las variables de entorno estén configuradas
2. Inicia la aplicación: `./mvnw quarkus:dev`
3. Intenta subir una imagen usando Postman
4. Revisa los logs para ver si la autenticación HMAC funciona

## Notas importantes

- **Seguridad**: Nunca commitees las credenciales HMAC en el repositorio
- **Rotación**: Puedes crear nuevas claves HMAC y desactivar las antiguas desde la consola
- **Permisos**: La cuenta de servicio necesita al menos el rol `Storage Object Admin` para escribir objetos
- **Secret Key**: El `GCP_STORAGE_HMAC_SECRET_KEY` debe ser el valor raw, no codificado en Base64

## Solución de problemas

### Error: SignatureDoesNotMatch
- Verifica que las credenciales sean correctas
- Asegúrate de que el Secret Key sea el valor raw (no Base64)
- Verifica que la cuenta de servicio tenga permisos en el bucket

### Error: Access Denied
- Verifica que la cuenta de servicio tenga el rol `Storage Object Admin` o `Storage Admin`
- Verifica que el bucket exista y esté accesible

### Error: Bucket not found
- Verifica que el nombre del bucket sea correcto
- Verifica que el bucket esté en el mismo proyecto de GCP

