# Verificación de Credenciales HMAC en GCP Cloud Shell

Este documento contiene comandos para verificar y diagnosticar problemas con las credenciales HMAC de GCP Storage.

## 1. Verificar que el Proyecto está Configurado

```bash
# Ver el proyecto actual
gcloud config get-value project

# Si necesitas cambiar el proyecto
gcloud config set project livemenu-sec
```

## 2. Verificar que el Bucket Existe

```bash
# Listar todos los buckets
gsutil ls

# Verificar un bucket específico
gsutil ls gs://livemenu-images

# Ver información detallada del bucket
gsutil ls -L -b gs://livemenu-images
```

## 3. Verificar la Cuenta de Servicio y sus HMAC Keys

```bash
# Ver la cuenta de servicio
gcloud iam service-accounts describe livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com

# Listar todas las cuentas de servicio
gcloud iam service-accounts list

# Ver las claves HMAC de la cuenta de servicio
gcloud storage hmac list --service-account=livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com --project=livemenu-sec

# Ver detalles de una clave HMAC específica (reemplaza ACCESS_KEY_ID con tu Access Key)
gcloud storage hmac describe GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS \
  --service-account=livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com
```

## 4. Verificar Permisos de la Cuenta de Servicio

```bash
# Ver los roles asignados a la cuenta de servicio a nivel de proyecto
gcloud projects get-iam-policy livemenu-sec \
  --flatten="bindings[].members" \
  --filter="bindings.members:livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com" \
  --format="table(bindings.role)"

# Ver los permisos a nivel de bucket (si hay políticas específicas)
gsutil iam get gs://livemenu-images

# Verificar si la cuenta de servicio tiene acceso
gsutil iam ch serviceAccount:livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com:roles/storage.admin gs://livemenu-images
```

## 5. Probar la Subida con gsutil (para comparar)

```bash
# Crear un archivo de prueba
echo "test" > test-upload.txt

# Intentar subir usando gsutil (esto usa autenticación diferente, pero verifica permisos)
gsutil cp test-upload.txt gs://livemenu-images/test/

# Limpiar
rm test-upload.txt
gsutil rm gs://livemenu-images/test/test-upload.txt
```

## 6. Verificar el Estado de la Clave HMAC

```bash
# Ver el estado de la clave (ACTIVE, INACTIVE, DELETED)
gcloud storage hmac describe GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS \
  --service-account=livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com \
  --format="value(state)"

# Si la clave está INACTIVE, activarla
gcloud storage hmac update GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS \
  --service-account=livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com \
  --state=ACTIVE
```

## 7. Verificar el Formato de la Secret Key

**IMPORTANTE**: La Secret Key debe ser el valor RAW que se muestra cuando creas la clave HMAC. 

```bash
# ⚠️ NO puedes recuperar la Secret Key después de crearla
# Si la perdiste, debes crear una nueva clave HMAC

# Para crear una nueva clave HMAC (si es necesario)
gcloud storage hmac create livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com

# Esto mostrará:
# - Access Key ID (ej: GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS)
# - Secret Access Key (este es el valor RAW que debes usar, NO está en Base64)
```

## 8. Probar HMAC con curl (para verificar la firma)

```bash
# Configurar variables (reemplaza con tus valores reales)
export ACCESS_KEY="GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS"
export SECRET_KEY="tu-secret-key-raw-aqui"
export BUCKET="livemenu-images"
export OBJECT_PATH="test/hmac-test.txt"
export CONTENT="Hello from HMAC test"

# Generar fecha en formato RFC 1123
export DATE=$(date -u +"%a, %d %b %Y %H:%M:%S GMT")

# Construir el StringToSign (formato AWS Signature Version 2)
# PUT\n\nimage/png\n${DATE}\n/${BUCKET}/${OBJECT_PATH}
export STRING_TO_SIGN="PUT\n\nimage/png\n${DATE}\n/${BUCKET}/${OBJECT_PATH}"

# Generar la firma HMAC-SHA256
export SIGNATURE=$(echo -n "$STRING_TO_SIGN" | openssl dgst -sha256 -hmac "$SECRET_KEY" -binary | base64)

# Hacer la petición
curl -X PUT \
  "https://storage.googleapis.com/${BUCKET}/${OBJECT_PATH}" \
  -H "Date: ${DATE}" \
  -H "Content-Type: image/png" \
  -H "Authorization: AWS ${ACCESS_KEY}:${SIGNATURE}" \
  --data-binary "${CONTENT}"
```

