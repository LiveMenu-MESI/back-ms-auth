# Cómo Generar el Archivo JSON de Service Account para GCP

Esta guía te muestra paso a paso cómo generar el archivo JSON de credenciales de Service Account para Google Cloud Storage.

## Paso 1: Acceder a Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Inicia sesión con tu cuenta de Google
3. Selecciona tu proyecto (o créalo si no tienes uno)
   - Si no tienes proyecto, haz clic en el selector de proyectos en la parte superior
   - Haz clic en **NEW PROJECT**
   - Ingresa un nombre (ej: `livemenu-sec`)
   - Haz clic en **CREATE**

## Paso 2: Navegar a Service Accounts

1. En el menú lateral izquierdo (☰), haz clic en **IAM & Admin**
2. Haz clic en **Service Accounts**

## Paso 3: Crear una Nueva Service Account

1. Haz clic en el botón **+ CREATE SERVICE ACCOUNT** (arriba)
2. Completa el formulario:
   - **Service account name**: `livemenu-storage` (o el nombre que prefieras)
   - **Service account ID**: Se genera automáticamente basado en el nombre
   - Haz clic en **CREATE AND CONTINUE**

## Paso 4: Asignar Roles

1. En **Grant this service account access to project**, busca y selecciona:
   - **Storage Admin** (recomendado para acceso completo)
   - O **Storage Object Admin** (solo para escribir/leer objetos)
   
   💡 **Tip**: Escribe "Storage" en el buscador para filtrar los roles relacionados

2. Haz clic en **CONTINUE**

## Paso 5: Opcional - Agregar Usuarios Administradores

1. (Opcional) Puedes agregar usuarios que puedan administrar esta cuenta
2. Haz clic en **DONE**

## Paso 6: Crear y Descargar la Clave JSON

1. En la lista de Service Accounts, haz clic en la cuenta que acabas de crear
   - Deberías ver algo como: `livemenu-storage@tu-proyecto.iam.gserviceaccount.com`

2. Ve a la pestaña **KEYS** (en la parte superior)

3. Haz clic en **ADD KEY** → **Create new key**

4. En el diálogo que aparece:
   - Selecciona **JSON** como tipo de clave
   - Haz clic en **CREATE**

5. **IMPORTANTE**: El archivo JSON se descargará automáticamente a tu carpeta de descargas

   ⚠️ **ADVERTENCIA**: Este archivo solo se puede descargar una vez. Guárdalo de forma segura.

## Paso 7: Verificar el Archivo JSON

El archivo descargado debería tener un nombre similar a:
```
tu-proyecto-xxxxx-xxxxx.json
```

Abre el archivo para verificar que tiene este formato:
```json
{
  "type": "service_account",
  "project_id": "tu-proyecto-id",
  "private_key_id": "xxxxx",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "livemenu-storage@tu-proyecto.iam.gserviceaccount.com",
  "client_id": "xxxxx",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/..."
}
```

## Paso 8: Mover el Archivo a una Ubicación Segura

1. Mueve el archivo JSON a una ubicación segura fuera del directorio del proyecto
   - Ejemplo: `C:\Users\TuUsuario\.gcp\service-account-key.json`
   - O: `C:\secure\credentials\livemenu-storage.json`

2. **NUNCA** commitees este archivo en Git
   - Asegúrate de que está en tu `.gitignore`

## Paso 9: Configurar en tu Proyecto

### Opción A: Variable de Entorno (Recomendado)

Agrega a tu archivo `.env`:
```bash
GOOGLE_APPLICATION_CREDENTIALS=C:\Users\TuUsuario\.gcp\service-account-key.json
GCP_STORAGE_BUCKET_NAME=livemenu-images
```

### Opción B: Configuración Directa

Agrega a tu archivo `.env`:
```bash
GCP_STORAGE_BUCKET_NAME=livemenu-images
GCP_STORAGE_CREDENTIALS_PATH=C:\Users\TuUsuario\.gcp\service-account-key.json
```

## Paso 10: Verificar que Funciona

1. Reinicia tu aplicación Quarkus
2. Prueba el endpoint de test:
   ```
   GET /api/v1/test/gcp/credentials
   ```
   (Requiere autenticación con Bearer token)

3. Si todo está bien, deberías recibir:
   ```json
   {
     "status": "success",
     "message": "GCP Storage credentials are valid",
     "testFile": "test/credentials-test-...",
     "note": "Test file uploaded successfully..."
   }
   ```

## Solución de Problemas

### Error: "File not found"
- Verifica que la ruta al archivo JSON sea correcta
- Usa rutas absolutas (completas) en lugar de relativas
- En Windows, usa `C:\` en lugar de `C:`

### Error: "Invalid credentials"
- Verifica que el archivo JSON no esté corrupto
- Asegúrate de que descargaste el archivo completo
- Intenta crear una nueva clave JSON si es necesario

### Error: "Access Denied"
- Verifica que la Service Account tenga el rol `Storage Admin` o `Storage Object Admin`
- Verifica que el bucket existe y es accesible

### Error: "Bucket not found"
- Verifica que `GCP_STORAGE_BUCKET_NAME` tenga el nombre correcto del bucket
- Asegúrate de que el bucket existe en el mismo proyecto de GCP

## Seguridad

⚠️ **IMPORTANTE - Buenas Prácticas de Seguridad:**

1. **Nunca commitees el archivo JSON** en tu repositorio Git
2. Agrega el archivo a `.gitignore`:
   ```
   *.json
   service-account-key.json
   credentials/
   .gcp/
   ```
3. **Guarda el archivo en una ubicación segura** fuera del directorio del proyecto
4. **Usa variables de entorno** en lugar de hardcodear rutas
5. **Rota las claves periódicamente** (cada 90 días recomendado)
6. **Elimina claves antiguas** que ya no uses desde la consola de GCP

## Comandos Rápidos (Cloud Shell)

Si prefieres usar la línea de comandos, puedes crear la Service Account desde Cloud Shell:

```bash
# Configurar proyecto
gcloud config set project tu-proyecto-id

# Crear Service Account
gcloud iam service-accounts create livemenu-storage \
  --display-name="LiveMenu Storage Service Account"

# Asignar rol Storage Admin
gcloud projects add-iam-policy-binding tu-proyecto-id \
  --member="serviceAccount:livemenu-storage@tu-proyecto-id.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# Crear y descargar clave JSON
gcloud iam service-accounts keys create service-account-key.json \
  --iam-account=livemenu-storage@tu-proyecto-id.iam.gserviceaccount.com
```

El archivo `service-account-key.json` se descargará en el directorio actual.

## Resumen Visual

```
Google Cloud Console
    ↓
IAM & Admin → Service Accounts
    ↓
CREATE SERVICE ACCOUNT
    ↓
Asignar rol: Storage Admin
    ↓
KEYS → ADD KEY → Create new key → JSON
    ↓
Archivo JSON descargado
    ↓
Configurar en .env
    ↓
¡Listo para usar!
```

## ¿Necesitas Ayuda?

Si tienes problemas:
1. Revisa los logs de la aplicación para ver errores específicos
2. Verifica que todas las variables de entorno estén configuradas
3. Asegúrate de que el bucket existe en tu proyecto de GCP
4. Verifica que la Service Account tiene los permisos correctos


