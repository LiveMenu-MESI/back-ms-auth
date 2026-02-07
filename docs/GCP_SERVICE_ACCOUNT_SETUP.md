# Guía para Configurar Service Account JSON Key para Google Cloud Storage

Esta guía explica cómo obtener y configurar las credenciales de Service Account (archivo JSON) para autenticarse con Google Cloud Storage.

## Pasos para obtener credenciales de Service Account

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

### 3. Crear y descargar la clave JSON

1. En la lista de cuentas de servicio, haz clic en la cuenta que acabas de crear
2. Ve a la pestaña **KEYS**
3. Haz clic en **ADD KEY** → **Create new key**
4. Selecciona **JSON** como tipo de clave
5. Haz clic en **CREATE**
6. **IMPORTANTE**: Se descargará automáticamente un archivo JSON con las credenciales

### 4. Guardar las credenciales de forma segura

**⚠️ IMPORTANTE**: El archivo JSON contiene credenciales sensibles. Guárdalo de forma segura y nunca lo commitees en el repositorio.

El archivo JSON tiene un formato similar a:
```json
{
  "type": "service_account",
  "project_id": "tu-proyecto",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "livemenu-storage@tu-proyecto.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

### 5. Configurar en el proyecto

Tienes dos opciones para configurar las credenciales:

#### Opción 1: Variable de entorno (Recomendado)

Configura la variable de entorno `GOOGLE_APPLICATION_CREDENTIALS` apuntando al archivo JSON:

**Windows (PowerShell):**
```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\service-account-key.json"
```

**Windows (CMD):**
```cmd
set GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account-key.json
```

**Linux/Mac:**
```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
```

**En archivo `.env`:**
```bash
GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\service-account-key.json
```

#### Opción 2: Configuración en application.properties

Agrega la ruta al archivo JSON en tu archivo `.env`:

```bash
# Google Cloud Storage configuration
GCP_STORAGE_BUCKET_NAME=livemenu-images
GCP_STORAGE_CREDENTIALS_PATH=C:\path\to\service-account-key.json
```

O directamente en `application.properties` (no recomendado para producción):
```properties
gcp.storage.credentials.path=/path/to/service-account-key.json
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
3. Intenta subir una imagen usando Postman o el endpoint de prueba
4. Revisa los logs para ver si la autenticación funciona

### Endpoint de prueba

Puedes usar el endpoint de prueba para validar las credenciales:

```
GET /api/v1/test/gcp/credentials
```

Requiere autenticación con Bearer token.

## Notas importantes

- **Seguridad**: Nunca commitees el archivo JSON de credenciales en el repositorio
- **Rotación**: Puedes crear nuevas claves JSON y eliminar las antiguas desde la consola
- **Permisos**: La cuenta de servicio necesita al menos el rol `Storage Object Admin` para escribir objetos
- **Ubicación del archivo**: Coloca el archivo JSON en una ubicación segura fuera del directorio del proyecto

## Solución de problemas

### Error: "Failed to initialize GCP Storage"

**Causa**: No se puede encontrar o leer el archivo JSON de credenciales.

**Solución**:
1. Verifica que la ruta al archivo JSON sea correcta
2. Verifica que el archivo existe y es legible
3. Verifica que el archivo JSON es válido (formato correcto)

### Error: "Access Denied" o "403 Forbidden"

**Causa**: La cuenta de servicio no tiene los permisos necesarios.

**Solución**:
1. Verifica que la cuenta de servicio tiene el rol `Storage Admin` o `Storage Object Admin`
2. Verifica que el bucket existe y es accesible

### Error: "Bucket not found" o "404 Not Found"

**Causa**: El nombre del bucket es incorrecto o no existe.

**Solución**:
1. Verifica que el nombre del bucket en `GCP_STORAGE_BUCKET_NAME` es correcto
2. Verifica que el bucket existe en tu proyecto de GCP

## Comparación con HMAC

**Ventajas de Service Account JSON:**
- ✅ Más fácil de configurar
- ✅ Manejo automático de tokens y renovación
- ✅ Más seguro (no necesitas manejar claves secretas manualmente)
- ✅ Funciona mejor con la biblioteca oficial de Google Cloud

**Ventajas de HMAC:**
- ✅ Más ligero (no requiere biblioteca adicional)
- ✅ Útil para integraciones simples con HTTP directo

Para la mayoría de casos de uso, Service Account JSON es la opción recomendada.