## 9. Diagnóstico Rápido (Script Completo)

Copia y pega este script completo en Cloud Shell:

```bash
#!/bin/bash

PROJECT_ID="livemenu-sec"
SERVICE_ACCOUNT="livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com"
BUCKET_NAME="livemenu-images"
ACCESS_KEY="GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS"

echo "=== Verificando Proyecto ==="
gcloud config set project $PROJECT_ID
echo "Proyecto actual: $(gcloud config get-value project)"

echo -e "\n=== Verificando Bucket ==="
if gsutil ls gs://$BUCKET_NAME &>/dev/null; then
    echo "✓ Bucket existe: gs://$BUCKET_NAME"
else
    echo "✗ Bucket NO existe: gs://$BUCKET_NAME"
fi

echo -e "\n=== Verificando Cuenta de Servicio ==="
if gcloud iam service-accounts describe $SERVICE_ACCOUNT &>/dev/null; then
    echo "✓ Cuenta de servicio existe: $SERVICE_ACCOUNT"
else
    echo "✗ Cuenta de servicio NO existe: $SERVICE_ACCOUNT"
fi

echo -e "\n=== Verificando Clave HMAC ==="
HMAC_INFO=$(gcloud storage hmac describe $ACCESS_KEY \
  --service-account=$SERVICE_ACCOUNT \
  --project=$PROJECT_ID \
  --format="value(state,accessId)" 2>/dev/null)

if [ $? -eq 0 ]; then
    STATE=$(echo $HMAC_INFO | cut -d' ' -f1)
    echo "✓ Clave HMAC encontrada"
    echo "  Estado: $STATE"
    if [ "$STATE" != "ACTIVE" ]; then
        echo "  ⚠️  ADVERTENCIA: La clave NO está ACTIVE"
    fi
else
    echo "✗ Clave HMAC NO encontrada o no accesible"
fi

echo -e "\n=== Verificando Permisos ==="
ROLES=$(gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:$SERVICE_ACCOUNT" \
  --format="value(bindings.role)" 2>/dev/null)

if echo "$ROLES" | grep -q "storage"; then
    echo "✓ La cuenta tiene roles de Storage:"
    echo "$ROLES" | grep "storage"
else
    echo "✗ La cuenta NO tiene roles de Storage asignados"
    echo "Roles actuales:"
    echo "$ROLES"
fi

echo -e "\n=== Resumen ==="
echo "Si todos los checks muestran ✓, las credenciales deberían funcionar."
echo "Si hay ✗, corrige esos problemas primero."
```

## 10. Solución de Problemas Comunes

### Error: SignatureDoesNotMatch

**Posibles causas:**
1. **Secret Key incorrecto**: Asegúrate de usar el valor RAW, no Base64
2. **Access Key y Secret Key no coinciden**: Deben ser del mismo par de claves HMAC
3. **Formato de fecha incorrecto**: Debe ser RFC 1123 en inglés (Locale.ENGLISH)
4. **StringToSign mal formado**: Verifica que el formato sea exactamente:
   ```
   PUT\n\nimage/png\nDate\n/bucket-name/object-path
   ```

### Error: AccessDenied

**Solución:**
```bash
# Asignar rol Storage Admin a nivel de proyecto
gcloud projects add-iam-policy-binding livemenu-sec \
  --member="serviceAccount:livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# O solo para el bucket específico
gsutil iam ch serviceAccount:livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com:roles/storage.admin gs://livemenu-images
```

### Clave HMAC Inactiva

```bash
# Activar la clave
gcloud storage hmac update GOOG1EUIBRUTOZQBXWFR6PFGB4WGJ64CAM6IQPSK2PXUDLFYMX3STLQLC2JTS \
  --service-account=livemenu-serviceaccount@livemenu-sec.iam.gserviceaccount.com \
  --project=livemenu-sec \
  --state=ACTIVE
```

## Notas Importantes

1. **Secret Key**: Una vez creada, NO puedes recuperarla. Si la perdiste, crea una nueva clave HMAC.
2. **Formato**: La Secret Key es un string RAW, NO está codificado en Base64.
3. **Permisos**: La cuenta de servicio necesita `roles/storage.admin` o `roles/storage.objectAdmin`.
4. **Estado**: La clave HMAC debe estar en estado `ACTIVE`.

